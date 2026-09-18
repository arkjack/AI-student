package com.edu.aienlighten.security;

import com.edu.aienlighten.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT 认证拦截器：解析 Bearer token → 写入 UserContext */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null) {
                UserContext.set(new LoginUser(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("role", Integer.class)
                ));
                return true;
            }
        }
        // 未登录（非白名单接口）→ 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
