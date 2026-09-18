package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QaReplyDTO {

    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
