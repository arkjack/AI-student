package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassCreateDTO {

    @NotBlank(message = "班级名称不能为空")
    private String name;

    private String grade;
    private String description;
}
