package com.edu.aienlighten.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求。
 *
 * <p>注意：新密码的强度规则在此声明为 Bean Validation，由 {@code @Valid} 触发，
 * 参数不合法会由全局异常处理器转成业务码 1002；service 层仍会再校验一次，
 * 避免绕过 Controller 直接调 service 时失守。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "请输入原密码")
    @Size(max = 64, message = "原密码长度不合法")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 8, max = 32, message = "新密码长度需为 8~32 位")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]+$",
            message = "新密码需同时包含字母和数字，且不能含空格或中文"
    )
    private String newPassword;
}
