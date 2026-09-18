package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.UserCreateDTO;
import com.edu.aienlighten.dto.UserStatusDTO;
import com.edu.aienlighten.dto.UserUpdateDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.AdminUserService;
import com.edu.aienlighten.vo.PageVO;
import com.edu.aienlighten.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端 · 用户管理 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** 分页查询用户 */
    @GetMapping
    @RequireRole({0})
    public Result<PageVO<UserVO>> page(@RequestParam(required = false) Integer role,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(adminUserService.page(role, keyword, page, size));
    }

    /** 新增用户 */
    @PostMapping
    @RequireRole({0})
    public Result<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.ok(adminUserService.create(dto));
    }

    /** 编辑用户（不改密码） */
    @PutMapping("/{id}")
    @RequireRole({0})
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        adminUserService.update(id, dto);
        return Result.ok();
    }

    /** 启用/禁用 */
    @PutMapping("/{id}/status")
    @RequireRole({0})
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        adminUserService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    /** 删除用户（管理员不可删） */
    @DeleteMapping("/{id}")
    @RequireRole({0})
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.ok();
    }
}
