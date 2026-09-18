package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 作业提交列表 VO（含学生名） */
@Data
public class AssignmentSubmissionVO {

    private Long submissionId;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    /** 1已提交 2已批改 */
    private Integer status;
    /** 提交内容 */
    private String content;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
