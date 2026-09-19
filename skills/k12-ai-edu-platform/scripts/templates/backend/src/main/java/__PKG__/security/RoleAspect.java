package __PKG__.security;

import __PKG__.common.BizException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** 角色权限切面：校验 @RequireRole，未登录/角色不符 → 401/403
 *  （方法级注解与类级注解分开定义切点，避免 OR 表达式绑定问题） */
@Aspect
@Component
public class RoleAspect {

    @Before("@annotation(requireRole)")
    public void checkMethod(RequireRole requireRole) {
        check(requireRole);
    }

    @Before("@within(requireRole)")
    public void checkClass(RequireRole requireRole) {
        check(requireRole);
    }

    private void check(RequireRole requireRole) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException(401, "请先登录");
        }
        if (Arrays.stream(requireRole.value()).noneMatch(r -> r == user.getRole())) {
            throw new BizException(403, "无权限访问");
        }
    }
}
