package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassStudentAddDTO {

    @NotNull(message = "学生 id 不能为空")
    private Long studentId;
}
