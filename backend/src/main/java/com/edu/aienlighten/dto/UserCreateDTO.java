package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotNull(message = "角色不能为空")
    /** 0管理员 1教师 2学生 */
    private Integer role;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String nickname;

    /** 教师学科 */
    private String subject;

    /** 学生加入的班级名称（可选，按名称匹配） */
    private String className;

    /** 学生加入的班级 id（可选，优先按 id 匹配） */
    private Long classId;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}
