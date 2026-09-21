# 从哪里继续（CONTINUE.md）

> **新会话先读这里 + `PROJECT-README.md`（项目主文档）**，即可无缝接上。
> 最后更新：2026-09-21

---

## 0. 位置与启动

**项目根目录**：`D:\想法、创意\实践\dph\毕业设计\`

```
毕业设计\
├── backend\                      ← SpringBoot 3.2.5 / Java 21 / MyBatis-Plus / MySQL 8.0
├── frontend\                     ← Vue 3 + Vite 6 + Element Plus + Blockly + ECharts + GSAP + Phosphor
├── DEPLOY.md                     ← ★ 线上运维文档（服务器 IP / 面板 / 部署路径 / 踩坑）
│                                    ⚠️ 已加入 .gitignore，**不会公开**
├── PROJECT-README.md             ← 项目主文档（架构 / 设计系统 / 三端页面 / 踩坑）
├── CONTINUE.md                   ← 本文件
├── test-all.mjs                  ← 全量接口回归脚本（84 项）
└── 1.温州商学院本科毕业论文（设计）任务书——李浩文.docx   （*.docx 已 gitignore）
```

### 本地启动（新会话不会自动起服务，要手动拉起）

```powershell
# 后端 → http://127.0.0.1:8080
cd backend ; mvn spring-boot:run

# 前端 → http://127.0.0.1:5173
cd frontend ; npm run dev
```

- 两个都用**后台任务**跑（`run_in_background: true`）。
- 前端改 `.vue` 会 HMR 热更；**后端改 Java 必须重启**才生效。
- 浏览器看到旧样式/旧逻辑 → **`Ctrl+Shift+R` 强刷**。

### 线上环境

**已部署到阿里云 ECS（宝塔面板环境），公网可访问。**
服务器地址、面板地址、部署目录、运维命令、重部署与回滚流程、踩坑记录，
全部在本地 **`DEPLOY.md`** —— 该文件**已加入 `.gitignore`，不会随仓库公开**
（内含服务器 IP 与已知安全弱点清单）。

---

## 1. 当前状态（一句话）

三端（学生 / 教师 / 管理）+ 后端功能全部完成，**已部署上线并通过公网验证**；
本轮补齐了任务书新增要求的**全链路内容安全体系**（敏感词库 / 三重前置过滤 / 交互日志 /
教师人工复核 / 运行模式三态），并顺带修掉了「管理端保存配置必然 400」与
「DeepSeek 模型名已下线」两个会让 AI 整体不可用的问题。本地与线上均为最新版本。
剩余主要是**论文文档**与几处演示数据打磨。

---

## 2. 本轮（2026-09-21）内容安全体系（第四轮）

> 触发点：任务书更新，新增「AI 生成内容安全审核机制」与「内容安全与合规性」两条要求。
> 完整设计说明见 `README.md` 技术亮点 §8。

### 2.1 建成的东西

| 层 | 实现 | 说明 |
|---|---|---|
| 输入侧 | `ContentSafetyService.checkInput` | 违规指令在**调用大模型之前**拦下（`1201`）并落日志。刻意排在密钥校验**之前** —— 不依赖 API Key，安全属性无条件成立 |
| 输出侧 | `checkOutput` 三重过滤 | ① 敏感词（硬规则，只对 `level=3` 生效）② 未成年人适宜性（分类权重加权打分，≥80 拦 / 30~79 转复核）③ 准确性（结构完整性 + 数值型常识比对） |
| 词库 | `sensitive_word` 表 + 管理端页面 | 45 条演示词，「涉政」只建分类不预置词条；支持增删改 / 启停 / 批量导入 |
| 日志 | `ai_interaction_log` 表 | 每次调用都写（通过/替换/拦截/降级），`hit_stage` 标层级、`review_status` 标是否需复核 |
| 复核 | 教师端「内容复核」页 | 按所带班级限定范围；管理员可兜底复核 |
| 策略 | `ai_scene_policy` 表 | 三个实验各自的五道开关，管理端真实读写 |
| 运行模式 | `ai_config.content_filter` 三态 | 0 完全关闭 / 1 正常 / 2 观察模式（只记录不拦截） |

**分层的目的**：避免「一个词就误杀」。单独出现「暴力」不拦（风险分 60），
同时出现「暴力 + 迷信」才拦（105 → 封顶 100）。词库的 `category` 字段同时充当打分权重。

### 2.2 顺带修掉的两个「AI 整体不可用」级问题

1. **管理端「保存配置」必然失败**：前端发的是布尔值 `contentFilter: true`，后端字段是 `Integer`，
   Jackson 类型不匹配 → 整个请求 400。**API Key 从来就存不进去** —— 这解释了线上
   `ai_config.api_key` 为空、AI 一律 1101 的现象。修为 `d.contentSafe ? 1 : 0`。
2. **DeepSeek 模型名已下线**：全仓库的 `deepseek-chat` 改为 `deepseek-flash`（官方现行名），
   并补了「模型名会变，上线前扫一遍」的检查清单。

### 2.3 两个成本优化

- **提示词结构**：DeepSeek 上下文缓存按公共前缀匹配，**命中价是未命中的 1/50**。
  原先把主题/风格拼进 `system`，前缀每次都变 → 缓存永远命中不了。现改为 `system` 恒定、
  变量全部进 `user`。**纪律：`system` 里不允许出现任何随请求变化的内容。**
- **关闭思考模式**：`deepseek-flash` 思考模式默认开启且 effort 为 `high`，会拖慢响应、
  按输出 token 计费，还可能让思维链吃掉 `max_tokens` 额度导致正文为空；且思考模式下
  `temperature` 被静默忽略。请求体显式带 `"thinking": {"type": "disabled"}`。

### 2.4 上游故障可见化

密钥无效 / 余额不足 / 被上游限流属**持续性配置故障**，原先被统一吞成「AI 开小差了」，
界面上完全看不出原因（本轮为此白排查了一轮）。现分别返回 `1103 / 1104 / 1105` 并透传真实原因、
写交互日志；网络抖动等**瞬时**故障仍走友好降级。学生端只看到「AI 老师暂时联系不上，
已用本地示例代替」，真实原因留给管理端「内容安全 → 交互日志」。

### 2.5 部署

本地与线上（阿里云 ECS）均已更新：线上新增 3 张表（20 → 23）、导入 45 条词库、
模型名改为 `deepseek-flash`、后端 jar 与前端产物均替换并验证。
运维细节见本地 `DEPLOY.md`（**已 gitignore，不公开**）。

---

## 3. 上一轮（2026-09-19）改动清单

### 3.1 部署上线（阿里云 ECS + 宝塔）

- 后端：Spring Boot fat jar + **Java 21（Alibaba Dragonwell）**，以 **systemd 服务**运行
  （开机自启、崩溃自动重启），监听 `127.0.0.1:8080`
- 前端：Vue 构建产物交由 **nginx** 提供，**复用站点原有 vhost**（`/api/` 反代到 8080），
  一行 nginx 配置都没改
- 数据库：MySQL 8.0，库 `ai_enlighten`，专用账号（最小权限，仅该库）
- 域名 + HTTPS：沿用服务器原有证书，http 自动 301 跳 https
- 详见本地 `DEPLOY.md`

### 3.2 修掉的 9 个问题

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 1 | **公网登录一律 403** `Invalid CORS request` | `WebConfig` 的 CORS 白名单只放行 `localhost`/`127.0.0.1`。浏览器对**同源 POST 也会带 Origin**，生产域名不在名单里 → 被 Spring 拒 | 改为可配置的 `cors.allowed-origins`，生产在 `application-prod.yml` 配置 |
| 2 | `init.sql` 表结构过期 | 代码有 19 个 `@TableName`，脚本只建 **16** 张（缺 `class_apply` / `blockly_template` / `notification`） | 补齐 3 张表 |
| 3 | `init.sql` 的 BCrypt 种子 hash 错误 | 该 hash 对应字符串 `password` 而非 `123456`，演示账号一律登录失败 | 换成正确的 hash，`fix_password.sql` 可作废 |
| 4 | `demo-data.sql` 漏插 `class_info` | 只插了 `class_student`，产生指向不存在班级的孤儿数据 | 补插班级 |
| 5 | **课程封面整批消失** | 封面按 `course-{id}.jpg` 匹配，而课程 id 漂移成了 9~14 | 课程 id 显式固定为 1~6（写进 `demo-data.sql`） |
| 6 | **未登录访问首页弹一摞「请先登录」** | `router/index.js` 的守卫**不校验登录态**（注释写着"原型阶段直接放行"），受保护页面先挂载、并发 5 个必 401 的请求 | 守卫加 token + 角色校验；401 提示做 3 秒去重；退出登录补上清 token |
| 7 | **构建产物从不清空** | 本机环境下 `fs.rmSync(recursive)` 静默失效，而 Vite 的 `emptyDir` 正是用它 → `dist` 无限累积（曾到 1300 个文件 / 23 份 `Login-*.js`） | 新增 `frontend/scripts/clean-dist.mjs` 挂到 `prebuild`，逐文件 `unlinkSync` 删除 |
| 8 | `course_category` id 漂移 | 服务器分类 id 是 6~10，而 `course.category_id` 是 1~5 → 关联断开，接口一律返回 `categoryName: null` | 分类 id 归位到 1~5 |
| 9 | **老师批改编程任务的分数从不回写** | `blockly_project.score` 在全后端**没有任何写入点**；学生做编程任务是把代码交到 `submission`，与作品表无关联 | 给 `blockly_project` 加 `assignment_id`，批改 `type=2` 任务时回写分数 |

> 附带修复：`GlobalExceptionHandler` 缺 404/405 分支 —— 路由写错时返回 `code:9999 系统繁忙`，
> 排查时极易误判成服务故障。现已分别返回明确的 404 / 405。

### 3.3 新增功能

- **经验值与等级体系**（完整重做）
  - 新增 `exp_log` 流水表，**账本为唯一真相**，总经验 = `SUM(exp)`；`(student_id, source_key)` 唯一保证幂等
  - 等级曲线 `need(n) = 100 + 50×(n-1)`（递增），称号 6 档：🌱AI 新芽 / 🔍AI 学徒 / 🧩积木达人 / 🚀AI 探索者 / 🎓AI 小专家 / 🏆AI 大师
  - 10 类经验来源、6 个业务触发点（看课里程碑与完成 / 编程作品 / AI 写作 / 闯关含满分加成 / 作业提交 / 教师批改优秀）+ 每日首次学习 + 连续学习里程碑（3/7/14/30 天）
  - 单日上限 300，防脚本刷分
  - 新增 `ExpService` / `ExpLevels` / `ActivityService`（后者从统计服务抽出，破除循环依赖）
  - 历史数据一次性回填（`sql/exp-backfill.sql`），老学生等级不归零
- **修改密码**：`POST /api/user/change-password`（路径刻意避开公开白名单 `/api/auth/**`）
  - 校验原密码 → 强度校验（8~32 位含字母数字）→ 新旧不得相同 → 15 分钟内失败 5 次锁定
  - 成功后写 `password_changed_at`，`AuthInterceptor` 据此**作废改密前签发的所有 token**
  - 顺带修复信息泄露：`User.password` 加 `@JsonIgnore`（此前 `/api/user/me` 会把 BCrypt 散列下发给前端）
- **用户偏好设置**：学习提醒 / 消息通知两个开关真正生效
  - `notify_enabled` 是**总开关**：关闭后后端不再为该用户生成任何站内消息
  - `remind_enabled` 是**子开关**：`StudyRemindTask` 每天 20:00 扫描，连续 ≥2 天没学习则推送提醒
- **新手教程页** `/student/guide`：5 步快速上手 + 8 个功能模块说明 + 7 条 FAQ（内容与实际实现严格对齐）
- **写作记录可重新打开**：`AiLab.vue` 的写作记录列表原本无点击事件、且加载时丢掉了 `content` 字段

### 3.4 学习数据「真实化」

首页数据条、班级通知、成就徽章、个人中心的学习数据原本都是**写死的假数据**（所有学生看到的一样），已全部改接真实接口：

| 位置 | 数据来源 |
|---|---|
| 首页数据条 4 个数字 | `/api/stats/student/my`（真实聚合） |
| 首页「班级通知」 | `/api/notify/my` 中 `type` 为 `task`/`grade` 的教师消息 |
| 首页 + 个人中心「成就徽章」 | `frontend/src/utils/achievements.js` 按统计值实时判定 |
| 个人中心「连续学习 / 经验 / 等级」 | `/api/stats/student/my` |
| 作品集评分状态 | 四分支：已批改 `XX 分` / 任务未批改 `待批改` / 自submit `已提交` / `草稿` |

---

## 4. 数据库（23 张表）

原有 19 张：`user` `class_info` `class_student` `class_apply` `course` `course_category` `course_progress`
`blockly_project` `blockly_template` `ai_writing` `quiz_question` `quiz_record` `qa_record`
`assignment` `submission` `announcement` `notification` `operation_log` `ai_config`

第三轮新增：`exp_log`（经验流水表）

**本轮（第四轮）新增 3 张**：

| 表 | 用途 |
|---|---|
| `sensitive_word` | 敏感词库。`category` 同时充当适宜性打分权重；`level`（1 提示 / 2 替换 / 3 拦截）决定处置动作 |
| `ai_interaction_log` | AI 交互日志与人工复核。每次调用都写一行；`hit_stage` 标明处理层级、`review_status` 标明是否需复核 |
| `ai_scene_policy` | 三个实验的场景策略（默认风格 / 题量 / 限时 + 五道安全开关） |

**本轮新增字段**：

| 表 | 字段 | 用途 |
|---|---|---|
| `ai_config` | `content_filter` 语义扩展 | 原「1 开 0 关」→「**0 完全关闭 / 1 正常 / 2 观察模式**」。取值天然向后兼容，老库无需迁移 |

**本轮新增脚本**（都在 `backend/sql/`）：`content-safety.sql`（建表 + 词库种子 + 场景策略，
幂等可重复执行；同样内容已合并进 `init.sql`，全新安装无需单独执行）

---

## 5. 关键约定与踩坑（省时间）

1. **Phosphor 图标**：必须 `import { PhChatCircleDots as ChatCircleDots } from '@phosphor-icons/vue'`。
   ⚠️ **纠正上一版的说法**：忘记 import **不会** build 失败 —— Vite 能编译通过，
   只在浏览器运行时抛 `ReferenceError` 导致**整页白屏**。本轮就栽在这上面
   （`TeacherLayout.vue` 用了 `ShieldCheck` 没 import，教师端一打开就是白的，排查了一轮）。
   排查手段：对 `.vue` 扫描「用了但没声明」的大写标识符，或在浏览器控制台看报错。
2. **dev server 在跑时，不要在 `frontend/` 里执行任何 build**：Vite 的 dev 与 build
   共用 `node_modules/.vite` 依赖预构建缓存，build 会覆盖 dev 正在服务的那份，
   浏览器随后请求按旧哈希命名的依赖 → 404/`504 Outdated Optimize Dep` → 白屏，且 **F5 救不回来**
   （要 `Ctrl+Shift+R`，或删 `node_modules/.vite` 后重启 dev server）。
   正确做法：先停 dev server 再 build。
3. **CSS token**：`main.css` 的 `:root` 里**没有** `--c-orange-deep`（旧页面引用它其实是失效的）→ 用 `--accent-deep`。改视觉先看 `styles/main.css` 与 `PROJECT-README.md` 第 4 章。
4. **Blockly**：解析 XML 用 `Blockly.utils.xml.textToDom(xml)` + `Blockly.Xml.domToWorkspace(dom, ws)`；`Blockly.utils.xml.domToWorkspace` **不存在**。`text_print` 生成 `console.log`（不要 `alert`）。
5. **MySQL 保留字** `read` → 用 `is_read`；控制台中文乱码是 **GBK 显示问题**，数据本身 UTF-8 正常（加 `--default-character-set=utf8mb4`）。
   同理：**PowerShell 输出里的中文乱码不代表数据坏了**，判断编码请用 node/浏览器实测。
6. **判断分数必须显式判空**：`score !== null && score !== undefined`。数据库里分数列默认是 `NULL`，而 **0 分是合法分数** —— 用真值判断会把 0 分当成"未批改"。
7. **`prebuild` 会清空 `dist`**：本项目 `npm run build` 前会执行 `node scripts/clean-dist.mjs dist`。
   本机环境下 Vite 自带的 `emptyOutDir` **不生效**（原因见第 3 节问题 7），所以这一步不能省。
   **不要**在多个进程里同时跑 `npm run build`（会互相删产物）。
8. **改密码的接口路径不能放 `/api/auth/**`**：该前缀在 `WebConfig` 里被拦截器整体放行（登录注册公开）。
9. **经验加分不要绕过 `ExpService.award`**：幂等与单日上限都在里面，直接 insert `exp_log` 会破坏约束。
10. **PowerShell 5.1 读 UTF-8 无 BOM 文件会按 GBK 解码**（中文显示乱码，文件本身没问题）；
    判断文件内容请用 read/grep 工具，不要用 PowerShell 字符串匹配下结论。
11. **git 代理残留会挡住本来能通的网络**：本机 `http.proxy` 曾被设为 `127.0.0.1:9674`，但该端口已无服务；
    而直连 GitHub 是通的。遇到 `Failed to connect to github.com ... via 127.0.0.1` 时：
    `git -c http.proxy= -c https.proxy= push origin main`（本次绕过），
    或 `git config --global --unset http.proxy` + `--unset https.proxy`（永久清掉）。
12. **诊断性查询不要丢弃 stderr**：本轮踩过 —— `mysql ... 2>/dev/null` 把 `Unknown column` 的报错整个吞掉，
    表现为"表是空的"，白排查一轮（实际是 ALTER 只加在本地库、忘了加服务器）。
13. **涉及外网的命令要拆小、给短超时**：直连 GitHub 从本机较慢，一条命令里串多个网络请求 + `--retry` + `sleep`
    很容易撞上工具超时被中断，看起来像"卡住"。
14. **远程执行 shell 用 here-string 管道**：`$script | ssh host 'bash -s'`，
    并先 `-replace "\`r\`n","\`n"` 换行符（否则 CRLF 会让远端 `bash` 把 `\r` 当成参数的一部分）。
    直接双引号里写 `$(...)` 会被 PowerShell 抢先展开。

---

## 6. 剩余待办（按优先级）

1. **论文文档**：需求分析 / 系统设计 / 测试报告（可参考 `backend/docs/test-cases-P1.md`）。
   内容安全模块的设计说明已整理进 `README.md` 技术亮点 §8，可直接作为系统设计章节素材。
2. **内容安全进阶（任务书已要求，本轮只做了最小可用版）**：
   - 归一化增强：全角转半角、变体字映射、去零宽字符 —— 现在 `赌 博`（插空格）、
     `賭博`（繁体）、`dubo`（拼音）**仍会漏过**；`SensitiveWordMatcher` 已留好替换点；
   - 词表遍历换 Trie/DFA（O(词数 × 文本长度) → O(文本长度)），词库上千后需要；
   - 词库规模扩充；「涉政」分类目前只建分类定义、无词条。
3. **课程 2~6 仍是占位视频地址**：课程 1 已指向站内真实视频 `/videos/what-is-ai.mp4`（可播放）；
   其余 5 门的 `video_url` 是 `douyin://play/ID-00x`，前端会显示「该课程暂无站内视频」且外链打不开。
4. **AI 出题主题偏题**：出题提示词是「中小学知识闯关出题老师」，
   实测生成的是天文/生物/数学题，与平台「AI 启蒙」定位不一致。
   改法：提示词里加入「围绕人工智能基础知识」。
5. **演示数据时间戳偏旧**：数据集中在 9 月上旬，「近 7 天学习动态」等图表偏空。
6. **可选优化**：前端 echarts/blockly 分包；JWT secret 改环境变量；
   `ai_config.max_concurrency` 存了但后端**未做并发限制**（本轮未处理）；
   管理端「新增模板」目前是前端本地新增（未落库）。

---

## 7. 验证账号与常用入口速查

> ⚠️ **以下密码只适用于「用 `init.sql` 全新建库」的本地环境。线上服务器的 admin 密码已单独修改，不在此文档记录。**

- 学生：`student01` → 首页 / 课程中心 / 编程实验室 / AI 魔法实验室 / 作业中心 / 答疑互动 / 学习数据 / 消息中心 / 个人中心 / 新手教程
- 教师：`teacher01` → 工作台 / 班级管理 / 任务布置 / 进度追踪 / 作业批改 / **内容复核** / 答疑互动 / 消息中心
- 管理：`admin` → 数据看板 / 用户管理 / 课程管理 / 实验资源（编程模板启停 + **AI 实验参数与安全策略** + AI 配置）/ **内容安全** / 公告与日志

密码均为 `123456`（**仅限本地演示**）；个人中心 → 设置里可自行修改密码。

**本轮新增页面的验收路径**：

1. 学生端 AI 实验室，写作主题填 `赌博技巧大全` → 应出现红色提示条，**不生成任何内容**
2. 教师端「内容复核」→ 能看到上一步那条记录（处理层级=输入侧检测、处置=拦截、命中词=赌博）
3. 管理端「内容安全」→ 词库 45 条 + 交互日志 + 概览数字
4. 管理端「实验资源」→ 五道安全策略标签**点一下刷新页面应保持**（此前是写死的前端演示数据）；
   「内容安全运行模式」三选一，切到「观察模式」后再做第 1 步 → 内容会放行，但日志照样记录命中
