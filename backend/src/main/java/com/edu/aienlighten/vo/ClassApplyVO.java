package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 教师端入班申请列表 VO */
@Data
public class ClassApplyVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNickname;
    private Long classId;
    private String className;
    /** 0待审 1同意 2拒绝 */
    private Integer status;
    private LocalDateTime createdAt;
}
