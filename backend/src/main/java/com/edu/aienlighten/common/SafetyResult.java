package com.edu.aienlighten.common;

import lombok.Data;

import java.util.List;

/**
 * 内容安全检测结果。
 *
 * <p>输入侧与输出侧共用同一个结果对象：输入侧只看 {@link #blocked}，
 * 输出侧看 {@link #blocked} / {@link #replaced} 与处置后的 {@link #text}。</p>
 */
@Data
public class SafetyResult {

    /** 是否应中断本次调用（输入侧命中时为 true，调用方据此拒绝调用大模型） */
    private boolean blocked;

    /** 输出是否被替换为安全文案 */
    private boolean replaced;

    /** 命中的处理层级，取值见 {@code AiInteractionLog.STAGE_*}；0 表示通过 */
    private int stage;

    /** 处置动作，取值见 {@code AiInteractionLog.ACTION_*} */
    private String action = "pass";

    /** 处置后的文本；输入侧为给用户看的提示语，输出侧为可安全展示的内容 */
    private String text;

    /** 命中的敏感词（去重、最多保留 10 个），仅用于日志与复核展示 */
    private List<String> hitWords = List.of();

    /** 未成年人适宜性风险分 0-100 */
    private int riskScore;

    /** 处置原因，展示给复核教师 */
    private String reason;

    /** 给学生看的友好提示，仅在被拦截/替换时有值 */
    private String tip;

    /** 是否需要教师人工复核 */
    private boolean needReview;

    public static SafetyResult pass(String text) {
        SafetyResult r = new SafetyResult();
        r.text = text;
        return r;
    }

    public static SafetyResult block(int stage, String reason, String tip,
                                     List<String> hitWords, int riskScore) {
        SafetyResult r = new SafetyResult();
        r.blocked = true;
        r.stage = stage;
        r.action = "block";
        r.reason = reason;
        r.tip = tip;
        r.text = tip;
        r.hitWords = hitWords;
        r.riskScore = riskScore;
        r.needReview = true;
        return r;
    }

    public static SafetyResult replace(int stage, String reason, String tip, String text,
                                       List<String> hitWords, int riskScore) {
        SafetyResult r = new SafetyResult();
        r.replaced = true;
        r.stage = stage;
        r.action = "replace";
        r.reason = reason;
        r.tip = tip;
        r.text = text;
        r.hitWords = hitWords;
        r.riskScore = riskScore;
        r.needReview = true;
        return r;
    }

    /** 放行但打上标记：内容已发给学生，同时进教师复核队列 */
    public static SafetyResult flag(int stage, String reason, String text, int riskScore) {
        return flag(stage, reason, text, riskScore, List.of());
    }

    /**
     * 放行但打上标记，并带上命中词。
     *
     * <p>命中词必须一起带出来：观察模式下走的全是这条分支，如果只记风险分不记命中词，
     * 复核页上就会出现「风险分 75 但命中词为空」这种查不下去的记录。</p>
     */
    public static SafetyResult flag(int stage, String reason, String text, int riskScore,
                                    List<String> hitWords) {
        SafetyResult r = new SafetyResult();
        r.stage = stage;
        r.action = "pass";
        r.reason = reason;
        r.text = text;
        r.riskScore = riskScore;
        r.hitWords = hitWords == null ? List.of() : hitWords;
        r.needReview = true;
        return r;
    }
}
