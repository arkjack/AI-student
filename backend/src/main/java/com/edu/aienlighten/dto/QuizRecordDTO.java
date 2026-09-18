package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizRecordDTO {

    @NotBlank(message = "关卡不能为空")
    private String level;

    private Integer score;

    private Integer correctCount;

    private Integer totalCount;

    /** 关联的作业任务 id（从作业中心进入时携带） */
    private Long assignmentId;
}
