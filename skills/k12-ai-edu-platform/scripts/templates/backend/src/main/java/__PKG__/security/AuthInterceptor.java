package __PKG__.security;

import __PKG__.entity.User;
import __PKG__.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;
import java.util.Date;

/** JWT 认证拦截器：解析 Bearer token → 校验账号状态 → 写入 UserContext */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null && isTokenUsable(claims)) {
                UserContext.set(new LoginUser(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("role", Integer.class)
                ));
                return true;
            }
        }
        // 未登录 / 登录态已失效（非白名单接口）→ 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * token 是否仍然可用。除签名与有效期（{@code parse} 已校验）外还需满足：
     * <ol>
     *   <li>账号仍然存在且未被禁用 —— 否则已删除账号手里的 token 会一直有效；</li>
     *   <li>密码未在该 token 签发之后被修改过 —— 改密即等价于「下线所有旧设备」。</li>
     * </ol>
     *
     * <p>时间比较刻意用「严格早于」：JWT 的 {@code iat} 只精确到秒，
     * 若写成 {@code <=}，用户在改密后同一秒内重新登录拿到的新 token 也会被判为失效。
     *
     * <p>代价是每个已登录请求多一次主键查询（MyBatis-Plus 一级缓存之外），
     * 在本项目的数据量级下可忽略；若将来 QPS 上来，可换成 userId → 改密时间的带过期缓存。
     */
    private boolean isTokenUsable(Claims claims) {
        Long uid;
        try {
            uid = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return false;
        }
        User user = userMapper.selectById(uid);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            return false;
        }
        Date issuedAt = claims.getIssuedAt();
        if (user.getPasswordChangedAt() != null && issuedAt != null) {
            long iat = issuedAt.toInstant().getEpochSecond();
            long pwdChangedAt = user.getPasswordChangedAt()
                    .atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            return iat >= pwdChangedAt;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
