# AI 启蒙星球 · AI Enlightenment Planet

> 面向中小学生（6–12 岁）的 AI 启蒙学习平台 —— 本科毕业设计
> **基于 SpringBoot 和 Vue.js 的中小学生 AI 启蒙平台的设计与实现**

覆盖 **学生 / 教师 / 管理员** 三类角色，构建「**学 — 练 — 玩 — 评**」一体化闭环：
AI 科普课程 · 图形化编程（Blockly）· AI 趣味实验（大模型）· 作业与评价 · 学情数据看板。

<p align="left">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="SpringBoot" src="https://img.shields.io/badge/SpringBoot-3.2.5-6DB33F?logo=springboot&logoColor=white">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white">
  <img alt="MyBatis-Plus" src="https://img.shields.io/badge/MyBatis--Plus-3.5-red">
</p>

---

## 目录

- [项目背景](#项目背景)
- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [接口约定](#接口约定)
- [技术亮点](#技术亮点)
- [项目结构](#项目结构)
- [说明与声明](#说明与声明)

---

## 项目背景

中小学 AI 启蒙教育当前普遍存在 **优质资源分布不均**、**实践互动性不足**、**教学管理体系缺失** 三类问题，
传统的线下科普与单一视频学习模式难以同时满足「知识传递」与「动手实践」的双重需求。

本项目设计并实现一个面向中小学场景的 AI 启蒙平台，以
「课程学习 → 编程实践 → AI 互动 → 作业提交 → 反馈评价」为主线，
用一套系统把学生、教师、管理员三类角色的教与学串联起来。

---

## 功能概览

### 学生端（11 个功能页）

| 模块 | 说明 |
|---|---|
| 学习首页 | 学习概览（连续学习天数 / 累计学习时长 / 完成课程 / 作品数）、课程推荐、公告、班级通知、成就徽章 |
| 课程中心 | 按分类与难度浏览课程，卡片显示「已完成 / 学习中 X% / 未开始」 |
| 课程详情 | HTML5 视频播放，`timeupdate` 节流上报进度，看完自动判定任务完成 |
| 编程实验室 | Blockly 拖拽式积木编程，积木转 JS 后于沙箱执行，支持运行 / 保存 / 提交作品 |
| AI 魔法实验室 | AI 创意写作 + AI 知识闯关出题（接入大语言模型），历史作品可重新打开继续编辑 |
| 作业中心 | 接收教师任务、提交作业与实验成果、查看批改反馈与得分 |
| 答疑互动 | 向本班负责教师提问并查看回复 |
| 学习数据 | 完成课程、作品数、平均分、闯关次数、观看时长、近 7 天活跃、成果分布、得分趋势 |
| 个人中心 | 资料维护、班级状态、**经验值与等级**（点开可看经验明细流水）、作品集、**修改密码**、**学习提醒 / 消息通知开关** |
| 消息中心 | 站内消息，类型筛选、逐条已读、一键全部已读 |
| **新手教程** | 面向第一次使用的学生：5 步快速上手 + 功能地图 + 常见问题 |

### 教师端（7 个功能页）

工作台 · 班级管理（含入班申请审批）· 任务布置 · 进度追踪 · 作业批改 · 答疑互动 · 消息中心

### 管理端（5 个功能页）

数据看板（注册趋势 / 课程完成率 / 活跃度）· 用户管理 · 课程管理 ·
实验资源（编程模板启停 + AI 接口配置）· 公告与操作日志

---

## 技术栈

**后端**

- SpringBoot 3.2.5 · Java 21
- MyBatis-Plus（持久层）· MySQL 8.0
- JWT（认证）· Spring AOP（`@RequireRole` 方法级权限）
- Java HttpClient（调用大模型 OpenAI 兼容接口）

**前端**

- Vue 3（组合式 API）· Vite 6
- Element Plus · Vue Router · Pinia
- ECharts（学情与运营数据可视化）
- Blockly（图形化编程）
- GSAP（动效，含 `prefers-reduced-motion` 可访问性兜底）
- Phosphor Icons

**大模型集成**

- DeepSeek（OpenAI 兼容协议 `/v1/chat/completions`），密钥与模型参数由管理端动态配置

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      浏览器（学生 / 教师 / 管理）              │
│   Vue 3 + Vite 6 + Element Plus + Pinia + Vue Router         │
│   ECharts 可视化 │ Blockly 图形化编程 │ GSAP 动效             │
└───────────────────────────┬─────────────────────────────────┘
                            │  HTTP / JSON（/api/**，Vite 代理）
                            │  Authorization: Bearer <JWT>
┌───────────────────────────▼─────────────────────────────────┐
│                    SpringBoot 3.2.5 应用层                    │
│  Controller ─ Service ─ Mapper（MyBatis-Plus）                │
│  ├─ AuthInterceptor  统一登录校验（白名单放行）                │
│  ├─ @RequireRole AOP 三级角色权限校验                         │
│  ├─ GlobalExceptionHandler + 统一返回体 {code,msg,data}       │
│  └─ AiService  限流 / 内容安全 / 三级降级                      │
└───────────┬─────────────────────────────┬───────────────────┘
            │                             │
   ┌────────▼────────┐          ┌─────────▼──────────┐
   │   MySQL 8.0     │          │  DeepSeek 大模型    │
   │   20 张业务表    │          │  （可配置 / 可降级） │
   └─────────────────┘          └────────────────────┘
```

---

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+（推荐 20/22） |
| MySQL | 8.0 |

### 1. 初始化数据库

```bash
# 创建数据库并导入表结构与演示数据
mysql -u root -p < backend/sql/init.sql
mysql -u root -p ai_enlighten < backend/sql/demo-data.sql
```

> `init.sql` 建表（20 张），`demo-data.sql` 灌入演示课程、公告、闯关题与班级数据。
> 生产环境请把 `demo-data.sql` 中的演示账号全部删除或改密。

### 2. 配置并启动后端

```bash
cd backend

# 方式一（推荐）：复制配置模板并填写自己的数据库账号
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml，填入数据库用户名/密码与 JWT secret

# 方式二：直接用环境变量注入
#   DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET

mvn spring-boot:run
```

后端启动于 `http://127.0.0.1:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动于 `http://127.0.0.1:5173`，`/api` 已配置代理到 `8080`。

### 4. 登录演示

登录页仅提供**账号 + 密码**登录（原「学生端 / 教师端 / 管理端」一键切换演示入口已移除）。
演示账号（密码均为 `123456`，**仅用于本地演示**）：

| 角色 | 账号 |
|---|---|
| 管理员 | `admin` |
| 教师 | `teacher01` |
| 学生 | `student01` |

---

## 配置说明

| 配置项 | 环境变量 | 说明 |
|---|---|---|
| 数据库地址 | `DB_URL` | 默认 `jdbc:mysql://127.0.0.1:3306/ai_enlighten` |
| 数据库账号 | `DB_USERNAME` | 默认 `ai_edu` |
| 数据库密码 | `DB_PASSWORD` | **必须自行配置** |
| JWT 密钥 | `JWT_SECRET` | **必须自行配置**，长度 ≥ 32 字符 |
| 激活配置 | `SPRING_PROFILES_ACTIVE` | 默认 `local`，加载 `application-local.yml` |

**安全约定**

- 仓库内 **不包含** 任何真实数据库密码、JWT 密钥或第三方 API Key；
- `application-local.yml`、`.env`、`.baoyu-skills/` 均已加入 `.gitignore`；
- 大模型 API Key 仅由后端持有并落库，查询接口脱敏为 `****后 4 位`，不下发前端。

---

## 接口约定

- 统一前缀 `/api`，除 `/api/auth/login`、`/api/auth/register`、`/api/auth/classes` 外均需携带
  `Authorization: Bearer <token>`；
- 统一响应体：

```json
{ "code": 0, "msg": "success", "data": {} }
```

| 业务码 | 含义 |
|---|---|
| `0` | 成功 |
| `401` | 未登录 / token 失效 |
| `403` | 无权限访问该资源 |
| `1001` | 当前资源已关闭，请联系管理员 |
| `1002` | 参数校验失败 |
| `1003` | 用户名或密码错误 |
| `1101` | AI 服务未配置或不可用 |

全量接口回归脚本：仓库根目录 `test-all.mjs`（覆盖三端鉴权、CRUD、越权与异常分支）。

```bash
node test-all.mjs    # 需后端已在 8080 运行
```

---

## 技术亮点

### 1. 三级 RBAC 权限模型

`AuthInterceptor` 负责「是否登录」，自定义注解 `@RequireRole` + AOP 切面负责「是否有权访问该方法 / 该类」，
配合 `UserContext`（ThreadLocal）传递当前登录用户，实现方法级与类级双重校验。
前端请求层统一处理凭证：**401 才清除 token 并跳登录，403 仅提示**，避免越权提示误伤登录态。

### 2. 大模型能力的工程化封装

- **限流**：按 `userId` 维度的每分钟固定窗口限流器，超限抛出业务异常；
- **内容安全**：系统提示词层面注入面向未成年人的安全约束，并对返回内容做敏感词扫描，命中即替换为友好提示；
- **三级降级**：网络异常 / 超时 / 非 200 状态码统一捕获，降级返回预设文案；出题场景解析失败自动回退内置题库，
  保证第三方模型不可用时平台功能不中断、接口层不抛未处理异常；
- **密钥安全**：API Key 仅后端持有，管理端读取时脱敏。

### 3. 学习进度闭环

课程视频通过 `timeupdate` 事件节流上报进度，后端取 `max(已存进度, 本次上报)` 保证只增不减（防刷分），
进度达到 100% 时反向将该学生对应的教师课程任务判定为完成，形成「观看 → 进度 → 任务完成」的可信闭环。

### 4. 图形化编程沙箱

Blockly 积木转换为 JavaScript 后在独立 iframe 中执行，与主应用隔离；
`text_print` 映射为 `console.log` 避免阻塞式弹窗，运行 / 保存 / 提交作品全链路可用。

### 5. 前端工程质量

- 全站设计系统以 CSS 变量（Design Token）统一维护，改主题只改一处；
- 动画仅使用 `transform` / `opacity`，进度条用 `scaleX()` 规避布局重排；
- GSAP 动画统一在 `matchMedia('(prefers-reduced-motion: no-preference)')` 内执行，
  组件卸载时 `gsap.context().revert()` 清理，兼顾性能与可访问性。

### 6. 经验值与等级体系（账本制 + 幂等）

- **账本为唯一真相**：新增 `exp_log` 流水表，总经验 = `SUM(exp)`，而非按当前状态实时推导 ——
  因此删除作品、老师撤销批改都不会让经验**倒退**；
- **幂等**：`(student_id, source_key)` 唯一约束，同一件事重复触发只记一次，重复提交 / 重复上报不会刷分；
- **防刷**：单日经验上限 300，且所有加分只在后端业务动作里触发，不对外暴露「加分接口」；
- **递增曲线**：`need(n) = 100 + 50×(n-1)`，而非每级恒定值，后期升级有成长感；称号 6 档；
- **可解释**：学生能查看自己的经验明细流水（`GET /api/exp/log`），只返回本人的数据。

### 7. 账号安全

- **修改密码**须校验原密码，长度 8~32 位且同时含字母与数字；同一用户 15 分钟内失败 5 次即锁定，防在线暴力破解；
- 失败文案统一为「原密码不正确」，不区分账号是否存在，避免**账号枚举**；
- 改密成功后写入 `password_changed_at`，鉴权拦截器拒绝签发时间早于它的 token ——
  **改密即等价于下线所有旧设备**（JWT 本身无法撤销，这是无状态令牌的标准补偿手段）；
- 密码散列字段标注 `@JsonIgnore`，避免随用户信息接口下发前端。

---

## 项目结构

```
.
├── backend/                            # SpringBoot 后端
│   ├── src/main/java/com/edu/aienlighten/
│   │   ├── controller/                 # 24 个 Controller
│   │   ├── service/  service/impl/     # 业务逻辑
│   │   ├── mapper/                     # MyBatis-Plus Mapper
│   │   ├── entity/  dto/  vo/          # 数据模型
│   │   ├── security/                   # JWT / 拦截器 / @RequireRole 切面
│   │   ├── config/                     # WebConfig / MyBatis-Plus / Jackson 配置
│   │   └── common/                     # 统一返回体与全局异常处理
│   ├── src/main/resources/
│   │   ├── application.yml             # 主配置（占位符 + 环境变量）
│   │   └── application-local.yml.example
│   ├── docs/                           # 测试用例文档
│   └── sql/                            # init.sql / demo-data.sql
├── frontend/                           # Vue 3 前端
│   ├── src/
│   │   ├── api/                        # 统一请求封装
│   │   ├── components/                 # 通用组件
│   │   ├── router/                     # 三端路由
│   │   ├── styles/                     # 设计系统 Token
│   │   ├── utils/                      # GSAP 动效工具
│   │   └── views/
│   │       ├── student/                # 学生端页面
│   │       ├── teacher/                # 教师端页面
│   │       ├── admin/                  # 管理端页面
│   │       └── shared/                 # 共用布局与消息中心
│   └── public/
│       └── aora-ball/                  # 登录页情感球引擎
├── test-all.mjs                        # 全量接口回归脚本
└── PROJECT-README.md                   # 项目开发文档（架构 / 约定 / 踩坑）
```

---

## 说明与声明

- 本项目为 **温州商学院本科毕业设计**，用于学习与技术交流，作者保留著作权；
- 项目中的课程视频、插画素材等第三方资源 **不随仓库分发**，如需本地演示请自行放置到
  `frontend/public/videos/`（`*.mp4` 视频源需自行准备）；
- 登录页使用的球形角色视觉资产版权归原作者所有，**仅供学习用途，禁止商用**；
- 演示账号与演示数据仅用于本地功能验证，部署到公网前请务必删除或修改。
