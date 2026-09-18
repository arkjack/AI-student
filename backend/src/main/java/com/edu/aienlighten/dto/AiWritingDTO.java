package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI 创意写作入参 */
@Data
public class AiWritingDTO {

    /** 写作主题 */
    @NotBlank(message = "主题不能为空")
    private String topic;

    /** 写作风格（小学童话/科幻等） */
    @NotBlank(message = "风格不能为空")
    private String style;

    /** 目标字数，默认 300 */
    private Integer length;
}
