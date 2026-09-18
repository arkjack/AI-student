# 从哪里继续（CONTINUE.md）

> **新会话先读这里 + `PROJECT-README.md`（项目主文档）**，即可无缝接上。
> 最后更新：2026-09-16（目录已迁移到 `毕业设计\`；通知/启停/真实数据化等本轮改动已写入）

---

## 0. 位置与启动（最重要，先看这段）

**项目根目录**：`D:\想法、创意\实践\dph\毕业设计\`
（2026-09-16 从 `D:\想法、创意\实践\dph\` 迁移进来；dph 根目录现在放的是**另一个对话**的渗透测试资料，别混）

```
D:\想法、创意\实践\dph\毕业设计\
├── backend\                      ← SpringBoot 3.2.5 / Java 21 / MyBatis-Plus / MySQL 8.0
├── frontend\                     ← Vue 3 + Vite 6 + Element Plus + Blockly + ECharts + GSAP + Phosphor
├── PROJECT-README.md             ← ★ 项目主文档（架构 / 设计系统 / 三端页面 / 踩坑）
├── CONTINUE.md                   ← 本文件
├── test-all.mjs                  ← 全量接口回归脚本（84 项）
├── 1.温州商学院本科毕业论文（设计）任务书——李浩文.docx
├── images-batch*.json            ← 插画生成批次（内部绝对路径已指向新目录）
├── .baoyu-skills\.env            ← ★ ARK_API_KEY（豆包 seedream 出图用，勿泄露）
├── .claude\  .codex\             ← 项目级 hook 配置（impeccable UI 检查）
├── vendor\gpt-image2-skill\      ← gpt-image CLI 源码
└── .tmp-aora\                    ← Emotion Ball（情感球）参考项目，含独立 .git
```

> `.baoyu-skills` / `.claude` / `.codex` / `vendor` 在 **`dph` 根目录也各留了一份原件**（迁移时是"复制"），两边都能用。

### 启动命令（新会话不会自动起服务，要手动拉起）
```powershell
# 后端 → http://127.0.0.1:8080
cd D:\想法、创意\实践\dph\毕业设计\backend ; mvn spring-boot:run

# 前端 → http://127.0.0.1:5173
cd D:\想法、创意\实践\dph\毕业设计\frontend ; npm run dev
```
- 两个都用**后台任务**跑（`run_in_background: true`）。
- 前端改 `.vue` 会 HMR 热更；**后端改 Java 必须重启**才生效。
- 浏览器看到旧样式/旧逻辑 → **`Ctrl+Shift+R` 强刷**。

### 数据库与账号
- 库 `ai_enlighten`（**19 张表**），业务账号与密码见本地 `application-local.yml`（不入库，模板 `application-local.yml.example`）。
- 演示账号（密码均 `123456`）：`admin` / `teacher01` / `student01`；另有 `student02`(10) / `student03`(12) / `testuser01` / `caseuser01`。

---

## 1. 当前状态（一句话）

三端（学生 / 教师 / 管理）+ 后端功能全部完成且已联调。本轮把 **答疑互动、站内消息通知、编程模板启停管控、学习数据 / 数据看板真实数据化** 补齐，并**清理了页面上残留的前端模拟（FALLBACK）数据**。剩余主要是**论文文档**与**真实 DeepSeek key**。

---

## 2. 本轮（2026-09 中旬）改动清单 —— 接手时优先看

### 2.1 学生端「答疑互动」（新）
- 后端 `QaController`：`POST /api/qa/ask`（学生提问；自动解析**负责教师**：class_student → class_info.teacher_id 写入 `qa_record.teacher_id`）、`GET /api/qa/my`（返回 `QaVO`，含 `teacherName`）。
- 教师端 `TeacherQaController`：`GET /api/teacher/qa/list`、`POST /api/teacher/qa/{id}/reply`。
- 前端：`views/student/StudentQa.vue`；学生顶栏新增「答疑互动」菜单 + 路由 `/student/qa`。

### 2.2 站内消息通知系统（新）
- 表 `notification`（`user_id / type / title / content / link / is_read / created_at`）。⚠️ 列名用 **`is_read`**，因为 `read` 是 MySQL 保留字。
- 接口 `/api/notify`：`GET /my`、`GET /unread-count`、`POST /read-all`、`POST /{id}/read`（角色 **{0,1,2}**）。
- 前端：
  - `components/NoticeBell.vue` —— 顶栏铃铛，未读数红点，点开下拉列表，30s 轮询；「查看全部消息」跳消息中心。
  - `views/shared/MessageCenter.vue` —— 完整消息中心（类型筛选 / 逐条已读 / 全部已读 / 点击跳转）；路由 `/student/messages`、`/teacher/messages`。
- **触发钩子**（业务动作处自动 `push`）：
  | 动作 | 位置 | 通知对象 / 类型 |
  |---|---|---|
  | 老师布置任务 | `TeacherAssignmentServiceImpl.create` | 该班每个学生 / `task` |
  | 老师批改作业 | `TeacherAssignmentServiceImpl.review` | 该学生 / `grade`（含得分） |
  | 管理员发公告 | `AdminAnnouncementServiceImpl.create` | 全体学生+教师 / `announce` |
  | 学生入班申请 | `UserServiceImpl.register` | 班主任 / `apply` |
- **管理端不显示铃铛**：`SidebarLayout` 增 `showNotice` 属性，`AdminLayout` 传 `false`。

### 2.3 编程模板「启停」管控（新，全端强制）
- 表 `blockly_template`（`name / description / level / emoji / enabled`），种子 = 学生端那 4 个模板（小猫咪动起来 / 会算数的机器人 / 小星星循环舞 / 自动巡逻车）。
- 接口：`GET /api/admin/templates`（全部）、`PUT /api/admin/templates/{id}/enabled`（启停）、`GET /api/project/templates`（**仅启用**，角色 1/2，学生与教师共用）。
- 学生端 `ProgrammingLab.vue`：停用模板下拉显示「××（已停用）」，**点击弹「当前资源已关闭请联系管理员」并回退选择**；`run/submit` 同样拦停。
- 教师端 `TeacherTasks.vue`：编程实验的「关联资源」只列**启用**模板；后端 `create` 校验停用资源 → 抛 `1001 当前资源已关闭请联系管理员`。

### 2.4 学习数据 / 数据看板「真实数据化」
- 学生端：新增 `GET /api/stats/student/my` → `StudentStatsVO`（已完成课程 / 作品数 / 作业平均分 / 闯关次数 / 累计观看分钟 / 近 7 天活跃 / 成果分布 / 得分趋势 / 等级经验 / 月度成就，全部聚合自真实业务表）。
  `views/student/Stats.vue` 已**删除 `@/mock/data` 依赖**；图表语义随之调整（「本周学习时长」→「最近 7 天学习动态（次）」，「学习时间分布」→「学习成果分布」）。
- 管理端：`AdminDashboard.vue` **删除全部 `FALLBACK_*` 模拟常量**；修 `normalizeTrend` 改读后端 `label`（原来读 `week` 导致横轴恒为 W1…W12）。

### 2.5 本轮修复的 Bug
| 现象 | 根因 | 修复 |
|---|---|---|
| 管理端登录后弹「无权限访问」并被踢回登录页 | `AdminLayout` 与教师端**共用 `SidebarLayout`**，铃铛请求 `/api/notify/**`（当时仅角色 1/2）→ 管理员(0) 403；`request.js` 又把 403 当登录失效清 token | ① notify 角色放宽 {0,1,2}；② 管理端 `showNotice=false` 不显示铃铛；③ 拦截器 **401 才清 token，403 仅提示** |
| 管理端「实验资源」编程模板名为空、难度显示 1/2 | `AdminLab.vue` 读 `title/difficulty(int)/desc`，后端返回 `name/level/description/enabled` | `normalizeTemplate` 改读正确字段；难度整数→中文兼容 |
| 操作日志里「布置任务」详情出现 `??-??` 乱码 | 早期用命令行测试时中文被编码坏掉写入 `operation_log`（**不是真实数据问题**） | 清理 `operation_log` 中 `detail LIKE '%?%'` 的脏记录 |

### 2.6 更早几轮（上一份 CONTINUE 之后累积）
AI 实验任务提交关联（作业中心 `goDo` 带 `assignmentId`）、写作记录「未批改 / 已批改 xx 分」+ 教师批改同步 `ai_writing.score`、闯关历次成绩时间改「年月日时分」、「查看全部消息」跳转、课程视频进度闭环、入班审批、任务统计、管理端用户班级下拉 + 重置密码等。

---

## 3. 数据库（19 张表）

`user` `class_info` `class_student` `class_apply` `course` `course_category` `course_progress`
`blockly_project` **`blockly_template`(新)** `ai_writing` `quiz_question` `quiz_record`
`qa_record` `assignment` `submission` `announcement` **`notification`(新)** `operation_log` `ai_config`

---

## 4. 关键约定与踩坑（省时间）

1. **Phosphor 图标**：必须 `import { PhChatCircleDots as ChatCircleDots } from '@phosphor-icons/vue'`（裸名会 build 失败）；emoji 只做头像/内容，**不当 UI 图标**。
2. **CSS token**：`main.css` 的 `:root` 里**没有** `--c-orange-deep`（旧页面引用它其实是失效的）→ 用 `--accent-deep`。改视觉先看 `styles/main.css` 与 `PROJECT-README.md` 第 4 章。
3. **Blockly**：解析 XML 用 `Blockly.utils.xml.textToDom(xml)` + `Blockly.Xml.domToWorkspace(dom, ws)`；`Blockly.utils.xml.domToWorkspace` **不存在**。`text_print` 生成 `console.log`（不要 `alert`）。
4. **MySQL 保留字** `read` → 用 `is_read`；控制台中文乱码是 **GBK 显示问题**，数据本身 UTF-8 正常（加 `--default-character-set=utf8mb4`）。
5. **用 PowerShell 测含中文的接口会乱码**：把 JSON 写成 **UTF-8 文件**（`[System.IO.File]::WriteAllText(..., UTF8Encoding($false))`），再用 `curl.exe --data-binary "@file"` 发。
6. **BCrypt hash 更新走 SQL 文件**（PowerShell 会展开 `$`）。
7. 前端验收三板斧：`npm run build` → impeccable `detect.mjs`（期望 `[]`）→ 浏览器确认路由。

---

## 5. 剩余待办（按优先级）

1. **配置真实 DeepSeek key**：管理端「实验资源」页保存 AI 配置（`PUT /api/admin/ai-config`）。
   ⚠️ 现有豆包 ARK key 只开通了 `seedream` 图像模型，chat 文本模型 404，**不能用于文本 AI**；AI 三接口当前返回 `1101`。
2. **论文文档**：需求分析 / 系统设计 / 测试报告（可参考 `backend/docs/test-cases-P1.md`）。
3. **可选优化**：前端 echarts/blockly 分包；清理无用 `src/mock/data.js`；JWT secret 改环境变量；管理端「新增模板」目前是前端本地新增（未落库，刷新即消失），如需要可补 `POST /api/admin/templates`。
4. ~~接口测试~~ 已完成：`test-all.mjs` 84 项全通过（本轮新接口未纳入该脚本，可补充）。

---

## 6. 验证账号与常用入口速查

- 学生：`student01` → 首页 / 课程中心 / 编程实验室 / AI 魔法实验室 / 作业中心 / **答疑互动** / 学习数据 / 消息中心
- 教师：`teacher01` → 工作台 / 班级管理 / 任务布置 / 进度追踪 / 作业批改 / 答疑互动 / 消息中心
- 管理：`admin` → 数据看板 / 用户管理 / 课程管理 / **实验资源（编程模板启停）** / 公告与日志
