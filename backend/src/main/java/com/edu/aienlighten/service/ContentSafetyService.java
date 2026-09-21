package com.edu.aienlighten.service;

import com.edu.aienlighten.common.SafetyResult;
import com.edu.aienlighten.dto.ScenePolicyDTO;
import com.edu.aienlighten.entity.AiScenePolicy;

import java.util.List;
import java.util.Map;

/**
 * 内容安全服务：任务书要求的「三重前置过滤」在代码里的落点。
 *
 * <p>调用顺序（由 {@code AiServiceImpl.doChat} 串起来）：</p>
 * <pre>
 *   学生输入 ──► checkInput   ──命中──► 抛 1201，不调用大模型
 *                   │未命中
 *                   ▼
 *              调用 DeepSeek
 *                   │
 *                   ▼
 *              checkOutput ──► ① 敏感词过滤 ② 未成年人适宜性评估 ③ 准确性校验
 * </pre>
 */
public interface ContentSafetyService {

    /**
     * 输入侧合规检测。命中时返回 {@code blocked=true} 的结果，
     * 由调用方中断本次请求，保证「违规指令不进入大模型」。
     *
     * @param policy 场景策略，为 null 时视为全关，直接放行
     * @param text   学生输入原文（写作主题 / 提问内容）
     */
    SafetyResult checkInput(AiScenePolicy policy, String text);

    /**
     * 输出侧三重过滤。返回结果中的 {@code text} 是**可直接发给学生**的最终内容：
     * 通过时为原文，被替换时为安全文案。
     *
     * @param policy 场景策略
     * @param text   AI 返回的原始内容
     */
    SafetyResult checkOutput(AiScenePolicy policy, String text);

    /**
     * 出题结果的准确性结构校验：剔除 answer 不在选项内、选项重复或题干过短的题目。
     * 全部不合格时返回空列表，由调用方回退内置题库。
     */
    List<Map<String, Object>> validateQuiz(AiScenePolicy policy, List<Map<String, Object>> questions);

    /** 读取场景策略；库中缺行时自动补默认行并返回，调用方无需处理空值 */
    AiScenePolicy policyOf(String scene);

    /**
     * 观察模式：复制一份策略并把「自动拦截」全部关掉。
     *
     * <p>返回的是副本，数据库里的配置**不会被改动**，退出观察模式后自动恢复。
     * 之所以这样做而不是简单地把内容安全整个跳过：观察模式下检测、打分、日志全都照常，
     * 只是不再真的拦截学生——这样既能看到真实的命中情况（用来评估误杀率），
     * 又不会留下审计空白。</p>
     */
    AiScenePolicy policyForObserve(AiScenePolicy src);

    /** 三个场景的策略，顺序固定为 writing / chat / quiz */
    List<AiScenePolicy> allPolicies();

    /** 保存场景策略（按 scene upsert，只更新传入的字段） */
    void savePolicies(List<ScenePolicyDTO> list);

    /** 词库增删改后调用，让敏感词匹配器的缓存立即失效 */
    void refreshWordCache();
}
