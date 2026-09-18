package com.edu.aienlighten.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private String realName;
    private String nickname;
    private String subject;
    /** 学生班级 id（非空则调整班级归属） */
    private Long classId;
    /** 新密码（非空则重置密码） */
    private String password;
}
