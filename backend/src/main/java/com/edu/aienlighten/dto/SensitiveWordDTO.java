package com.edu.aienlighten.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 敏感词新增 / 编辑入参 */
@Data
public class SensitiveWordDTO {

    @NotBlank(message = "敏感词不能为空")
    @Size(max = 50, message = "敏感词长度不能超过 50")
    private String word;

    /** 1涉政 2色情 3暴力 4赌博毒品 5迷信诈骗 6广告导流 7其他 */
    @Min(value = 1, message = "分类取值 1-7")
    @Max(value = 7, message = "分类取值 1-7")
    private Integer category;

    /** 1提示 2替换 3拦截 */
    @Min(value = 1, message = "级别取值 1-3")
    @Max(value = 3, message = "级别取值 1-3")
    private Integer level;

    /** 1启用 0停用，缺省为启用 */
    private Integer enabled;

    @Size(max = 120, message = "备注不能超过 120 字")
    private String remark;
}
