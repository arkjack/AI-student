# 从哪里继续（CONTINUE.md）

> **新会话先读这里 + `PROJECT-README.md`（项目主文档）**，即可无缝接上。
> 最后更新：2026-09-19

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
本轮补齐了**经验值与等级体系、修改密码、用户偏好设置、新手教程**，
并把首页学习数据、班级通知、成就徽章、作品评分语义全部改为真实数据。
剩余主要是**论文文档**与几处演示数据打磨。

---

## 2. 本轮（2026-09-19）改动清单

### 2.1 部署上线（阿里云 ECS + 宝塔）

- 后端：Spring Boot fat jar + **Java 21（Alibaba Dragonwell）**，以 **systemd 服务**运行
  （开机自启、崩溃自动重启），监听 `127.0.0.1:8080`
- 前端：Vue 构建产物交由 **nginx** 提供，**复用站点原有 vhost**（`/api/` 反代到 8080），
  一行 nginx 配置都没改
- 数据库：MySQL 8.0，库 `ai_enlighten`，专用账号（最小权限，仅该库）
- 域名 + HTTPS：沿用服务器原有证书，http 自动 301 跳 https
- 详见本地 `DEPLOY.md`

### 2.2 修掉的 9 个问题

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

### 2.3 新增功能

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

### 2.4 学习数据「真实化」

首页数据条、班级通知、成就徽章、个人中心的学习数据原本都是**写死的假数据**（所有学生看到的一样），已全部改接真实接口：

| 位置 | 数据来源 |
|---|---|
| 首页数据条 4 个数字 | `/api/stats/student/my`（真实聚合） |
| 首页「班级通知」 | `/api/notify/my` 中 `type` 为 `task`/`grade` 的教师消息 |
| 首页 + 个人中心「成就徽章」 | `frontend/src/utils/achievements.js` 按统计值实时判定 |
| 个人中心「连续学习 / 经验 / 等级」 | `/api/stats/student/my` |
| 作品集评分状态 | 四分支：已批改 `XX 分` / 任务未批改 `待批改` / 自submit `已提交` / `草稿` |

---

## 3. 数据库（20 张表）

原有 19 张：`user` `class_info` `class_student` `class_apply` `course` `course_category` `course_progress`
`blockly_project` `blockly_template` `ai_writing` `quiz_question` `quiz_record` `qa_record`
`assignment` `submission` `announcement` `notification` `operation_log` `ai_config`

**本轮新增**：`exp_log`（经验流水表）

**本轮新增字段**：

| 表 | 字段 | 用途 |
|---|---|---|
| `user` | `notify_enabled` / `remind_enabled` | 两个偏好开关 |
| `user` | `password_changed_at` | 改密后作废旧 token |
| `blockly_project` | `assignment_id` | 区分「教师布置的编程任务作品」与「学生自由创作」 |

**本轮新增脚本**（都在 `backend/sql/`）：
`exp-system.sql`、`exp-backfill.sql`、`blockly-assignment-link.sql`、`blockly-link-backfill.sql`

---

## 4. 关键约定与踩坑（省时间）

1. **Phosphor 图标**：必须 `import { PhChatCircleDots as ChatCircleDots } from '@phosphor-icons/vue'`（裸名会 build 失败）；emoji 只做头像/内容，**不当 UI 图标**。
2. **CSS token**：`main.css` 的 `:root` 里**没有** `--c-orange-deep`（旧页面引用它其实是失效的）→ 用 `--accent-deep`。改视觉先看 `styles/main.css` 与 `PROJECT-README.md` 第 4 章。
3. **Blockly**：解析 XML 用 `Blockly.utils.xml.textToDom(xml)` + `Blockly.Xml.domToWorkspace(dom, ws)`；`Blockly.utils.xml.domToWorkspace` **不存在**。`text_print` 生成 `console.log`（不要 `alert`）。
4. **MySQL 保留字** `read` → 用 `is_read`；控制台中文乱码是 **GBK 显示问题**，数据本身 UTF-8 正常（加 `--default-character-set=utf8mb4`）。
5. **判断分数必须显式判空**：`score !== null && score !== undefined`。数据库里分数列默认是 `NULL`，而 **0 分是合法分数** —— 用真值判断会把 0 分当成"未批改"。
6. **`prebuild` 会清空 `dist`**：本项目 `npm run build` 前会执行 `node scripts/clean-dist.mjs dist`。
   本机环境下 Vite 自带的 `emptyOutDir` **不生效**（原因见第 2.2 节问题 7），所以这一步不能省。
   **不要**在多个进程里同时跑 `npm run build`（会互相删产物）。
7. **改密码的接口路径不能放 `/api/auth/**`**：该前缀在 `WebConfig` 里被拦截器整体放行（登录注册公开）。
8. **经验加分不要绕过 `ExpService.award`**：幂等与单日上限都在里面，直接 insert `exp_log` 会破坏约束。
9. **PowerShell 5.1 读 UTF-8 无 BOM 文件会按 GBK 解码**（中文显示乱码，文件本身没问题）；
   判断文件内容请用 read/grep 工具，不要用 PowerShell 字符串匹配下结论。

---

## 5. 剩余待办（按优先级）

1. **论文文档**：需求分析 / 系统设计 / 测试报告（可参考 `backend/docs/test-cases-P1.md`）。
2. **课程 2~6 仍是占位视频地址**：课程 1 已指向站内真实视频 `/videos/what-is-ai.mp4`（可播放）；
   其余 5 门的 `video_url` 是 `douyin://play/ID-00x`，前端会显示「该课程暂无站内视频」且外链打不开。
   补齐真实视频后更新 `video_url`；暂无视频建议置空以免出现死链。
3. **AI 出题主题偏题**：`AiServiceImpl` 第 103 行的出题提示词是「中小学知识闯关出题老师」，
   实测生成的是天文/生物/数学题，与平台「AI 启蒙」定位和内置题库（全是 AI 题）不一致。
   改法：提示词里加入「围绕人工智能基础知识」。
4. **演示数据时间戳偏旧**：数据集中在 9 月上旬，「近 7 天学习动态」等图表偏空。
   如需演示效果更好可把相关时间字段整体平移到最近一周（纯数据改动，无需重新构建）。
5. **可选优化**：前端 echarts/blockly 分包；JWT secret 改环境变量；管理端「新增模板」目前是前端本地新增（未落库）。

---

## 6. 验证账号与常用入口速查

> ⚠️ **以下密码只适用于「用 `init.sql` 全新建库」的本地环境。线上服务器的 admin 密码已单独修改，不在此文档记录。**

- 学生：`student01` → 首页 / 课程中心 / 编程实验室 / AI 魔法实验室 / 作业中心 / 答疑互动 / 学习数据 / 消息中心 / 个人中心 / **新手教程**
- 教师：`teacher01` → 工作台 / 班级管理 / 任务布置 / 进度追踪 / 作业批改 / 答疑互动 / 消息中心
- 管理：`admin` → 数据看板 / 用户管理 / 课程管理 / 实验资源（编程模板启停 + AI 配置）/ 公告与日志

密码均为 `123456`（**仅限本地演示**）；个人中心 → 设置里可自行修改密码。
