package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 教师端任务列表 VO（含班级名、资源名、提交统计） */
@Data
public class AssignmentVO {

    private Long id;
    private String title;
    private Integer type;
    private Long classId;
    private String className;
    private Long resourceId;
    private String resourceName;
    private String content;
    private LocalDateTime deadline;
    private Integer status;
    /** 已提交人数 */
    private Long submissionCount;
    /** 班级总人数 */
    private Long totalCount;
}
