# AI 启蒙星球 · 项目阅读文档

> 基于 SpringBoot + Vue.js 的中小学生 AI 启蒙平台（毕业设计）
> 本文档用于快速了解项目现状，任何修改前请先读这里。
> 最后更新：2026-09-16（目录迁入 `dph\毕业设计\`；答疑互动 / 站内消息 / 编程模板启停 / 数据真实化已完成）

---

## 1. 项目一句话

面向中小学生（6-12 岁）的 AI 启蒙学习平台，覆盖 **管理员 / 教师 / 学生** 三类角色，
实现「学—练—玩—评」闭环：AI 科普课程、图形化编程（Blockly）、AI 趣味实验（DeepSeek）、作业与评价。

**技术栈**（任务书定死）：SpringBoot 3.2.5 + Java 21 + MyBatis-Plus + MySQL 8.0（后端已完成）
前端：Vue 3 + Vite 6 + Element Plus + Pinia（预留）+ Vue Router + GSAP + ECharts + Phosphor Icons + Blockly + Emotion Ball

---

## 2. 目录结构

```
D:\想法、创意、实践\dph\毕业设计\      ← ★ 项目根目录（2026-09-16 从 dph\ 迁入）
├── 1.温州商学院...任务书——李浩文.docx    ← 原始任务书
├── PROJECT-README.md                     ← 本文档（项目主文档）
├── CONTINUE.md                           ← ★ 断点快照（新会话先读这个）
├── test-all.mjs                          ← 全量接口回归脚本（84 项）
├── backend\                              ← ★ 后端工程（SpringBoot 3.2.5 / Java 21）
│   └── src\main\java\com\edu\aienlighten\
│       ├── controller\  service\  mapper\  entity\  dto\  vo\  security\  common\  config\
│       └── resources\application.yml     ← 数据源配置（账号密码走环境变量 / application-local.yml）
├── frontend\                             ← ★ 前端工程（已完整）
│   ├── public\
│   │   ├── aora-ball\js\                 ← Emotion Ball 情感球引擎（rings/emotions/ball/engine.js）
│   │   └── videos\                       ← 课程视频（what-is-ai.mp4，抖音下载的科普视频）
│   ├── src\
│   │   ├── main.js                       ← 入口（Element Plus 全量 + 图标全局注册，纯冗余可删）
│   │   ├── api\request.js                ← ★ 统一请求封装（JWT 注入 / 401 清 token / 403 仅提示）
│   │   ├── styles\main.css               ← ★ 设计系统 v4（全站 token，改样式先看这里）
│   │   ├── router\index.js               ← 三端路由（/student /teacher /admin）
│   │   ├── utils\motion.js               ← GSAP 统一工具（插件注册/countUp/reduced-motion）
│   │   ├── components\
│   │   │   ├── CourseCard.vue            ← 课程卡片（封面按 course-{id}.jpg 匹配）
│   │   │   └── NoticeBell.vue            ← ★ 站内消息铃铛（未读红点 + 下拉 + 轮询）
│   │   ├── mock\data.js                  ← 学生端遗留 mock（多数页面已改为真实 API）
│   │   └── views\
│   │       ├── Login.vue                 ← 登录页（情感球 + 三端切换按钮）
│   │       ├── student\                  ← 学生端 10 页（顶部导航）
│   │       ├── teacher\                  ← 教师端 6 页（侧栏布局）
│   │       ├── admin\                    ← 管理端 5 页（侧栏布局）
│   │       └── shared\                   ← SidebarLayout(showNotice 控制铃铛) / MessageCenter / ComingSoon
│   │   └── assets\images\                ← AI 插画（徽章/封面/登录背景/Home 背景/吉祥物）
├── vendor\gpt-image2-skill\              ← gpt-image CLI 源码仓库（保留供更新）
├── images-batch*.json                    ← 插画生成批处理配置（内部绝对路径已指向本目录）
├── .baoyu-skills\.env                    ← ★ ARK_API_KEY（豆包，勿泄露到代码）
├── .claude\  .codex\                     ← 项目级 hook 配置（impeccable UI 检查）
└── .tmp-aora\                            ← Emotion Ball 参考项目（含独立 .git）
```

> 说明：`.baoyu-skills` / `.claude` / `.codex` / `vendor` 在 `dph` 根目录**也各留一份原件**（迁移时用的是"复制"）。

## 3. 三端页面与路由

| 角色 | 入口 | 页面 |
|---|---|---|
| 学生 | `/student/home` | 首页 · 课程中心 · 课程详情 · 编程实验室(Blockly) · AI 魔法实验室(写作/闯关) · 作业中心 · **答疑互动** · 学习数据 · 个人中心 · **消息中心** |
| 教师 | `/teacher/workbench` | 工作台 · 班级管理 · 任务布置 · 进度追踪 · 作业批改 · 答疑互动 · **消息中心** |
| 管理 | `/admin/dashboard` | 数据看板 · 用户管理 · 课程管理 · 实验资源(编程模板启停 / AI 配置) · 公告与日志 |

登录页（`/login`）底部有「学生端 / 教师端 / 管理端」三个一键切换按钮。

## 4. 设计系统 v4（改视觉前必读）

**方向**：深海军蓝 + 暖橙单 accent、细边框卡片、渐变清零、去 AI 化、活泼但高级（对标 Duolingo/Khan Kids）。

| Token | 值 | 用途 |
|---|---|---|
| `--brand` | `#1e4fd8` | 主色（按钮/激活态/进度条） |
| `--brand-deep` | `#163a9e` | hover 深蓝 |
| `--navy` | `#15285c` | 深色面（Hero/侧栏/登录品牌区） |
| `--accent` | `#ff8a3d` | 唯一暖橙（CTA/闯关/标签） |
| `--ink / --ink-2 / --ink-3` | 深→灰蓝 | 文字三层 |
| `--line` | `#e6eaf3` | 细边框/分隔线 |
| `--bg` | `#f8fafd` | 页面背景（近白） |

- 形状：卡片 16px / 控件 12px / 小元素 pill（`--radius-lg/md/sm`）
- 卡片：**1px 边框 + 极轻阴影**（`.k-card`），hover 才抬升
- 按钮：`.k-btn`（主蓝，纯色）/ `k-btn--ghost`（白底蓝）/ `k-btn--orange`（暖橙）；按压 `scale(0.97)` 反馈
- 标签：`.k-tag` + `k-tag--orange/green/teal/...`
- 标题字体：**站酷快乐体**（`@fontsource/zcool-kuaile`，`--font-title`）；正文系统字体
- 标题结构：`<h1 class="k-h1-icon"><Xxx :size="24" weight="bold" /> 标题</h1>` + `<p class="sub">` 包在 `.page-head` 里

## 5. 关键实现约定（踩过的坑，勿重复踩）

1. **Phosphor 图标导入必须 Ph 前缀别名**：
   `import { PhHouse as House } from '@phosphor-icons/vue'`（包的导出是 `PhXxx`，裸名会 build 失败）。
   禁用 `el-icon`；emoji 只允许出现于头像/内容/徽章，**不能当 UI 图标**。
2. **进度条动画用 `transform: scaleX()`**，禁止 `width` 动画（布局重排，检测器会报警）。
3. **GSAP**：插件注册只在 `src/utils/motion.js` 一次；组件内 `gsap.context(scope)` + `onUnmounted → ctx.revert()`；
   所有动画包在 `matchMedia('(prefers-reduced-motion: no-preference)')` 里（可访问性）。
4. **Emotion Ball**（登录页）：引擎在 `public/aora-ball/js/`，4 个文件按 rings→emotions→ball→engine 顺序加载；
   `EmotionBall.create(el, { emotion:'02', idle:true, eyeScale:1.35 })`；`startTour` 巡演、`setGaze` 全页注视、`spin/burst` 点击彩带；
   `onUnmounted → destroy()`。**授权**：球形角色视觉仅供学习，禁止商用（毕设学习用途 OK）。
5. **Blockly**（编程实验室）：积木转 JS 在沙箱 iframe 执行，模板 XML 在组件内 `TEMPLATE_XML`。
6. **mock 数据**：学生端在 `src/mock/data.js`；教师/管理端各页面组件内联。接入后端时逐一替换为 API。
7. **图片素材**：`src/assets/images/` 全部由豆包 seedream 生成（同族 3D 蓝白风格）。
   重新生成用 `baoyu-imagine` skill（批量：`npx -y bun …/main.ts --batchfile xx.json`），模型 `doubao-seedream-5-0-pro-260628`（key 在 `.baoyu-skills/.env`）。
8. **gpt-image CLI**（可选出图路径）：`OPENAI_API_KEY=<ARK key>` + `OPENAI_BASE_URL=https://ark.cn-beijing.volces.com/api/v3` + `--model doubao-seedream-5-0-pro-260628`（skilled 已实测通）。
9. **服务启动**（frontend 目录）：`npm run dev`（端口 5173）。看到旧页面时用 **Ctrl+Shift+R 强刷**；换浏览器/端口不符也会导致"没变化"错觉。
10. **验收三板斧**（每次改动后）：`npm run build` → `node C:\Users\arkjack\.agents\skills\impeccable\scripts\detect.mjs --json src`（期望 `[]`）→ 浏览器打开对应路由确认。
11. **CSS token 陷阱**：`main.css` 的 `:root` 里**并没有** `--c-orange-deep`（旧页面写了它其实是失效的）→ 统一用 `--accent-deep`。
12. **MySQL 保留字**：`read` 是保留字，字段命名用 `is_read`（`notification` 表即如此）。
13. **测中文接口**：PowerShell 直接发中文 body 会乱码 → 先把 JSON 写成 UTF-8 文件（`UTF8Encoding($false)`），再 `curl.exe --data-binary "@file"`。
14. **中文路径注意**：项目路径含中文，个别时候命令行/工具解析中文路径会"假性找不到"；可用「列出目录 → 按对象 `FullName` 操作」的方式规避。

## 6. 动效清单

- 首页：Hero 内容时间线进场、数据条数字滚动（count-up）、课程卡片滚动分批进场（paused timeline + ScrollTrigger.once）、成就徽章弹入
- 闯关：选项判对/错图标弹性弹入 + 分数跳动 + 反馈区滑入
- 学习数据/看板：指标数字滚动、ECharts 自身动画
- 全局：按钮按压、卡片 hover 抬升、`prefers-reduced-motion` 全兜底

## 7. 当前状态与路线图

- [x] 三端前端全部功能模块（mock 数据）
- [x] 设计系统 / 插画素材 / 动效 / 图标
- [x] **后端 P1 完成**：SpringBoot 3.2.5 + Java 21 工程、17 张表入库、JWT 登录注册、统一返回/异常、拦截器强制登录 + @RequireRole AOP、CORS
- [x] **后端 P2 完成**：学生端 API（课程/进度/编程作品/写作/闯关/答疑/作业/公告，52 个文件）
- [x] **后端 P3+P4 完成**：教师端（班级/任务/进度/批改/答疑）+ 管理端（用户/课程/资源/AI 配置/公告/日志/看板统计）
- [x] **后端 P5 完成**：DeepSeek 接入（HttpClient 调用、按 userId 限流、内容安全过滤、降级文案、内置题库回退）
- [x] **前端对接完成**：三端页面 mock → 真实 API（`src/api/request.js` 统一封装 + vite proxy `/api`→8080；登录/学生/教师/管理端均已端到端验证）
- [x] **去模拟数据**：学生「学习数据」、管理「数据看板」「实验资源·编程模板」已彻底移除前端 `FALLBACK_*` 模拟数据，全部走真实接口
- [x] 演示数据已入库（6 课程/4 公告/5 闯关题/班级与学生关联）
- [ ] DeepSeek key 配置（管理端"实验资源"页面 PUT ai-config 后 AI 实验室真实生效）
- [ ] 文档：需求分析 / 系统设计 / 测试报告
- [ ] 测试：功能/兼容/性能

### 2026-09 功能完善（联调阶段新增，均已完成）

1. **课程视频 + 进度闭环**：抖音科普视频下载为 `public/videos/what-is-ai.mp4`，课程详情用 HTML5 `<video>` 真实播放；`timeupdate` 节流上报进度、`ended` 上报 100%；课程详情/列表接口回填当前学生 `progress/completed`（刷新不归零）。**观看进度 ≥100% → 教师端对应课程任务自动判定完成**。
2. **入班审批流程**：新增 `class_apply` 表（第 17 张）。学生注册选班 → 生成待审批申请 → 教师端「班级管理」审批（同意/拒绝）→ 同意后写入 `class_student` 正式入班。注册页班级为下拉选择（公开接口 `/api/auth/classes`）。
3. **教师端任务统计**：新增 `AssignmentVO`，任务列表直接返回 `className/resourceName/submissionCount/totalCount`；课程任务完成数按 `course_progress` 统计、其余按 `submission` 统计，列表进入即显示完成人数。
4. **作业中心状态监察**：课程类任务按观看进度判定「已完成」，新增「已完成」筛选与统计；闯关类（AI 实验）任务完成答题后自动生成提交记录。
5. **闯关历次成绩**：新增 `GET /api/quiz/my`，闯关完成页展示历次答题成绩；`/quiz/record` 保存后自动关联学生所在班级的 type=3 任务生成 submission。
6. **管理端用户管理**：新增/编辑账号的班级改为下拉选择（`/api/auth/classes`）；编辑支持重置密码（留空不改）；`UserVO` 新增 `classId/className` 回填。
7. **个人中心班级状态**：新增 `GET /api/homework/my-class`，显示「已入班 / 待审批 / 未加入班级」。
8. **课程中心**：卡片状态显示「已完成 / 学习中 X% / 未开始」；视频时长按真实时长精确到分钟（课程 1 实测 173 秒 → 3 分钟）。
9. **学生端公告**：首页「更多」弹出完整公告列表弹窗。
10. **修复一批前后端字段类型不一致**：`isTop/status/difficulty` 由字符串/布尔改为数字、`resourceId/deadline` 空值处理、`classStudentId` 取 `s.id`、`PhBellRing` 更正为 `PhBellRinging` 等。

### 2026-09 中旬 功能完善（第二轮，均已完成）

1. **答疑互动闭环**：学生端新增「答疑互动」页（提问自动派发给负责教师，`class_student → class_info.teacher_id`）；教师端答疑面板可查看并回复。接口 `/api/qa/ask|my`、`/api/teacher/qa/list`、`POST /api/teacher/qa/{id}/reply`。
2. **站内消息通知系统**：新增 `notification` 表 + `/api/notify/my|unread-count|read-all|{id}/read`；前端 `NoticeBell.vue`（铃铛，未读红点、30s 轮询）+ `MessageCenter.vue` 消息中心（路由 `/student/messages`、`/teacher/messages`）。触发点：布置任务→本班学生、批改→该学生、发公告→全体师生、入班申请→班主任。**管理端不显示铃铛**（`SidebarLayout` 的 `showNotice=false`）。
3. **编程模板启停管控**：新增 `blockly_template` 表 + `GET /api/admin/templates`、`PUT /api/admin/templates/{id}/enabled`、`GET /api/project/templates`（仅启用）。学生端停用模板显示「（已停用）」，点击弹「当前资源已关闭请联系管理员」并禁止运行/提交；教师端编程任务只能关联启用模板，后端对停用资源发布直接拒绝（1001）。
4. **数据真实化**：新增 `GET /api/stats/student/my`（学生学习数据全部聚合自真实业务表）；`Stats.vue` / `AdminDashboard.vue` 移除前端 `FALLBACK_*` 模拟数据；修复看板注册趋势横轴（改读后端 `label`）。
5. **修复**：管理端与教师端共用 `SidebarLayout`，导致管理员请求 `/api/notify/**` 得 403 又被拦截器清 token 踢回登录页 → notify 角色放宽为 `{0,1,2}` + 管理端隐藏铃铛 + 拦截器改为「**401 才清 token，403 仅提示**」；管理端「实验资源」模板字段映射错位（`title/difficulty(int)` → `name/level/description/enabled`）；清理 `operation_log` 中早期命令行测试写入的乱码详情。

### 2026-09-19 部署上线与体系补全（第三轮，均已完成）

1. **部署上线**：后端以 systemd 服务（Java 21 / Dragonwell）运行，前端构建产物交由 nginx 提供并复用站点原有 vhost 反代 `/api`，域名 + HTTPS 沿用原有证书。运维细节（服务器地址 / 部署路径 / 重部署与回滚 / 踩坑）见本地 `DEPLOY.md`（**已加入 .gitignore，不随仓库公开**）。
2. **修掉 9 个缺陷**（详见 `CONTINUE.md` 第 2.2 节）：CORS 白名单缺生产域名导致公网登录一律 403、`init.sql` 缺 3 张表且 BCrypt 种子 hash 错误、`demo-data.sql` 漏插班级、课程 id 漂移导致封面整批消失、**路由守卫不校验登录态**导致未登录时弹一摞「请先登录」、本机构建产物永不清空、`course_category` id 漂移导致分类名全空、**老师批改编程任务的分数从不回写**。
3. **经验值与等级体系（完整重做）**：新增 `exp_log` 流水表 —— 账本为唯一真相（总经验 = `SUM(exp)`）、`(student_id, source_key)` 唯一保证幂等、单日上限 300；等级曲线 `need(n) = 100 + 50×(n-1)`，称号 6 档；10 类经验来源接入 6 个业务触发点 + 每日首次学习 + 连续学习里程碑；历史数据一次性回填。新增 `ExpService` / `ExpLevels` / `ActivityService`（后者从统计服务抽出以破除循环依赖）。
4. **修改密码**：`POST /api/user/change-password`（路径刻意避开公开白名单 `/api/auth/**`），校验原密码 + 强度（8~32 位含字母数字）+ 15 分钟内失败 5 次锁定；成功后写 `password_changed_at`，拦截器据此**作废改密前签发的所有 token**。顺带修复 `User.password` 缺 `@JsonIgnore` 导致 `/api/user/me` 把 BCrypt 散列下发给前端。
5. **用户偏好真正生效**：`user.notify_enabled` 为**总开关**（关闭后后端不再为该用户生成任何站内消息），`user.remind_enabled` 为**子开关**（`StudyRemindTask` 每天 20:00 扫描，连续 ≥2 天未学习则推送提醒，cron 可用 `app.remind.cron` 覆盖）。
6. **新手教程页** `/student/guide`：5 步快速上手 + 8 个功能模块说明 + 7 条 FAQ（内容与实际实现严格对齐）。
7. **学习数据全面真实化**：首页数据条 / 班级通知 / 成就徽章 / 个人中心等级经验 / 作品集评分状态全部改接真实接口，删除写死的假数据（此前**所有学生看到的数字完全一样**）；新增 `frontend/src/utils/achievements.js` 统一徽章判定。
8. **作品评分语义修正**：`blockly_project` 新增 `assignment_id` 区分「教师布置的编程任务作品」与「学生自由创作」；老师批改 `type=2` 任务时把分数回写到作品；前端四分支显示（已批改 `XX 分` / 任务未批改 `待批改` / 自由创作 `已提交` / `草稿`）。注意判断必须显式判空 —— **0 分是合法分数**。
9. **写作记录可重开**：`AiLab.vue` 的记录列表补上点击载入（原本无点击事件，且 `loadWritingRecords` 丢掉了后端返回的 `content`）；含滚动定位、编辑中高亮、以及「已批改记录另存为新草稿」的保护文案。
10. **构建产物清理**：新增 `frontend/scripts/clean-dist.mjs` 挂在 `prebuild` —— 本机环境下 Vite 自带的 `emptyOutDir` **静默失效**（`fs.rmSync(dir, {recursive:true})` 不抛异常也不删任何文件），曾累积到 1300 个文件 / 23 份 `Login-*.js`，并导致已下线代码仍可通过旧哈希访问。
11. **全局异常处理补 404/405**：路由写错时原本被兜底成 `code:9999 系统繁忙`，排查时极易误判成服务故障。

### 后端新增接口速查（本次）

| 接口 | 说明 |
|---|---|
| `GET /api/auth/classes` | 公开班级列表（注册/管理端选班用） |
| `GET /api/teacher/classes/applies` | 教师端入班申请列表 |
| `POST /api/teacher/classes/applies/{id}/approve` | 同意入班申请 |
| `POST /api/teacher/classes/applies/{id}/reject` | 拒绝入班申请 |
| `GET /api/homework/my-class` | 学生当前班级状态 |
| `GET /api/quiz/my` | 我的历次闯关成绩 |
| `POST /api/qa/ask` / `GET /api/qa/my` | 学生提问 / 我的问答历史（含 `teacherName`） |
| `GET /api/teacher/qa/list` / `POST /api/teacher/qa/{id}/reply` | 教师查看学生提问 / 回复 |
| `GET /api/notify/my` / `unread-count` / `POST read-all` / `{id}/read` | 站内消息（角色 0/1/2） |
| `GET /api/admin/templates` / `PUT /api/admin/templates/{id}/enabled` | 编程模板列表 / 启停 |
| `GET /api/project/templates` | 启用的编程模板（学生/教师共用） |
| `GET /api/stats/student/my` | 学生学习数据聚合（真实业务表），含 `streakDays` 与等级字段 |
| `POST /api/user/change-password` | 修改密码（校验原密码 + 强度 + 失败锁定 + 作废旧 token） |
| `GET /api/user/preferences` / `PUT` | 读取 / 更新偏好设置（学习提醒、消息通知） |
| `GET /api/exp/log?limit=20` | 我的经验明细（只返回当前登录用户自己的流水） |

### 后端快速指引（backend 目录）

- 启动：`mvn spring-boot:run`（端口 8080；数据源已配 ai_edu 账号，见 application.yml）
- 数据库：库 `ai_enlighten`（**20 张表**，初始化脚本 `sql/init.sql` + `class_apply` 入班申请表 + `blockly_template` 编程模板表 + `notification` 站内消息表 + `exp_log` 经验流水表），业务账号与密码见本地 `application-local.yml`（该文件不入库，模板见 `application-local.yml.example`）
- 演示账号（密码均 123456）：`admin` / `teacher01` / `student01`
- 接口约定：`/api/auth/login|register` 公开；其余 `/api/**` 需 `Authorization: Bearer <token>`；统一返回 `{code,msg,data}`（code 0 成功；401 未登录/403 无权限/1003 用户名密码错误）
- 已踩坑备忘：① JDBC `characterEncoding=UTF-8`（不能写 utf8mb4）② BCrypt hash 更新务必用 SQL 文件执行（PowerShell 会展开 `$`）③ 拦截器强制登录后，注册/登录白名单在 `WebConfig.excludePathPatterns`

## 8. 环境备忘

- Node v24 / npm 11；
- **git 网络**：本机 `http.proxy` / `https.proxy` 曾被设为 `127.0.0.1:9674`，但**该端口已无服务监听**
  （Clash 未运行或端口变更）。而**直连 GitHub 是通的**，所以这个残留配置会把本来能通的请求堵死。
  遇到 `Failed to connect to github.com ... via 127.0.0.1` 时，用下面任一方式解决：
  ```bash
  git -c http.proxy= -c https.proxy= push origin main      # 本次绕过
  git config --global --unset http.proxy                   # 永久清掉
  git config --global --unset https.proxy
  ```
- Chrome：已开启 remote-debugging（CDP proxy 常驻 3456 端口，用于逐页验收）
- 豆包 key：存于 `.baoyu-skills/.env` 的 `ARK_API_KEY`（**该目录已加入 .gitignore，严禁提交**）
