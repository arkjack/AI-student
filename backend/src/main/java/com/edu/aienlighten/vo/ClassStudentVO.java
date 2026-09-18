package com.edu.aienlighten.vo;

import lombok.Data;

/** 班级学生 VO */
@Data
public class ClassStudentVO {

    /** class_student.id */
    private Long id;
    private Long studentId;
    private String username;
    private String realName;
    private String nickname;
    private String avatar;
    private String subject;
}
