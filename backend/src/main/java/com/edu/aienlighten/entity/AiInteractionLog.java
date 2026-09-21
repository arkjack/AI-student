package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 交互日志与内容复核记录。
 *
 * <p>每次 AI 调用都写一行：通过、被拦截、网络降级都写。
 * {@code hitStage} 标明这次调用在哪一层被处理，{@code reviewStatus} 标明是否需要教师人工复核 ——
 * 一张表即可完整回答「这次调用到底发生了什么」，满足任务书「交互日志留存可追溯」的要求。</p>
 */
@Data
@TableName("ai_interaction_log")
public class AiInteractionLog {

    /** hitStage 取值 */
    public static final int STAGE_PASS = 0;
    public static final int STAGE_INPUT = 1;
    public static final int STAGE_SENSITIVE = 2;
    public static final int STAGE_SUITABILITY = 3;
    public static final int STAGE_ACCURACY = 4;
    public static final int STAGE_FALLBACK = 5;

    /** action 取值 */
    public static final String ACTION_PASS = "pass";
    public static final String ACTION_REPLACE = "replace";
    public static final String ACTION_BLOCK = "block";
    public static final String ACTION_FALLBACK = "fallback";

    /** reviewStatus 取值 */
    public static final int REVIEW_NONE = 0;
    public static final int REVIEW_PENDING = 1;
    public static final int REVIEW_PASSED = 2;
    public static final int REVIEW_REJECTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 0管理员 1教师 2学生 */
    private Integer userRole;

    /** writing / chat / quiz */
    private String scene;

    private String inputText;

    /** AI 原始返回（未处置前），供教师复核时对照 */
    private String outputText;

    private String model;

    private Integer hitStage;

    private String hitWords;

    private Integer riskScore;

    private String action;

    private String reason;

    private Integer reviewStatus;

    private Long reviewerId;

    private String reviewRemark;

    private LocalDateTime reviewedAt;

    private Integer elapsedMs;

    private LocalDateTime createdAt;
}
