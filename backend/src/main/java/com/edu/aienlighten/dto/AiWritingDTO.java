package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 创意写作入参 */
@Data
public class AiWritingDTO {

    /** 写作主题。限制长度既是防超长提示词烧 token，也是内容安全的第一道栅栏 */
    @NotBlank(message = "主题不能为空")
    @Size(max = 50, message = "写作主题不能超过 50 个字")
    private String topic;

    /** 写作风格（小学童话/科幻等） */
    @NotBlank(message = "风格不能为空")
    @Size(max = 20, message = "风格名称不能超过 20 个字")
    private String style;

    /** 目标字数，默认 300 */
    private Integer length;
}
