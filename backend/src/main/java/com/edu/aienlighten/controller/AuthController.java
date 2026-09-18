package com.edu.aienlighten.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.LoginDTO;
import com.edu.aienlighten.dto.RegisterDTO;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.UserService;
import com.edu.aienlighten.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClassInfoMapper classInfoMapper;

    /** 登录（公开） */
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /** 注册（公开，默认学生） */
    @PostMapping("/auth/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    /** 当前登录用户信息 */
    @GetMapping("/user/me")
    public Result<User> me() {
        return Result.ok(userService.getById(UserContext.userId()));
    }

    /** 公开：班级列表（注册选班用） */
    @GetMapping("/auth/classes")
    public Result<List<ClassInfo>> classes() {
        return Result.ok(classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .orderByAsc(ClassInfo::getId)));
    }
}
