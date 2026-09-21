package com.edu.aienlighten.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 实验场景策略（一行一个场景）。
 *
 * <p>这是管理端「实验资源」页三个实验卡片的真实数据源。与 {@code ai_config.content_filter}
 * 的关系是：{@code content_filter} 为总开关，为 0 时本表策略一律不生效。</p>
 */
@Data
@TableName("ai_scene_policy")
public class AiScenePolicy {

    /** 场景标识 */
    public static final String SCENE_WRITING = "writing";
    public static final String SCENE_CHAT = "chat";
    public static final String SCENE_QUIZ = "quiz";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** writing / chat / quiz */
    private String scene;

    /** 展示名，如「创意写作实验室」 */
    private String sceneName;

    /** 该实验的默认风格 */
    private String defaultStyle;

    /** 单次生成数量：quiz=出题数，writing=生成篇数，chat=不适用（0） */
    private Integer genCount;

    /** 答题限时（秒），仅 quiz 生效，0=不限时 */
    private Integer answerLimitSec;

    /** 输入侧合规检测 */
    private Integer inputFilter;

    /** 输出敏感词过滤 */
    private Integer outputSensitive;

    /** 生成内容准确性校验 */
    private Integer accuracyCheck;

    /** 未成年人适宜性评估 */
    private Integer minorSuitability;

    /** 命中即自动拦截（关闭则只替换/记录） */
    private Integer autoBlock;

    private LocalDateTime updatedAt;

    /** 开关语义：只有显式等于 1 才算开启，避免 null 被当成开启 */
    public static boolean on(Integer flag) {
        return flag != null && flag == 1;
    }

    public boolean inputOn() {
        return on(inputFilter);
    }

    public boolean outputSensitiveOn() {
        return on(outputSensitive);
    }

    public boolean accuracyOn() {
        return on(accuracyCheck);
    }

    public boolean suitabilityOn() {
        return on(minorSuitability);
    }

    public boolean autoBlockOn() {
        return on(autoBlock);
    }
}
