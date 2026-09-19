# 后端骨架与约定

依赖版本已在 `scripts/templates/backend/pom.xml` 中固定，**直接用，不要自行升级**：

| 依赖 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.2.5 | Java 21 |
| MyBatis-Plus | `mybatis-plus-spring-boot3-starter` 3.5.7 | 注意是 **spring-boot3** 那个 artifact |
| jjwt | 0.12.6 | api + impl + jackson 三件套 |
| spring-security-crypto | 随 Boot | **只为用 BCrypt**，不引入完整 Spring Security |
| MySQL 驱动 | 随 Boot | `com.mysql:mysql-connector-j` |

> 不引入完整 Spring Security 是刻意选择：本项目角色固定为 3 个、权限静态，用「拦截器 + AOP 注解」约 60 行即可覆盖，配置成本和调试难度都远低于 Security 的过滤器链。**如果权限需要动态配置（角色-资源-操作三级可配），再换成 Spring Security。**

## 1. 统一返回体与异常

模板：[`common/Result.java`](scripts/templates/backend/src/main/java/__PKG__/common/Result.java)、[`common/BizException.java`](scripts/templates/backend/src/main/java/__PKG__/common/BizException.java)、[`common/GlobalExceptionHandler.java`](scripts/templates/backend/src/main/java/__PKG__/common/GlobalExceptionHandler.java)

```java
// 成功
return Result.ok(data);
return Result.ok();
// 失败：抛异常，由全局处理器转换，不要在 Controller 里 return Result.fail(...)
throw new BizException(1001, "当前资源已关闭请联系管理员");
```

**全局异常处理器必须显式处理这三类，否则会被兜底吞成「系统繁忙 9999」**：

| 异常 | 返回 | 为什么必须单独处理 |
|---|---|---|
| `MethodArgumentNotValidException` | `1002` + 具体字段提示 | 否则用户看到的是「系统繁忙」，不知道哪个参数错了 |
| `NoResourceFoundException` | `404` + 路径 | 调错路径时若被兜底成 9999，排查时极易误判为服务故障 |
| `HttpRequestMethodNotSupportedException` | `405` + 方法 | 常见于把 POST 接口当 GET 调 |

### 错误码表

| code | 含义 |
|---|---|
| `0` | 成功 |
| `401` | 未登录 / token 失效 |
| `403` | 已登录但无权限 |
| `404` | 接口不存在 |
| `405` | 请求方法不支持 |
| `1001` | 业务失败（默认），如「资源已关闭」 |
| `1002` | 参数校验失败 |
| `1003` | 用户名或密码错误 |
| `1101` | 大模型服务未配置或不可用 |
| `9999` | 未预期的系统异常 |

**约定**：`401` 与 `403` 语义必须严格分开——前端靠这个区分「踢回登录页」与「只弹提示」。混用会导致有权限问题的用户被反复踢出登录。

## 2. 认证与权限

### 2.1 两层结构

```
AuthInterceptor（HandlerInterceptor）  →  「有没有登录」
@RequireRole（自定义注解 + AOP 切面）   →  「这个角色能不能调这个接口」
```

`UserContext` 用 ThreadLocal 持有当前登录用户，请求结束时在 `afterCompletion` 里 `clear()`——**忘记清理会在复用线程池时串号**。

### 2.2 token 可用性判定（容易被忽略，务必照抄）

模板 [`security/AuthInterceptor.java`](scripts/templates/backend/src/main/java/__PKG__/security/AuthInterceptor.java) 里除了验签与过期时间，还额外校验两件事：

1. **账号仍然存在且未被禁用** —— 否则已删除/已封禁账号手里的 token 会一直有效到过期；
2. **密码未在 token 签发之后被修改** —— 改密即等价于「下线所有旧设备」。

```java
// 时间比较刻意用「严格早于」：JWT 的 iat 只精确到秒，
// 若写成 <=，用户在改密后同一秒内重新登录拿到的新 token 也会被判为失效。
return iat >= pwdChangedAt;
```

代价是每个请求多一次主键查询，在本类项目的数据量级下可忽略；QPS 上来后可换成带过期时间的缓存。

### 2.3 AOP 切面必须分开定义切点

```java
@Before("@annotation(requireRole)")   // 方法级
public void checkMethod(RequireRole requireRole) { check(requireRole); }

@Before("@within(requireRole)")       // 类级
public void checkClass(RequireRole requireRole) { check(requireRole); }
```

**不要写成 `@annotation(x) || @within(x)` 的合并切点**——绑定参数在 OR 表达式中会解析失败。

用法：

```java
@RestController
@RequestMapping("/api/admin/users")
@RequireRole(0)                       // 类级：整个 Controller 仅管理员
public class AdminUserController { ... }

@RequireRole({0, 1})                  // 方法级：管理员与教师
@GetMapping("/api/project/templates")
public Result<List<BlocklyTemplateVO>> templates() { ... }
```

### 2.4 拦截器白名单

```java
registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/**")     // 登录/注册/公开班级列表
        .excludePathPatterns("/api/public/**");  // 连通性自检等
```

新增公开接口时**必须**在这里放行，否则会被 401 拦掉。建议把公开接口统一收敛到 `/api/public/**` 前缀，减少白名单维护。

### 2.5 CORS

`WebConfig` 默认放行 `http://localhost:*` 与 `http://127.0.0.1:*`（Vite 端口可能变动）。

> ⚠️ 部署到公网后**必须**通过 `cors.allowed-origins` 配置真实域名。浏览器对同源 POST 也会带 `Origin` 头，漏配会导致登录等 POST 接口被 Spring 以 `403 Invalid CORS request` 拒绝，而 GET 却正常——这个现象非常容易误判为「后端权限写错了」。

## 3. 配置与密钥

`application.yml` 中**只放占位符**，真实值走环境变量或 gitignore 的本地配置：

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}   # 本地加载 application-local.yml（不入库）
  datasource:
    username: ${DB_USERNAME:ai_edu}
    password: ${DB_PASSWORD:change-me}
jwt:
  secret: ${JWT_SECRET:change-me-to-a-random-string-at-least-32-characters}
  expire-hours: 72
```

- 提供 `application-local.yml.example` 模板，`.gitignore` 忽略 `application-local.yml`；
- 建表脚本 `init.sql` 里的建库账号密码同样用 `CHANGE_ME_STRONG_PASSWORD` 占位；
- **提交前用 `verify.ps1` 扫描**，确保没有真实凭据进仓库。

## 4. 分层与事务

```
controller → service(impl) → mapper → MySQL
```

- **Controller 不写业务逻辑**，只做 `@Valid` 参数校验 + 调 service + 包 `Result`；
- **写操作想清楚事务边界**：涉及多表写入（如入班审批要同时改 `class_apply` 与插 `class_student`）必须加 `@Transactional`；
- **教师端查询一律带 `teacher_id` 过滤**，权限不靠前端隐藏菜单；
- 分页统一用 MyBatis-Plus `Page<T>`，出参用 VO 聚合前端需要的展示字段（如任务列表直接返回 `className / submissionCount / totalCount`），避免前端拿着 id 再逐个查。

## 5. 大模型接入

见 [REFERENCE-ai-integration.md](REFERENCE-ai-integration.md)。
