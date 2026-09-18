# P1 认证与基础设施 · 测试用例与结果

> 被测系统：ai-enlighten-backend 0.1.0（SpringBoot 3.2.5 / Java 21 / MySQL 8.0）
> 测试方式：接口级功能测试（黑盒，HTTP 请求）
> 测试日期：2026-09-03 · 结果：17/17 通过

## 测试环境

| 项 | 值 |
|---|---|
| 后端 | http://127.0.0.1:8080 |
| 数据库 | ai_enlighten（MySQL 8.0.45，utf8mb4） |
| 演示账号 | admin / teacher01 / student01（密码均为 123456） |
| 认证方式 | JWT（Authorization: Bearer &lt;token&gt;） |
| 统一返回 | `{code, msg, data}`，code=0 成功 |

## 用例明细

### 一、登录功能（T1-T6）

| 编号 | 用例 | 前置 | 操作 | 预期 | 实际 | 结果 |
|---|---|---|---|---|---|---|
| T1 | 学生正常登录 | 学生账号已存在 | POST /api/auth/login {student01,123456} | code=0，role=2，返回 token | code=0, role=2, token 有效 | ✅ |
| T2 | 教师正常登录 | 教师账号已存在 | POST /api/auth/login {teacher01,123456} | code=0，role=1 | code=0, role=1 | ✅ |
| T3 | 管理员正常登录 | 管理员账号已存在 | POST /api/auth/login {admin,123456} | code=0，role=0 | code=0, role=0 | ✅ |
| T4 | 密码错误登录 | 学生账号 | POST /api/auth/login {student01,错误密码} | code=1003 用户名或密码错误 | code=1003 | ✅ |
| T5 | 不存在的用户 | 无 | POST /api/auth/login {nobody99,123456} | code=1003（不暴露用户是否存在） | code=1003 | ✅ |
| T6 | 空参数登录 | 无 | POST /api/auth/login {username:"",password:""} | code=1002 参数校验失败 | code=1002 用户名不能为空 | ✅ |

### 二、注册功能（T7-T9）

| 编号 | 用例 | 操作 | 预期 | 实际 | 结果 |
|---|---|---|---|---|---|
| T7 | 重复用户名注册 | POST /api/auth/register {student01,...} | code=1005 用户名已存在 | code=1005 | ✅ |
| T8 | 正常注册 | POST /api/auth/register {caseuser01,...} | code=0，role=2（默认学生），返回 token | code=0, role=2 | ✅ |
| T9 | 密码过短注册 | 密码 3 位 | code=1002 密码长度 6-20 位 | code=1002 | ✅ |

### 三、认证鉴权（T10-T17）

| 编号 | 用例 | 操作 | 预期 | 实际 | 结果 |
|---|---|---|---|---|---|
| T10 | 携带合法 token 访问 | GET /api/user/me + token | code=0，返回用户信息 | code=0, id=3 | ✅ |
| T11 | 无 token 访问 | GET /api/user/me（无认证头） | HTTP 401 + {code:401} | HTTP 401 | ✅ |
| T12 | 伪造 token 访问 | GET /api/user/me + Bearer abc.def.ghi | HTTP 401 | HTTP 401 | ✅ |
| T13 | 学生越权访问教师接口 | GET /api/test/teacher + 学生 token | code=403 无权限 | code=403 | ✅ |
| T14 | 教师访问教师接口 | GET /api/test/teacher + 教师 token | code=0 | code=0 | ✅ |
| T15 | 教师越权访问管理员接口 | GET /api/test/admin + 教师 token | code=403 | code=403 | ✅ |
| T16 | 管理员访问管理员接口 | GET /api/test/admin + 管理员 token | code=0 | code=0 | ✅ |
| T17 | 学生越权访问管理员接口 | GET /api/test/admin + 学生 token | code=403 | code=403 | ✅ |

## 覆盖矩阵（需求映射）

| 任务书要求 | 对应用例 |
|---|---|
| 账号登录注册 | T1-T3, T8 |
| 密码安全存储（BCrypt） | T4-T5（错误口令不可登录） |
| 输入合法性校验 | T6, T9 |
| 账号唯一性 | T7 |
| 权限管控：三类角色边界 | T13-T17 |
| 无授权访问拦截 | T10-T12 |

## 遗留说明

- 演示账号密码为 123456，仅开发期使用；上线前需修改并进行口令强度策略（后续 P5 安全增强）
- `caseuser01` 为测试注册产生，测试完成后可清理
- 密码修改 / 找回 / 验证码等功能不在 P1 范围，列入后续迭代
