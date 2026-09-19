package __PKG__.controller;

import __PKG__.common.Result;
import __PKG__.dto.LoginDTO;
import __PKG__.dto.RegisterDTO;
import __PKG__.entity.User;
import __PKG__.security.UserContext;
import __PKG__.service.UserService;
import __PKG__.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>⚠️ 本 Controller 下的路径必须在 WebConfig 的 excludePathPatterns 白名单里，
 * 否则未登录时调登录接口本身就会被拦截器拦掉（死循环）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 登录（公开） */
    @PostMapping("/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /** 注册（公开，默认学生角色） */
    @PostMapping("/auth/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    /** 当前登录用户信息（需登录；User.password 上有 @JsonIgnore，散列不会下发） */
    @GetMapping("/user/me")
    public Result<User> me() {
        return Result.ok(userService.getById(UserContext.userId()));
    }
}
