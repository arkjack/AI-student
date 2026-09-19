package __PKG__.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import __PKG__.common.BizException;
import __PKG__.dto.LoginDTO;
import __PKG__.dto.RegisterDTO;
import __PKG__.entity.User;
import __PKG__.mapper.UserMapper;
import __PKG__.security.JwtUtil;
import __PKG__.service.UserService;
import __PKG__.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * BCrypt 是无状态工具，做成静态常量复用。
     * 每次 new 一个会重复做强度初始化，没有必要。
     */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        // 账号不存在与密码错误返回同一文案，避免账号枚举
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
        if (count != null && count > 0) {
            throw new BizException(1005, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 只存散列，永不存明文
        user.setPassword(ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRealName(dto.getNickname());
        user.setAvatar("🦊");
        user.setRole(2);          // 注册即学生
        user.setStatus(1);
        userMapper.insert(user);

        return toVO(user);
    }

    @Override
    public User getById(Long id) {
        return id == null ? null : userMapper.selectById(id);
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
