package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 答疑记录 VO */
@Data
public class QaVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String question;
    private String answer;
    /** 1AI 2教师 */
    private Integer answerType;
    private Long teacherId;
    private String teacherName;
    private LocalDateTime createdAt;
}
