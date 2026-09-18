package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QaAskDTO {

    @NotBlank(message = "问题不能为空")
    private String question;
}
