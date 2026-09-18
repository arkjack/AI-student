package com.edu.aienlighten.vo;

import lombok.Data;

/** 学生学习概况 VO */
@Data
public class StudentProgressVO {

    private Long studentId;
    private String realName;
    private String nickname;
    private String avatar;
    /** 已完成课程数 */
    private Long completedCourses;
    /** 已提交作业数 */
    private Long submissions;
}
