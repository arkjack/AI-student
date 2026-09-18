package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 任务提交进度 VO（每个学生） */
@Data
public class AssignmentProgressVO {

    private Long studentId;
    private String studentName;
    /** 是否已提交 */
    private Boolean submitted;
    /** 1已提交 2已批改；未提交为 null */
    private Integer status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
