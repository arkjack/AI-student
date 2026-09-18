package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI 智能答疑入参 */
@Data
public class AiChatDTO {

    /** 学生提问内容 */
    @NotBlank(message = "问题不能为空")
    private String question;
}
