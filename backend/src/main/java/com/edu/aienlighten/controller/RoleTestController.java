package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.security.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 角色权限验证测试接口（P1 交付验证用，P2 将被真实业务接口取代） */
@RestController
@RequestMapping("/api/test")
public class RoleTestController {

    /** 仅教师/管理员可访问 */
    @GetMapping("/teacher")
    @RequireRole({0, 1})
    public Result<String> teacherOnly() {
        return Result.ok("教师/管理员接口访问成功");
    }

    /** 仅管理员可访问 */
    @GetMapping("/admin")
    @RequireRole({0})
    public Result<String> adminOnly() {
        return Result.ok("管理员接口访问成功");
    }
}
