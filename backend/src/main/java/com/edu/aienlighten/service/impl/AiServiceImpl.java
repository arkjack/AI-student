package com.edu.aienlighten.service.impl;

import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.common.SafetyResult;
import com.edu.aienlighten.entity.AiConfigEntity;
import com.edu.aienlighten.entity.AiInteractionLog;
import com.edu.aienlighten.entity.AiScenePolicy;
import com.edu.aienlighten.mapper.AiConfigMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.AiService;
import com.edu.aienlighten.service.ContentSafetyService;
import com.edu.aienlighten.service.InteractionLogService;
import com.edu.aienlighten.vo.AiReplyVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeepSeek（OpenAI 兼容 /v1/chat/completions）接入实现。
 *
 * <p>仅使用 JDK 内置 {@link HttpClient} + Spring Boot 自带 Jackson（{@link ObjectMapper}），无新增依赖。
 * 任何网络异常 / 超时 / 非 200 均降级返回预设文案，接口层不抛未处理异常。</p>
 *
 * <p><b>内容安全链路</b>（对应任务书「AI 生成内容安全审核机制」）：</p>
 * <pre>
 *   读 ai_config + ai_scene_policy
 *     → 校验 apiKey（1101）
 *     → checkInput：命中直接抛 1201，**不调用大模型**，并落一条拦截日志
 *     → 限流（1102）
 *     → 拼装安全系统提示词（第 0 道防线）
 *     → 调用 DeepSeek
 *     → checkOutput：敏感词过滤 → 未成年人适宜性评估 → 准确性校验
 *     → 无论通过/替换/拦截/降级都写 ai_interaction_log
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    /** 单行表约定的主键 id=1 */
    private static final Long CONFIG_ID = 1L;

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    /**
     * 默认模型。DeepSeek 现行模型名为 {@code deepseek-flash}（DeepSeek-V4.1-Flash）
     * 与 {@code deepseek-v4-pro}；旧名 {@code deepseek-chat} 已不在官方支持列表中，
     * 因此管理端填了旧名会调用失败，需要在这里跟上官方的命名变化。
     */
    private static final String DEFAULT_MODEL = "deepseek-flash";
    private static final int DEFAULT_TIMEOUT_SEC = 60;
    private static final int DEFAULT_RATE_LIMIT_PER_MIN = 20;
    private static final int DEFAULT_CONTENT_FILTER = 1;

    /**
     * 第 0 道防线：内容安全开启时追加到系统提示词的安全约束。
     * 它挡不住所有情况，所以才需要后面基于词库与打分的检测兜底。
     */
    private static final String SAFETY_PROMPT =
            "你是面向中小学生的科普助手，回答要通俗、积极、安全。"
                    + "不要讨论不适合未成年人的话题（暴力、色情、赌博、毒品、迷信、恐怖等）；"
                    + "不要索取或提供任何联系方式，不要引导学生到站外交流。";

    /* ------------------------------------------------------------------
       提示词结构：**固定部分进 system，变化部分进 user**
       ----------------------------------------------------------------
       这不是风格问题，而是直接影响成本：DeepSeek 的上下文硬盘缓存按「公共前缀」匹配，
       缓存命中的输入价格是未命中的 1/50。如果把每次都在变的主题/风格/数量拼进 system，
       前缀每次都不同 → 缓存永远命中不了。把变量挪到 user 之后，system 变成一个
       稳定不变的公共前缀，第 3 次请求起即可稳定命中缓存。

       改这里时请守住一条：system 里不允许出现任何随请求变化的内容。
       ------------------------------------------------------------------ */
    private static final String WRITING_SYSTEM =
            "你是中小学写作老师。请按照用户给出的主题、风格与字数要求写一篇作文。"
                    + "要求：条理清晰、语言生动、积极向上、符合中小学生阅读水平，"
                    + "直接输出作文正文，不要额外解释。";

    private static final String ANSWER_SYSTEM =
            "你是中小学生科普答疑老师，请用通俗易懂的语言，耐心解答学生的问题。";

    private static final String QUIZ_SYSTEM =
            "你是中小学知识闯关出题老师。请按照用户给出的难度与数量出选择题。"
                    + "只输出一个 JSON 数组，不要输出任何解释或多余文字。"
                    + "数组元素格式为：{\"question\":\"题干\",\"options\":[\"选项A\",\"选项B\",\"选项C\",\"选项D\"],"
                    + "\"answer\":\"正确选项内容\"}。请确保 answer 与 options 中某个选项完全一致。";

    private static final String DEFAULT_FALLBACK = "AI 暂时开小差了，请稍后再试～";
    private static final String ANSWER_FALLBACK = "网络有点忙，先去看看课程吧，AI 老师稍后就来！";
    private static final String QUIZ_FALLBACK = "出题老师休息一下，请稍后再试～";

    /** 输入侧命中时的业务码：前端据此区分「内容不合规」与「AI 未配置」 */
    public static final int CODE_CONTENT_BLOCKED = 1201;

    /** 上游返回 401/403：密钥无效或过期 */
    public static final int CODE_UPSTREAM_AUTH = 1103;

    /** 上游返回 402：账户余额不足 */
    public static final int CODE_UPSTREAM_BALANCE = 1104;

    /** 上游返回 429：被上游限流 */
    public static final int CODE_UPSTREAM_RATE_LIMIT = 1105;

    private final AiConfigMapper aiConfigMapper;
    private final ObjectMapper objectMapper;
    private final ContentSafetyService contentSafetyService;
    private final InteractionLogService interactionLogService;

    /** 按用户维度的每分钟限流器缓存 */
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public AiReplyVO chat(String systemPrompt, String userPrompt) {
        return reply(doChat(AiScenePolicy.SCENE_CHAT, systemPrompt, userPrompt,
                DEFAULT_FALLBACK, userPrompt));
    }

    @Override
    public AiReplyVO writing(String topic, String style, int length) {
        // system 固定不变，变量全部放进 user（见上方提示词结构说明）
        String userPrompt = "主题：" + topic + "\n风格：" + style + "\n字数：约 " + length + " 字";
        // 输入侧检测的是学生填写的主题，而不是拼好的提示词
        return reply(doChat(AiScenePolicy.SCENE_WRITING, WRITING_SYSTEM, userPrompt,
                DEFAULT_FALLBACK, topic));
    }

    @Override
    public AiReplyVO answer(String question) {
        return reply(doChat(AiScenePolicy.SCENE_CHAT, ANSWER_SYSTEM, question, ANSWER_FALLBACK, question));
    }

    @Override
    public List<Map<String, Object>> genQuiz(int count) {
        return genQuiz(1, count);
    }

    @Override
    public List<Map<String, Object>> genQuiz(int level, int count) {
        int safeCount = Math.max(count, 1);
        String levelText = switch (level) {
            case 2 -> "进阶";
            case 3 -> "挑战";
            default -> "基础";
        };
        // 同样把难度与数量放到 user，让 system 保持恒定以便命中缓存
        String userPrompt = "难度：" + levelText + "\n数量：" + safeCount + " 道";
        // 出题场景没有学生自由输入，输入侧检测传空串自然放行
        ChatOutcome outcome = doChat(AiScenePolicy.SCENE_QUIZ, QUIZ_SYSTEM, userPrompt, QUIZ_FALLBACK, "");
        String content = outcome.text();

        List<Map<String, Object>> quiz = parseQuiz(content, safeCount);
        if (quiz == null || quiz.isEmpty()) {
            log.warn("AI 出题解析失败，回退内置题库。原始内容: {}", content);
            return builtinQuiz(safeCount);
        }
        // 准确性校验：剔除 answer 不在选项内 / 选项重复 / 题干过短的题目
        List<Map<String, Object>> valid = contentSafetyService.validateQuiz(outcome.policy(), quiz);
        if (valid.isEmpty()) {
            log.warn("AI 出题全部未通过准确性校验，回退内置题库。原始内容: {}", content);
            return builtinQuiz(safeCount);
        }
        return valid;
    }

    private AiReplyVO reply(ChatOutcome outcome) {
        return outcome.filtered()
                ? AiReplyVO.filtered(outcome.text(), outcome.tip())
                : AiReplyVO.of(outcome.text());
    }

    /** 一次调用的结果：最终文本 + 是否被过滤 + 提示语 + 本次生效的场景策略 */
    private record ChatOutcome(String text, boolean filtered, String tip, AiScenePolicy policy) {
    }

    /**
     * 核心调用链路：读配置 - 校验密钥 - 输入检测 - 限流 - 拼装安全提示 - 调用 - 输出过滤 - 落日志。
     * 网络异常 / 超时 / 非 200 均捕获后降级返回 {@code fallback}。
     *
     * @param scene         场景标识（writing / chat / quiz），决定生效的安全策略与日志归属
     * @param inputForCheck 需要做输入侧合规检测的**学生原文**（不是拼装后的提示词）
     */
    private ChatOutcome doChat(String scene, String systemPrompt, String userPrompt,
                               String fallback, String inputForCheck) {
        AiConfigEntity config = loadConfig();
        int mode = AiConfigEntity.modeOf(config.getContentFilter());

        // 运行模式决定策略怎么生效：
        //   正常      → 按场景策略执行
        //   观察模式  → 策略照读，但把「自动拦截」关掉：检测/打分/日志全在，只是不拦
        //   完全关闭  → 策略为 null，整条检测链路跳过
        AiScenePolicy policy = null;
        if (mode != AiConfigEntity.MODE_OFF) {
            policy = contentSafetyService.policyOf(scene);
            if (mode == AiConfigEntity.MODE_OBSERVE) {
                policy = contentSafetyService.policyForObserve(policy);
            }
        }

        // ---------- 输入侧检测：命中即中断，违规指令根本不进入大模型 ----------
        // 刻意放在密钥校验之前：输入检测不依赖 API Key，放前面「违规指令不进入大模型」
        // 这条安全属性才是无条件的——AI 配没配好都一样成立。
        SafetyResult input = contentSafetyService.checkInput(policy, inputForCheck);
        if (input.isBlocked()) {
            interactionLogService.recordInputBlock(scene, inputForCheck, input);
            throw new BizException(CODE_CONTENT_BLOCKED, input.getTip());
        }

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BizException(1101, "AI 接口未配置，请联系管理员");
        }

        rateLimit(config);

        String safeSystemPrompt = appendSafety(config, systemPrompt, mode);
        long start = System.currentTimeMillis();
        String text;
        try {
            text = callDeepSeek(config, safeSystemPrompt, userPrompt);
        } catch (BizException e) {
            // 上游配置类错误（密钥无效 / 余额不足 / 被限流）。
            // 这类问题是**持续性的**，如果也用「AI 开小差了」糊过去，平台会一直悄悄返回
            // 罐头回答而没人知道原因——所以这里记一条日志后把真实原因抛给前端。
            log.warn("DeepSeek 上游返回配置类错误：code={}，msg={}", e.getCode(), e.getMessage());
            interactionLogService.recordFallback(new InteractionLogService.AiCall(
                    scene, inputForCheck, null, config.getModel(), elapsed(start)), e.getMessage());
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("DeepSeek 调用异常，触发降级。cause={}", e.toString());
            interactionLogService.recordFallback(new InteractionLogService.AiCall(
                    scene, inputForCheck, null, config.getModel(), elapsed(start)),
                    "模型调用失败，已降级返回兜底文案");
            return new ChatOutcome(fallback, false, null, policy);
        }
        int cost = elapsed(start);

        // ---------- 输出侧三重过滤 ----------
        SafetyResult output = contentSafetyService.checkOutput(policy, text);
        SafetyResult logged = mergeInputFlag(input, output);
        interactionLogService.record(new InteractionLogService.AiCall(
                scene, inputForCheck, text, config.getModel(), cost), logged);

        return new ChatOutcome(output.getText(), output.isReplaced(), output.getTip(), policy);
    }

    private int elapsed(long start) {
        return (int) (System.currentTimeMillis() - start);
    }

    /**
     * 合并输入侧与输出侧的结论。
     *
     * <p>输入侧「命中但不拦截」时 {@code checkOutput} 很可能返回一个干净的 pass，
     * 若直接以输出侧为准，这条输入命中就丢了；这里把它补回日志，保证「交互日志留存可追溯」。</p>
     */
    private SafetyResult mergeInputFlag(SafetyResult input, SafetyResult output) {
        if (output.getStage() != AiInteractionLog.STAGE_PASS
                || input.getStage() == AiInteractionLog.STAGE_PASS) {
            if (output.getRiskScore() < input.getRiskScore()) {
                output.setRiskScore(input.getRiskScore());
            }
            return output;
        }
        SafetyResult merged = SafetyResult.flag(input.getStage(), input.getReason(),
                output.getText(), input.getRiskScore());
        merged.setHitWords(input.getHitWords());
        return merged;
    }

    /** 读取 ai_config 单行记录；无记录或字段缺失时用默认值补齐 */
    private AiConfigEntity loadConfig() {
        AiConfigEntity config = aiConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new AiConfigEntity();
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            config.setBaseUrl(DEFAULT_BASE_URL);
        }
        if (config.getModel() == null || config.getModel().isBlank()) {
            config.setModel(DEFAULT_MODEL);
        }
        if (config.getTimeoutSec() == null || config.getTimeoutSec() <= 0) {
            config.setTimeoutSec(DEFAULT_TIMEOUT_SEC);
        }
        if (config.getRateLimitPerMin() == null || config.getRateLimitPerMin() <= 0) {
            config.setRateLimitPerMin(DEFAULT_RATE_LIMIT_PER_MIN);
        }
        if (config.getContentFilter() == null) {
            config.setContentFilter(DEFAULT_CONTENT_FILTER);
        }
        return config;
    }

    /** 按 userId 做每分钟限流，超限抛业务异常（走全局异常处理） */
    private void rateLimit(AiConfigEntity config) {
        Long userId = UserContext.userId();
        String key = userId == null ? "anonymous" : String.valueOf(userId);
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> new RateLimiter(config.getRateLimitPerMin()));
        if (!limiter.tryAcquire()) {
            throw new BizException(1102, "请求太频繁，休息一下再试吧～");
        }
    }

    /**
     * 按运行模式决定是否往系统提示词里追加安全约束。
     *
     * <p>观察模式**保留**这段约束：它不拦截学生，只是引导模型的措辞，
     * 属于「让 AI 尽量别说错话」，与「拦不拦」是两回事。只有完全关闭模式才一起去掉。</p>
     */
    private String appendSafety(AiConfigEntity config, String systemPrompt, int mode) {
        if (mode == AiConfigEntity.MODE_OFF) {
            return systemPrompt;
        }
        return systemPrompt + "\n" + SAFETY_PROMPT;
    }

    /** 调用 DeepSeek /v1/chat/completions，返回 choices[0].message.content；非 200 抛 IOException 触使降级 */
    private String callDeepSeek(AiConfigEntity config, String systemPrompt, String userPrompt)
            throws IOException, InterruptedException {
        int timeoutSec = config.getTimeoutSec() == null || config.getTimeoutSec() <= 0
                ? DEFAULT_TIMEOUT_SEC : config.getTimeoutSec();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .build();

        String url = normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions";
        String body = buildRequestBody(config.getModel(), systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("DeepSeek 返回非 200：status={}，body={}", resp.statusCode(), resp.body());
            switch (resp.statusCode()) {
                case 401, 403 -> throw new BizException(CODE_UPSTREAM_AUTH,
                        "DeepSeek 密钥无效或已过期，请联系管理员在「实验资源」页重新配置");
                case 402 -> throw new BizException(CODE_UPSTREAM_BALANCE,
                        "DeepSeek 账户余额不足，请联系管理员充值");
                case 429 -> throw new BizException(CODE_UPSTREAM_RATE_LIMIT,
                        "AI 请求太频繁，休息一下再试吧～");
                default -> throw new IOException("DeepSeek HTTP " + resp.statusCode());
            }
        }
        return parseContent(resp.body());
    }

    /**
     * 拼装请求体：{model, messages, temperature:0.7, max_tokens:800,
     * thinking:{type:disabled}, reasoning_effort:low}。
     *
     * <p><b>为什么要显式关掉思考模式</b>：{@code deepseek-flash} 的思考模式**默认开启**
     * 且 effort 默认为 {@code high}，模型会先输出一大段思维链再给答案。对本平台的三个场景
     * （写作文 / 科普答疑 / 出选择题）来说，这既拖慢响应又白白消耗输出 token，
     * 还会让 {@code max_tokens=800} 的额度被思维链吃掉、导致正文为空。
     * 另外思考模式下 {@code temperature} 会被忽略，关掉它之后采样参数才真正生效。</p>
     *
     * <p><b>关于 reasoning_effort</b>：思考模式已经关闭，此时 effort 没有可作用的对象，
     * 严格来说是冗余参数。这里仍然显式写成 {@code low} 属于防御性配置——
     * 万一将来有人把 thinking 改回 enabled，或者上游默认值变化，也不会突然按 high 跑。</p>
     */
    private String buildRequestBody(String model, String systemPrompt, String userPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userPrompt);
        root.put("temperature", 0.7);
        root.put("max_tokens", 800);
        root.putObject("thinking").put("type", "disabled");
        root.put("reasoning_effort", "low");
        return objectMapper.writeValueAsString(root);
    }

    /** 解析响应 choices[0].message.content */
    private String parseContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IOException("DeepSeek 响应缺少 choices[0].message.content");
        }
        return content.asText();
    }

    /** 解析 AI 出题的 JSON 数组；解析失败返回 null（由调用方回退内置题库） */
    private List<Map<String, Object>> parseQuiz(String content, int count) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonArray(content));
            if (!root.isArray()) {
                return null;
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode question = item.path("question");
                JsonNode answer = item.path("answer");
                JsonNode options = item.path("options");
                if (question.isMissingNode() || answer.isMissingNode()
                        || !options.isArray() || options.isEmpty()) {
                    continue;
                }
                List<String> optionList = new ArrayList<>();
                for (JsonNode opt : options) {
                    optionList.add(opt.asText());
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("question", question.asText());
                map.put("options", optionList);
                map.put("answer", answer.asText());
                result.add(map);
                if (result.size() >= count) {
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            log.warn("AI 出题 JSON 解析失败", e);
            return null;
        }
    }

    /** 剔除 markdown 代码围栏，只保留 JSON 数组片段，便于稳健解析 */
    private String extractJsonArray(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        text = text.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /** 内置 5 题题库模板（出题解析失败时兜底） */
    private List<Map<String, Object>> builtinQuiz(int count) {
        List<Map<String, Object>> all = new ArrayList<>();
        all.add(quizItem("光在真空中的传播速度约为多少？",
                "每秒 30 万公里", "每秒 3 万公里", "每秒 3000 公里", "每秒 30 亿公里", "每秒 30 万公里"));
        all.add(quizItem("水在标准大气压下的沸点是多少？",
                "0℃", "50℃", "100℃", "200℃", "100℃"));
        all.add(quizItem("太阳系中体积最大的行星是哪一颗？",
                "地球", "火星", "木星", "土星", "木星"));
        all.add(quizItem("中国最长的河流是哪一条？",
                "黄河", "长江", "珠江", "淮河", "长江"));
        all.add(quizItem("下面哪一种动物属于两栖动物？",
                "青蛙", "乌龟", "鳄鱼", "金鱼", "青蛙"));
        int size = Math.max(0, Math.min(count, all.size()));
        return new ArrayList<>(all.subList(0, size));
    }

    private Map<String, Object> quizItem(String question, String a, String b, String c, String d, String answer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("question", question);
        map.put("options", Arrays.asList(a, b, c, d));
        map.put("answer", answer);
        return map;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /** 简约每分钟固定窗口限流器（按 userId 维度） */
    private static class RateLimiter {
        private final int limit;
        private final AtomicInteger count = new AtomicInteger();
        private volatile long windowStart;

        private RateLimiter(int limit) {
            this.limit = limit;
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000L) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
