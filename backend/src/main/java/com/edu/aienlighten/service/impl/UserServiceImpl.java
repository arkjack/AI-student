package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.ChangePasswordDTO;
import com.edu.aienlighten.dto.LoginDTO;
import com.edu.aienlighten.dto.RegisterDTO;
import com.edu.aienlighten.dto.UserPreferenceDTO;
import com.edu.aienlighten.entity.ClassApply;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.ClassApplyMapper;
import com.edu.aienlighten.mapper.ClassInfoMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.JwtUtil;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.service.UserService;
import com.edu.aienlighten.vo.LoginVO;
import com.edu.aienlighten.vo.UserPreferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ClassApplyMapper classApplyMapper;
    private final ClassInfoMapper classInfoMapper;
    private final NotificationService notificationService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 新密码强度：8~32 位、同时含字母与数字、不含空格与中文。
     * 与 {@link ChangePasswordDTO} 上的注解保持一致，service 层再挡一次 ——
     * 防止将来有人绕过 Controller 直接调 service 时强度校验失守。
     */
    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,32}$");

    /** 原密码连续校验失败上限与统计窗口（防暴力破解） */
    private static final int MAX_FAIL = 5;
    private static final long FAIL_WINDOW_MS = 15 * 60 * 1000L;
    /** userId → [失败次数, 窗口起始毫秒] */
    private final Map<Long, long[]> failWindow = new ConcurrentHashMap<>();

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

    /* ============================================================
       修改密码
       ------------------------------------------------------------
       安全要点：
         ① 必须已登录（路由 /api/user/** 不在 AuthInterceptor 的白名单内）
         ② 必须校验原密码，失败文案统一为「原密码不正确」，不区分账号是否存在 → 防账号枚举
         ③ BCryptPasswordEncoder.matches 内部为恒定时间比较，无短路时序泄露
         ④ 同一用户 15 分钟内连续失败 5 次即锁定，防在线暴力破解
         ⑤ 成功后写 password_changed_at，AuthInterceptor 会据此作废改密前签发的所有 token
         ⑥ 全流程不打印、不返回任何密码或散列
       ============================================================ */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long uid = UserContext.userId();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        assertNotLocked(uid);

        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(401, "请先登录");
        }

        // ① 原密码校验
        if (dto.getOldPassword() == null || !ENCODER.matches(dto.getOldPassword(), user.getPassword())) {
            recordFail(uid);
            throw new BizException(1006, "原密码不正确");
        }

        // ② 新密码强度（兜底再校验一次）
        String newPwd = dto.getNewPassword();
        if (newPwd == null || !STRONG_PASSWORD.matcher(newPwd).matches()) {
            throw new BizException(1002, "新密码需为 8~32 位，且同时包含字母和数字");
        }

        // ③ 新旧密码不得相同
        if (ENCODER.matches(newPwd, user.getPassword())) {
            throw new BizException(1007, "新密码不能与原密码相同");
        }

        // ④ 落库：只更新密码与改密时间（MyBatis-Plus 默认忽略 null 字段，其他字段不受影响）
        User upd = new User();
        upd.setId(uid);
        upd.setPassword(ENCODER.encode(newPwd));
        upd.setPasswordChangedAt(LocalDateTime.now());
        userMapper.updateById(upd);

        failWindow.remove(uid);
    }

    @Override
    public UserPreferenceVO getPreferences() {
        User user = requireCurrentUser();
        return new UserPreferenceVO(isOn(user.getNotifyEnabled()), isOn(user.getRemindEnabled()));
    }

    @Override
    public UserPreferenceVO updatePreferences(UserPreferenceDTO dto) {
        Long uid = UserContext.userId();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        User upd = new User();
        upd.setId(uid);
        if (dto.getNotifyEnabled() != null) {
            upd.setNotifyEnabled(dto.getNotifyEnabled() ? 1 : 0);
        }
        if (dto.getRemindEnabled() != null) {
            upd.setRemindEnabled(dto.getRemindEnabled() ? 1 : 0);
        }
        if (upd.getNotifyEnabled() != null || upd.getRemindEnabled() != null) {
            userMapper.updateById(upd);
        }
        return getPreferences();
    }

    /* ---------------- 内部工具 ---------------- */

    private User requireCurrentUser() {
        Long uid = UserContext.userId();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(401, "请先登录");
        }
        return user;
    }

    /** 开关为 null 时按「开启」处理，兼容早期没有该字段的数据 */
    private boolean isOn(Integer flag) {
        return flag == null || flag == 1;
    }

    private void assertNotLocked(Long uid) {
        long[] w = failWindow.get(uid);
        if (w == null) {
            return;
        }
        synchronized (w) {
            long now = System.currentTimeMillis();
            if (now - w[1] > FAIL_WINDOW_MS) {
                failWindow.remove(uid);
                return;
            }
            if (w[0] >= MAX_FAIL) {
                long restMin = (FAIL_WINDOW_MS - (now - w[1])) / 60000 + 1;
                throw new BizException(1008, "原密码错误次数过多，请 " + restMin + " 分钟后再试");
            }
        }
    }

    private void recordFail(Long uid) {
        long[] w = failWindow.computeIfAbsent(uid, k -> new long[]{0L, 0L});
        synchronized (w) {
            long now = System.currentTimeMillis();
            if (w[1] == 0L || now - w[1] > FAIL_WINDOW_MS) {
                w[0] = 0L;
                w[1] = now;
            }
            w[0]++;
        }
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
