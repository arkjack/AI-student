package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.ChangePasswordDTO;
import com.edu.aienlighten.dto.UserPreferenceDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.UserService;
import com.edu.aienlighten.vo.UserPreferenceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的设置类接口（个人中心「设置」区）。
 *
 * <p>⚠️ 路径必须避开 {@code /api/auth/**}：该前缀在 WebConfig 里被 AuthInterceptor 整体放行
 * （登录、注册是公开接口），而修改密码属于敏感操作，必须要求已登录。
 * 放在 {@code /api/user/**} 下即可由拦截器强制鉴权。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 读取偏好设置（学习提醒 / 消息通知） */
    @GetMapping("/preferences")
    @RequireRole({0, 1, 2})
    public Result<UserPreferenceVO> preferences() {
        return Result.ok(userService.getPreferences());
    }

    /** 更新偏好设置 */
    @PutMapping("/preferences")
    @RequireRole({0, 1, 2})
    public Result<UserPreferenceVO> updatePreferences(@RequestBody UserPreferenceDTO dto) {
        return Result.ok(userService.updatePreferences(dto));
    }

    /**
     * 修改密码。必须先通过原密码校验；成功后此前签发的 token 全部失效，
     * 前端应清掉本地登录态并跳回登录页。
     */
    @PostMapping("/change-password")
    @RequireRole({0, 1, 2})
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }
}
