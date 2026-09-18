package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 注册默认是学生角色 */
    private String className;

    /** 选择的班级 id（非空则发起入班申请，待教师审批） */
    private Long classId;
}
