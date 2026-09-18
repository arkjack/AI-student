package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewDTO {

    @NotNull(message = "分数不能为空")
    private Integer score;

    private String feedback;
}
