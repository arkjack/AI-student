package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 交互日志条目：管理端「内容安全」页与教师端「内容复核」页共用同一个视图对象。
 *
 * <p>同时带上学生姓名与各枚举的中文说明，前端可以直接渲染，不必再维护一份映射表。</p>
 */
@Data
public class AiInteractionLogVO {

    private Long id;

    private Long userId;

    /** 发起调用的学生姓名（realName 缺失时回退 nickname） */
    private String studentName;

    private String avatar;

    private Integer userRole;

    /** writing / chat / quiz */
    private String scene;

    private String sceneName;

    /** 学生输入原文（写作主题或提问） */
    private String inputText;

    /** AI 原始返回，未处置前的内容 */
    private String outputText;

    private String model;

    private Integer hitStage;

    private String hitStageText;

    private String hitWords;

    private Integer riskScore;

    private String action;

    private String actionText;

    private String reason;

    private Integer reviewStatus;

    private String reviewStatusText;

    private Long reviewerId;

    private String reviewerName;

    private String reviewRemark;

    private LocalDateTime reviewedAt;

    private Integer elapsedMs;

    private LocalDateTime createdAt;

    public static String stageText(Integer stage) {
        if (stage == null) {
            return "未知";
        }
        return switch (stage) {
            case 1 -> "输入侧检测";
            case 2 -> "敏感词过滤";
            case 3 -> "适宜性评估";
            case 4 -> "准确性校验";
            case 5 -> "网络降级";
            default -> "通过";
        };
    }

    public static String actionText(String action) {
        if (action == null) {
            return "未知";
        }
        return switch (action) {
            case "replace" -> "替换为安全文案";
            case "block" -> "拦截";
            case "fallback" -> "降级兜底";
            default -> "放行";
        };
    }

    public static String reviewText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "待复核";
            case 2 -> "复核通过";
            case 3 -> "复核驳回";
            default -> "无需复核";
        };
    }

    public static String sceneName(String scene) {
        if (scene == null) {
            return "未知场景";
        }
        return switch (scene) {
            case "chat" -> "智能答疑实验室";
            case "quiz" -> "知识闯关实验室";
            case "writing" -> "创意写作实验室";
            default -> scene;
        };
    }
}
