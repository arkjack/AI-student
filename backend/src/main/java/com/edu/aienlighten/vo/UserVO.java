package com.edu.aienlighten.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户信息 VO（不含密码） */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String nickname;
    private String avatar;
    /** 0管理员 1教师 2学生 */
    private Integer role;
    private String subject;
    /** 1启用 0禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    /** 学生所在班级 id（仅学生） */
    private Long classId;
    /** 学生所在班级名称（仅学生） */
    private String className;
}
