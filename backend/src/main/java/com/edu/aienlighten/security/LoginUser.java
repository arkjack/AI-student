package com.edu.aienlighten.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录用户上下文对象 */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private Integer role; // 0管理员 1教师 2学生
}
