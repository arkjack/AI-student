---
name: k12-ai-edu-platform
description: 从零构建 K12 中小学 AI 启蒙教育平台（SpringBoot 3 + Vue 3 三端全栈），覆盖需求分析、19 张表数据模型、JWT+AOP 三级 RBAC 后端骨架、三端前端与设计系统、大模型接入（限流/内容安全/三级降级）、教学业务闭环与接口回归验收。Use when 用户要开发中小学/青少年教育平台、AI 启蒙或科普学习平台、课程+作业+班级管理类系统，或提到「三端（学生/教师/管理员）教学平台」「SpringBoot+Vue 教育类毕设/课设」「图形化编程 Blockly」「AI 实验 / 智能答疑 / 作业批改」「学情看板」，或要求参考 AI-student 做一个类似的网站。
---

# K12 AI 启蒙教育平台 · 构建流程

面向中小学生（6–12 岁）的 AI 启蒙平台：**三端**（学生 / 教师 / 管理员）× **三模块**（AI 科普课程 / 图形化编程 / 大模型实验），构建「学 — 练 — 玩 — 评」闭环。

本 skill 提供实战验证过的分阶段流程、可直接复用的骨架代码、数据模型、设计系统与踩坑清单。

## Quick start

```powershell
# 1. 生成可运行骨架（后端 + 前端 + 建表脚本）
powershell -ExecutionPolicy Bypass -File scripts/init-project.ps1 `
  -TargetDir D:\my-platform -Package com.example.edu -AppName EduApplication

# 2. 建库（密码自行替换，架构见 backend/sql/init.sql）
mysql -u root -p < D:\my-platform\backend\sql\init.sql
mysql -u root -p ai_enlighten < D:\my-platform\backend\sql\demo-data.sql

# 3. 起服务（各自用后台任务，勿前台阻塞）
cd D:\my-platform\backend  ; mvn spring-boot:run          # → 8080
cd D:\my-platform\frontend ; npm install ; npm run dev     # → 5173

# 4. 验收（构建 + 密钥扫描 + mock 残留 + 接口回归）
powershell -File scripts/verify.ps1 -ProjectDir D:\my-platform
```

## 适用 / 不适用

**适用**：三端教育或教学管理平台、AI 启蒙/科普学习平台、课程+作业+班级管理系统、需要「角色权限 + 大模型接入」的教学类毕设/课设。

**不适用**：单端官网与落地页、纯内容 CMS、无角色体系的小工具、非教学场景的电商/社交。

## 8 个阶段

| # | 阶段 | 关键产出 | 参考 |
|---|---|---|---|
| 1 | 需求与角色建模 | 三端功能矩阵、权限矩阵 | [REFERENCE-architecture.md](REFERENCE-architecture.md) |
| 2 | 数据库设计 | ≥15 张表 DDL、E-R 关系 | [REFERENCE-architecture.md](REFERENCE-architecture.md) |
| 3 | 后端地基 | 统一返回体、全局异常、JWT、拦截器、`@RequireRole` AOP | [REFERENCE-backend.md](REFERENCE-backend.md) |
| 4 | 后端业务 | 三端 REST 接口（Controller/Service/Mapper 分层） | [REFERENCE-backend.md](REFERENCE-backend.md) |
| 5 | 前端地基 | 请求封装、三端路由、设计系统 Token、布局件 | [REFERENCE-frontend.md](REFERENCE-frontend.md) |
| 6 | 前端页面 | 三端页面 + 复用组件 | [REFERENCE-frontend.md](REFERENCE-frontend.md) |
| 7 | 大模型接入 | 限流 + 内容安全 + 三级降级 + 密钥脱敏 | [REFERENCE-ai-integration.md](REFERENCE-ai-integration.md) |
| 8 | 联调与验收 | mock 清零、接口回归、构建通过 | [scripts/verify.ps1](scripts/verify.ps1) |

**地基不可跳过**：3 没做完就写 4、5 没做完就写 6，后面每加一个接口都要返工一次。

## 不可妥协的 6 条约定

1. **数据库先行** —— 表结构没定完，不写任何接口。改表比改接口贵十倍。
2. **统一返回体从第一个接口就用** —— `{code, msg, data}`，`code=0` 成功。后期再改会牵动前端每一处调用。
3. **权限分两层** —— `AuthInterceptor` 只管「有没有登录」，`@RequireRole` 管「这个角色能不能调」。两件事别塞进一处。
4. **前端所有请求走 `@/api/request`** —— 页面里禁止裸写 `axios`，否则 token 注入与 401/403 处理会各写一套。
5. **密钥一律环境变量 / gitignore 的本地配置** —— 数据库密码、JWT secret、大模型 API Key 禁止进仓库。
6. **交付前 mock 必须清零** —— `verify.ps1` 扫 `FALLBACK_`/`mock` 残留。留着 mock 会让「看起来对」掩盖真实的字段错误。

## 各阶段要点

**阶段 1–2**：先定三端功能矩阵（学生 10 页 / 教师 7 页 / 管理 5 页是经过验证的合理规模），再定表。表的划分原则是「一个业务动作一张主表 + 必要的关联表」，别提前做过度范式化。

**阶段 3**：直接复用 `scripts/templates/` 里的骨架文件，它们已包含几处容易写错的处理——token 失效判定要同时校验「账号未禁用」与「密码未在签发后被改」；AOP 切面必须把方法级 `@annotation` 与类级 `@within` 分开定义切点。

**阶段 4**：Controller 只做参数校验与调用，业务逻辑放 Service。分页统一用 MyBatis-Plus 的 `Page`。每个写操作都要想清楚事务边界。

**阶段 5**：设计系统先落 `main.css` 的 `:root` Token，页面里禁止写死颜色与圆角。三端共用一套 Token，但布局件分开（学生端顶部导航、教师/管理端侧栏）。

**阶段 6**：高交互页面（图形化编程、AI 实验）单独排期，它们比普通 CRUD 页面耗时 3–5 倍。

**阶段 7**：大模型能力必须当成**不可靠的外部依赖**来设计——限流、内容安全、降级一个都不能少。

**阶段 8**：写接口回归脚本并跑通；把前端 mock 全部替换为真实接口，过程中会暴露一批前后端字段类型不一致的问题（详见 PITFALLS）。

## 验收标准（缺一不可）

- [ ] `mvn -q compile` 通过，后端能启动并连上数据库
- [ ] `npm run build` 通过
- [ ] `verify.ps1` 全绿（密钥扫描 / mock 残留 / 越权测试 / 构建）
- [ ] 三端均可登录，且**跨角色越权被拒**（拿学生 token 调管理端接口应返回 403）
- [ ] 大模型不可用时 AI 功能降级为友好提示，而非抛异常或白屏

## 文件导航

| 文件 | 内容 |
|---|---|
| [REFERENCE-architecture.md](REFERENCE-architecture.md) | 三端功能矩阵、19 张表设计、业务闭环设计 |
| [REFERENCE-backend.md](REFERENCE-backend.md) | 后端骨架代码、错误码表、分层约定 |
| [REFERENCE-frontend.md](REFERENCE-frontend.md) | 请求封装、路由守卫、设计系统 Token、组件约定 |
| [REFERENCE-ai-integration.md](REFERENCE-ai-integration.md) | 大模型接入：限流、内容安全、三级降级、密钥脱敏 |
| [PITFALLS.md](PITFALLS.md) | 实战踩坑清单（省下大量调试时间） |
| [scripts/init-project.ps1](scripts/init-project.ps1) | 一键生成可运行骨架 |
| [scripts/verify.ps1](scripts/verify.ps1) | 验收检查（密钥/越权/mock/构建） |

## 出处

流程与代码骨架提炼自开源毕业设计项目 **[AI-student](https://github.com/arkjack/AI-student)**（基于 SpringBoot 和 Vue.js 的中小学生 AI 启蒙平台的设计与实现）。骨架代码均为该项目实际运行验证过的版本，非示例伪码。
