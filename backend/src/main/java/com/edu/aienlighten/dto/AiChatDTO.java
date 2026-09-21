package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 智能答疑入参 */
@Data
public class AiChatDTO {

    /** 学生提问内容。限制长度既是防超长提示词烧 token，也是内容安全的第一道栅栏 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 200, message = "提问不能超过 200 个字")
    private String question;
}
