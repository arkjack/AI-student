package com.edu.aienlighten.dto;

import lombok.Data;

/** AI 实验场景策略（管理端「实验资源」页保存用） */
@Data
public class ScenePolicyDTO {

    /** writing / chat / quiz */
    private String scene;

    private String defaultStyle;

    /** 单次生成数量：quiz=出题数，writing=生成篇数，chat=不适用（0） */
    private Integer genCount;

    /** 答题限时（秒），仅 quiz 生效，0=不限时 */
    private Integer answerLimitSec;

    private Integer inputFilter;

    private Integer outputSensitive;

    private Integer accuracyCheck;

    private Integer minorSuitability;

    private Integer autoBlock;
}
