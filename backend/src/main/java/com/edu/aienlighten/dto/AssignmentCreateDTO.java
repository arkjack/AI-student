package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentCreateDTO {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    @NotEmpty(message = "班级列表不能为空")
    private List<Long> classIds;

    /** 1课程 2编程 3AI实验 4闯关 */
    private Integer type;

    private Long resourceId;

    private String content;

    private LocalDateTime deadline;
}
