package com.edu.aienlighten.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 教师 / 管理员对 AI 生成内容的人工复核入参 */
@Data
public class ContentReviewDTO {

    /** 2 复核通过（内容可保留），3 复核驳回（内容需下架） */
    @NotNull(message = "复核结论不能为空")
    @Min(value = 2, message = "复核结论取值 2 通过 / 3 驳回")
    @Max(value = 3, message = "复核结论取值 2 通过 / 3 驳回")
    private Integer result;

    @Size(max = 255, message = "复核意见不能超过 255 字")
    private String remark;
}
