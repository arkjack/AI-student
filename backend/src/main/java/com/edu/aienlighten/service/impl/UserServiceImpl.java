package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.LoginDTO;
import com.edu.aienlighten.dto.RegisterDTO;
import com.edu.aienlighten.entity.ClassApply;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassApplyMapper;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.JwtUtil;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.service.UserService;
import com.edu.aienlighten.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ClassApplyMapper classApplyMapper;
    private final ClassInfoMapper classInfoMapper;
    private final NotificationService notificationService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(1003, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(1004, "账号已被禁用，请联系管理员");
        }
        return toVO(user);
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(1005, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRealName(dto.getNickname());
        user.setAvatar("🦊");
        user.setRole(2); // 注册即学生
        user.setStatus(1);
        userMapper.insert(user);
        // 选择了班级则发起入班申请（待教师审批，通过后才正式入班）
        if (dto.getClassId() != null) {
            ClassApply apply = new ClassApply();
            apply.setStudentId(user.getId());
            apply.setClassId(dto.getClassId());
            apply.setStatus(0);
            classApplyMapper.insert(apply);
            // 通知该班教师：有新的入班申请
            ClassInfo cls = classInfoMapper.selectById(dto.getClassId());
            if (cls != null && cls.getTeacherId() != null) {
                notificationService.push(cls.getTeacherId(), "apply", "入班申请",
                        "学生 " + (user.getRealName() == null ? user.getNickname() : user.getRealName()) + " 申请加入班级，请及时审批",
                        "/teacher/classes");
            }
        }
        return toVO(user);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    private LoginVO toVO(User user) {
        return LoginVO.builder()
                .token(jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole()))
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}
