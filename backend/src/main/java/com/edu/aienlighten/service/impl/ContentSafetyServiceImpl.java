package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.common.SafetyResult;
import com.edu.aienlighten.dto.ScenePolicyDTO;
import com.edu.aienlighten.entity.AiInteractionLog;
import com.edu.aienlighten.entity.AiScenePolicy;
import com.edu.aienlighten.mapper.AiScenePolicyMapper;
import com.edu.aienlighten.service.ContentSafetyService;
import com.edu.aienlighten.service.impl.SensitiveWordMatcher.WordEntry;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内容安全服务实现：敏感词过滤 + 未成年人适宜性评估 + 准确性校验。
 *
 * <p><b>三层各自的分工（这一点是设计的关键，三层不是重复劳动）：</b></p>
 * <ul>
 *   <li><b>敏感词过滤</b>——硬规则。只对 {@code level=3} 的明令禁止词生效，非黑即白，命中即拦。</li>
 *   <li><b>未成年人适宜性评估</b>——程度判断。把 {@code level&lt;3} 的词按分类权重累加打分，
 *       再叠加站外导流特征分，达到阈值才处置。单看一个「暴力」不拦，同时出现「暴力 + 迷信」才拦。</li>
 *   <li><b>准确性校验</b>——生成质量。结构层面（过短、模型自我描述泄漏）+ 数值型常识比对。</li>
 * </ul>
 *
 * <p><b>准确性校验的边界（务必如实理解）</b>：本阶段只做「结构完整性 + 少量数值型常识的启发式比对」，
 * 不是事实核查模型。它只能发现「和内置常识表冲突的数值断言」并转人工复核，无法判断一般性论述的对错。
 * 真正的事实级校验需要独立的科普知识库，属于后续增强项。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSafetyServiceImpl implements ContentSafetyService {

    /** 三个场景的固定顺序，管理端展示与库中补行都以它为准 */
    private static final List<String> SCENES =
            List.of(AiScenePolicy.SCENE_WRITING, AiScenePolicy.SCENE_CHAT, AiScenePolicy.SCENE_QUIZ);

    private static final Map<String, String> SCENE_NAMES = Map.of(
            AiScenePolicy.SCENE_WRITING, "创意写作实验室",
            AiScenePolicy.SCENE_CHAT, "智能答疑实验室",
            AiScenePolicy.SCENE_QUIZ, "知识闯关实验室");

    // ---------------- 未成年人适宜性评估的打分参数 ----------------

    /** 各分类的基础权重。数值越高，出现该分类内容的风险越大 */
    private static final Map<Integer, Integer> CATEGORY_WEIGHT = Map.of(
            1, 80,  // 涉政
            2, 70,  // 色情
            3, 60,  // 暴力
            4, 55,  // 赌博毒品
            5, 45,  // 迷信诈骗
            6, 25,  // 广告导流
            7, 20); // 其他

    /** 命中 level=3 硬禁用词时的附加分 */
    private static final int HARD_WORD_BONUS = 20;

    /** 出现站外联系方式（微信/QQ/邮箱/链接）的附加分，单独出现即达复核阈值 */
    private static final int CONTACT_BONUS = 35;

    /** 风险分达到该值即拦截 */
    private static final int RISK_BLOCK = 80;

    /** 风险分达到该值即记入教师复核队列（内容仍然放行） */
    private static final int RISK_REVIEW = 30;

    /** 命中词最多记 10 个进日志，避免 TEXT 列被刷爆 */
    private static final int MAX_HIT_WORDS = 10;

    /** 生成内容短于该长度视为生成失败 */
    private static final int MIN_OUTPUT_LEN = 5;

    /** 站外导流特征：链接 / 邮箱 / 微信号 QQ 号 */
    private static final Pattern CONTACT_PATTERN = Pattern.compile(
            "https?://\\S+"
                    + "|www\\.[a-zA-Z0-9.-]+"
                    + "|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
                    + "|(微信|weixin|WeChat|QQ|qq|企鹅)\\s*(号)?\\s*[:：]?\\s*[a-zA-Z0-9_-]{5,}");

    /** 模型自我描述泄漏：出现即认为提示词约束被绕过，内容放行但必须复核 */
    private static final List<Pattern> META_LEAK_PATTERNS = List.of(
            Pattern.compile("作为(一个)?\\s*(AI|人工智能|语言模型|大模型|智能助手|聊天机器人)"),
            Pattern.compile("我是(一个)?\\s*(AI|人工智能|语言模型|大模型|聊天机器人)"),
            Pattern.compile("(system\\s*prompt|系统提示词|我的提示词|提示词是)"),
            Pattern.compile("(?i)as an AI"),
            Pattern.compile("(?i)I am an AI"));

    /**
     * 数值型常识对照表。
     *
     * <p>触发条件：文本提到了该常识的任一关键词，且出现了数字，却匹配不上正确表述的正则
     * —— 例如「水在标准大气压下的沸点是 200 摄氏度」。命中只提高风险分并转人工复核，不直接拦截，
     * 避免误杀。</p>
     */
    private record ScienceFact(List<String> keywords, Pattern correct, String expected, boolean needDigit) {
    }

    private static final List<ScienceFact> SCIENCE_FACTS = List.of(
            new ScienceFact(List.of("光速", "光在真空", "光的速度", "光的传播速度"),
                    Pattern.compile("30\\s*万|3\\s*[×xX*]\\s*10\\s*\\^?\\s*8|299792|30万千米"),
                    "每秒约 30 万公里", true),
            new ScienceFact(List.of("水的沸点", "水在标准大气压", "水沸腾"),
                    Pattern.compile("100\\s*(℃|度|摄氏度)|摄氏\\s*100"),
                    "标准大气压下 100℃", true),
            new ScienceFact(List.of("水的冰点", "水的凝固点", "水结冰"),
                    Pattern.compile("0\\s*(℃|度|摄氏度)|零度|摄氏\\s*0"),
                    "0℃", true),
            new ScienceFact(List.of("八大行星", "太阳系有几颗行星", "太阳系的行星"),
                    Pattern.compile("8|八"),
                    "8 颗", true),
            new ScienceFact(List.of("人体正常体温", "人的正常体温", "人体体温"),
                    Pattern.compile("3[67]"),
                    "约 36~37℃", true),
            new ScienceFact(List.of("地球公转", "地球绕太阳", "绕太阳一圈"),
                    Pattern.compile("36[56]|一年"),
                    "约 365 天", true),
            new ScienceFact(List.of("圆周率"),
                    Pattern.compile("3\\.14"),
                    "约 3.14", true),
            new ScienceFact(List.of("空气中氧气", "氧气的占比", "氧气含量"),
                    Pattern.compile("21\\s*(%|％)|百分之二十一"),
                    "约 21%", true),
            new ScienceFact(List.of("月球绕地球", "月亮绕地球"),
                    Pattern.compile("2[78]|一个月"),
                    "约 27~28 天", true),
            new ScienceFact(List.of("太阳系最大的行星", "太阳系中体积最大", "太阳系里最大的行星"),
                    Pattern.compile("木星"),
                    "木星", false));

    private final SensitiveWordMatcher matcher;
    private final AiScenePolicyMapper policyMapper;

    // ==================== 输入侧 ====================

    @Override
    public SafetyResult checkInput(AiScenePolicy policy, String text) {
        if (policy == null || !policy.inputOn() || text == null || text.isBlank()) {
            return SafetyResult.pass(text);
        }
        List<WordEntry> hits = matcher.match(text);
        if (hits.isEmpty()) {
            return SafetyResult.pass(text);
        }
        int risk = riskScore(hits, text);
        String words = joinWords(hits);

        boolean forbidden = hits.stream().anyMatch(h -> h.level() >= 3);
        boolean overRisk = risk >= RISK_BLOCK;
        String reason = "输入命中敏感词：" + words + "（风险分 " + risk + "）";

        if ((forbidden || overRisk) && policy.autoBlockOn()) {
            return SafetyResult.block(AiInteractionLog.STAGE_INPUT, reason,
                    tipUnfit(policy.getScene()), splitWords(words), risk);
        }
        // 命中不严重、或场景关闭了自动拦截：放行进入大模型，但记入教师复核队列
        return SafetyResult.flag(AiInteractionLog.STAGE_INPUT, reason, text, risk, splitWords(words));
    }

    // ==================== 输出侧三重过滤 ====================

    @Override
    public SafetyResult checkOutput(AiScenePolicy policy, String text) {
        if (text == null || text.isBlank()) {
            return SafetyResult.pass(text);
        }
        AiScenePolicy p = policy == null ? defaultPolicy(AiScenePolicy.SCENE_WRITING) : policy;

        // 敏感词过滤与适宜性评估共用一次匹配、一次打分；关闭时才跳过，避免无谓的遍历
        boolean needWordScan = p.outputSensitiveOn() || p.suitabilityOn();
        List<WordEntry> hits = needWordScan ? matcher.match(text) : List.of();
        int risk = needWordScan ? riskScore(hits, text) : 0;

        // ---------- ① 敏感词过滤：只对 level=3 的明令禁止词生效 ----------
        if (p.outputSensitiveOn()) {
            List<WordEntry> forbidden = hits.stream().filter(h -> h.level() >= 3).toList();
            if (!forbidden.isEmpty()) {
                String reason = "输出命中禁用词：" + joinWords(forbidden);
                List<String> words = splitWords(joinWords(forbidden));
                if (p.autoBlockOn()) {
                    return SafetyResult.replace(AiInteractionLog.STAGE_SENSITIVE, reason,
                            tipReplaced(p.getScene()), tipReplaced(p.getScene()), words, risk);
                }
                return SafetyResult.flag(AiInteractionLog.STAGE_SENSITIVE, reason, text, risk, words);
            }
        }

        // ---------- ② 未成年人适宜性评估：加权风险分 ----------
        if (p.suitabilityOn()) {
            List<String> hitWordList = splitWords(joinWords(hits));
            if (risk >= RISK_BLOCK) {
                String reason = "适宜性风险分 " + risk + " 超过拦截阈值 " + RISK_BLOCK;
                if (p.autoBlockOn()) {
                    return SafetyResult.replace(AiInteractionLog.STAGE_SUITABILITY, reason,
                            tipReplaced(p.getScene()), tipReplaced(p.getScene()), hitWordList, risk);
                }
                return SafetyResult.flag(AiInteractionLog.STAGE_SUITABILITY, reason, text, risk, hitWordList);
            }
            if (risk >= RISK_REVIEW) {
                return SafetyResult.flag(AiInteractionLog.STAGE_SUITABILITY,
                        "适宜性风险分 " + risk + "，已达复核阈值 " + RISK_REVIEW, text, risk, hitWordList);
            }
        }

        // ---------- ③ 准确性校验 ----------
        if (p.accuracyOn()) {
            String hard = accuracyHardIssue(text);
            if (hard != null) {
                if (p.autoBlockOn()) {
                    return SafetyResult.replace(AiInteractionLog.STAGE_ACCURACY, hard,
                            tipFailed(p.getScene()), tipFailed(p.getScene()), List.of(), risk);
                }
                return SafetyResult.flag(AiInteractionLog.STAGE_ACCURACY, hard, text, risk);
            }
            String suspect = accuracySuspect(text);
            if (suspect != null) {
                return SafetyResult.flag(AiInteractionLog.STAGE_ACCURACY, suspect, text, risk + 40);
            }
        }

        SafetyResult pass = SafetyResult.pass(text);
        pass.setRiskScore(risk);
        return pass;
    }

    // ==================== 出题结构校验 ====================

    @Override
    public List<Map<String, Object>> validateQuiz(AiScenePolicy policy, List<Map<String, Object>> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        if (policy != null && !policy.accuracyOn()) {
            return questions;
        }
        List<Map<String, Object>> valid = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            String question = str(q.get("question"));
            String answer = str(q.get("answer"));
            List<String> options = toOptionList(q.get("options"));

            if (question.trim().length() < 5) {
                log.debug("出题校验：题干过短，已剔除。question={}", question);
                continue;
            }
            if (options.size() < 2) {
                log.debug("出题校验：选项不足两个，已剔除。question={}", question);
                continue;
            }
            if (new HashSet<>(options).size() != options.size()) {
                log.debug("出题校验：选项存在重复，已剔除。question={}", question);
                continue;
            }
            String resolved = resolveAnswer(answer, options);
            if (resolved == null) {
                log.debug("出题校验：answer 不在选项内，已剔除。answer={}，question={}", answer, question);
                continue;
            }
            Map<String, Object> fixed = new LinkedHashMap<>(q);
            fixed.put("answer", resolved);
            valid.add(fixed);
        }
        if (valid.size() < questions.size()) {
            log.warn("出题准确性校验剔除 {}/{} 道题目", questions.size() - valid.size(), questions.size());
        }
        return valid;
    }

    // ==================== 场景策略 ====================

    @Override
    public AiScenePolicy policyOf(String scene) {
        String s = normalizeScene(scene);
        AiScenePolicy p = policyMapper.selectOne(new LambdaQueryWrapper<AiScenePolicy>()
                .eq(AiScenePolicy::getScene, s)
                .last("limit 1"));
        if (p != null) {
            return p;
        }
        return createDefault(s);
    }

    @Override
    public List<AiScenePolicy> allPolicies() {
        List<AiScenePolicy> list = new ArrayList<>(SCENES.size());
        for (String scene : SCENES) {
            list.add(policyOf(scene));
        }
        return list;
    }

    @Override
    public AiScenePolicy policyForObserve(AiScenePolicy src) {
        if (src == null) {
            return null;
        }
        AiScenePolicy copy = new AiScenePolicy();
        copy.setId(src.getId());
        copy.setScene(src.getScene());
        copy.setSceneName(src.getSceneName());
        copy.setDefaultStyle(src.getDefaultStyle());
        copy.setGenCount(src.getGenCount());
        copy.setAnswerLimitSec(src.getAnswerLimitSec());
        copy.setInputFilter(src.getInputFilter());
        copy.setOutputSensitive(src.getOutputSensitive());
        copy.setAccuracyCheck(src.getAccuracyCheck());
        copy.setMinorSuitability(src.getMinorSuitability());
        // 唯一的变化：不再拦截。检测与打分仍然全开，日志里照样能看到命中情况。
        copy.setAutoBlock(0);
        copy.setUpdatedAt(src.getUpdatedAt());
        return copy;
    }

    @Override
    public void savePolicies(List<ScenePolicyDTO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (ScenePolicyDTO dto : list) {
            AiScenePolicy p = policyOf(dto.getScene());
            if (dto.getDefaultStyle() != null) {
                p.setDefaultStyle(dto.getDefaultStyle());
            }
            if (dto.getGenCount() != null) {
                p.setGenCount(dto.getGenCount());
            }
            if (dto.getAnswerLimitSec() != null) {
                p.setAnswerLimitSec(dto.getAnswerLimitSec());
            }
            if (dto.getInputFilter() != null) {
                p.setInputFilter(dto.getInputFilter());
            }
            if (dto.getOutputSensitive() != null) {
                p.setOutputSensitive(dto.getOutputSensitive());
            }
            if (dto.getAccuracyCheck() != null) {
                p.setAccuracyCheck(dto.getAccuracyCheck());
            }
            if (dto.getMinorSuitability() != null) {
                p.setMinorSuitability(dto.getMinorSuitability());
            }
            if (dto.getAutoBlock() != null) {
                p.setAutoBlock(dto.getAutoBlock());
            }
            policyMapper.updateById(p);
        }
    }

    @Override
    public void refreshWordCache() {
        matcher.invalidate();
    }

    // ==================== 内部实现 ====================

    /** 加权风险分：分类权重之和 + 禁用词附加分 + 站外导流附加分，封顶 100 */
    private int riskScore(List<WordEntry> hits, String text) {
        int score = 0;
        for (WordEntry h : hits) {
            score += CATEGORY_WEIGHT.getOrDefault(h.category(), 20);
        }
        if (hits.stream().anyMatch(h -> h.level() >= 3)) {
            score += HARD_WORD_BONUS;
        }
        if (text != null && CONTACT_PATTERN.matcher(text).find()) {
            score += CONTACT_BONUS;
        }
        return Math.min(score, 100);
    }

    /** 结构层面的硬性问题：生成失败。返回 null 表示无问题 */
    private String accuracyHardIssue(String text) {
        if (text.trim().length() < MIN_OUTPUT_LEN) {
            return "生成内容过短（不足 " + MIN_OUTPUT_LEN + " 字），疑似生成失败";
        }
        return null;
    }

    /** 需要人工复核的疑点：模型自我描述泄漏、常识数值冲突。返回 null 表示无疑点 */
    private String accuracySuspect(String text) {
        for (Pattern p : META_LEAK_PATTERNS) {
            if (p.matcher(text).find()) {
                return "生成内容夹杂模型自我描述，疑似提示词约束被绕过";
            }
        }
        boolean digit = hasDigit(text);
        for (ScienceFact fact : SCIENCE_FACTS) {
            // needDigit=false 的常识（如太阳系最大的行星）不要求文本里出现数字
            if (fact.needDigit() && !digit) {
                continue;
            }
            if (!containsAny(text, fact.keywords())) {
                continue;
            }
            if (!fact.correct().matcher(text).find()) {
                return "疑似常识表述有误：关于「" + fact.keywords().get(0)
                        + "」，正确表述应约为 " + fact.expected();
            }
        }
        return null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** 把 answer 归一化成选项原文；兼容模型返回「A/B/C/D」序号的情况。无法归一化返回 null */
    private String resolveAnswer(String answer, List<String> options) {
        if (answer == null || answer.isBlank()) {
            return null;
        }
        String a = answer.trim();
        for (String o : options) {
            if (o != null && o.trim().equals(a)) {
                return o;
            }
        }
        if (a.length() == 1) {
            int idx = Character.toUpperCase(a.charAt(0)) - 'A';
            if (idx >= 0 && idx < options.size()) {
                return options.get(idx);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> toOptionList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> options = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o == null) {
                return List.of();
            }
            options.add(String.valueOf(o));
        }
        return options;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** 去重后按原顺序拼接命中词 */
    private String joinWords(List<WordEntry> hits) {
        Set<String> words = new LinkedHashSet<>();
        for (WordEntry h : hits) {
            words.add(h.word());
            if (words.size() >= MAX_HIT_WORDS) {
                break;
            }
        }
        return String.join("、", words);
    }

    private List<String> splitWords(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return List.of(joined.split("、"));
    }

    private String normalizeScene(String scene) {
        if (scene == null || scene.isBlank()) {
            throw new BizException("AI 实验场景不能为空");
        }
        String s = scene.trim().toLowerCase();
        if (!SCENES.contains(s)) {
            throw new BizException("未知的 AI 实验场景：" + scene);
        }
        return s;
    }

    private AiScenePolicy createDefault(String scene) {
        AiScenePolicy p = defaultPolicy(scene);
        try {
            policyMapper.insert(p);
        } catch (DuplicateKeyException e) {
            // 并发下另一个线程已经插入：唯一键冲突说明行已存在，直接读回来
            AiScenePolicy exists = policyMapper.selectOne(new LambdaQueryWrapper<AiScenePolicy>()
                    .eq(AiScenePolicy::getScene, scene)
                    .last("limit 1"));
            if (exists != null) {
                return exists;
            }
            throw e;
        }
        return p;
    }

    /** 场景缺行时的兜底默认值：全部开关打开，与 init.sql 的初始数据保持一致 */
    private AiScenePolicy defaultPolicy(String scene) {
        AiScenePolicy p = new AiScenePolicy();
        p.setScene(scene);
        p.setSceneName(SCENE_NAMES.getOrDefault(scene, scene));
        p.setDefaultStyle(switch (scene) {
            case AiScenePolicy.SCENE_QUIZ -> "竞赛风";
            case AiScenePolicy.SCENE_CHAT -> "友好助教";
            default -> "童话风";
        });
        p.setGenCount(AiScenePolicy.SCENE_QUIZ.equals(scene) ? 10 : (AiScenePolicy.SCENE_WRITING.equals(scene) ? 1 : 0));
        p.setAnswerLimitSec(AiScenePolicy.SCENE_QUIZ.equals(scene) ? 30 : 0);
        p.setInputFilter(1);
        p.setOutputSensitive(1);
        p.setAccuracyCheck(1);
        p.setMinorSuitability(1);
        p.setAutoBlock(1);
        return p;
    }

    // ---------------- 处置文案：按场景区分，不要所有场景共用一句 ----------------

    private String tipUnfit(String scene) {
        return switch (scene == null ? "" : scene) {
            case AiScenePolicy.SCENE_CHAT -> "这个问题不太适合小朋友哦，换一个问题再试试吧～";
            case AiScenePolicy.SCENE_QUIZ -> "这个方向不太适合出题哦，换一个再来～";
            default -> "这个主题不太适合小朋友哦，换一个主题再试试吧～";
        };
    }

    private String tipReplaced(String scene) {
        return switch (scene == null ? "" : scene) {
            case AiScenePolicy.SCENE_CHAT -> "这个回答被安全小卫士拦下啦，换个问题再试试吧～";
            case AiScenePolicy.SCENE_QUIZ -> "出题老师休息一下，请稍后再试～";
            default -> "这次生成的内容没有通过安全检查，换个主题再试试吧～";
        };
    }

    private String tipFailed(String scene) {
        return switch (scene == null ? "" : scene) {
            case AiScenePolicy.SCENE_CHAT -> "AI 老师没想好怎么回答，再问一次试试吧～";
            case AiScenePolicy.SCENE_QUIZ -> "出题老师休息一下，请稍后再试～";
            default -> "这次没有写好，再点一次试试吧～";
        };
    }
}
