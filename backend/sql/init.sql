-- ============================================================
-- AI 启蒙星球 · 数据库初始化脚本
-- MySQL 8.0 / utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_enlighten DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ⚠️ 部署前请把下面的占位密码替换为你自己的强密码，并与 application-local.yml 保持一致
CREATE USER IF NOT EXISTS 'ai_edu'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ai_enlighten.* TO 'ai_edu'@'localhost';
FLUSH PRIVILEGES;

USE ai_enlighten;

-- ---------- 用户 ----------
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(50) DEFAULT NULL COMMENT '头像（emoji）',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '0管理员 1教师 2学生',
  `subject` VARCHAR(50) DEFAULT NULL COMMENT '教师学科',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `notify_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1接收站内消息 0不接收',
  `remind_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1开启学习提醒 0关闭',
  `password_changed_at` DATETIME DEFAULT NULL COMMENT '密码最后修改时间，用于作废此前签发的 token',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 班级 ----------
CREATE TABLE IF NOT EXISTS `class_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '班级名称',
  `grade` VARCHAR(30) DEFAULT NULL COMMENT '年级',
  `teacher_id` BIGINT NOT NULL COMMENT '班主任 user.id',
  `description` VARCHAR(200) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ---------- 班级学生关联 ----------
CREATE TABLE IF NOT EXISTS `class_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_class_student` (`class_id`,`student_id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关联表';

-- ---------- 入班申请 ----------
CREATE TABLE IF NOT EXISTS `class_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL COMMENT '学生 user.id',
  `class_id` BIGINT NOT NULL COMMENT '班级 id',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1同意 2拒绝',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_class_status` (`class_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入班申请表';

-- ---------- 课程分类 ----------
CREATE TABLE IF NOT EXISTS `course_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(30) NOT NULL,
  `emoji` VARCHAR(10) DEFAULT NULL COMMENT '分类图标',
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- ---------- 课程 ----------
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL,
  `category_id` BIGINT NOT NULL,
  `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '1入门 2进阶 3挑战',
  `duration_minutes` INT NOT NULL DEFAULT 15,
  `cover_emoji` VARCHAR(10) DEFAULT NULL COMMENT '封面兜底 emoji',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图 URL',
  `summary` VARCHAR(300) DEFAULT NULL COMMENT '简介',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '抖音分享链接',
  `teacher` VARCHAR(50) DEFAULT NULL,
  `views` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ---------- 课程学习进度 ----------
CREATE TABLE IF NOT EXISTS `course_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '0-100',
  `watch_seconds` INT NOT NULL DEFAULT 0,
  `completed` TINYINT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_stu_course` (`student_id`,`course_id`),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程进度表';

-- ---------- 积木编程作品 ----------
CREATE TABLE IF NOT EXISTS `blockly_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `blocks_json` TEXT COMMENT '积木结构 JSON/XML',
  `code_text` TEXT COMMENT '生成的代码',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已提交',
  `score` TINYINT DEFAULT NULL,
  `feedback` VARCHAR(500) DEFAULT NULL,
  `submitted_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程作品表';

-- ---------- 编程模板（编程实验室的可选模板，管理端可启停）----------
CREATE TABLE IF NOT EXISTS `blockly_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `level` VARCHAR(20) DEFAULT '入门',
  `emoji` VARCHAR(10) DEFAULT NULL,
  `enabled` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程模板表';

-- ---------- 作业任务 ----------
CREATE TABLE IF NOT EXISTS `assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT NOT NULL,
  `class_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) DEFAULT NULL COMMENT '任务要求',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '1课程 2编程 3AI实验 4闯关',
  `resource_id` BIGINT DEFAULT NULL COMMENT '关联资源 id（课程/模板/实验）',
  `deadline` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1进行中 0已截止',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业任务表';

-- ---------- 作业提交 ----------
CREATE TABLE IF NOT EXISTS `submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `content` TEXT COMMENT '提交内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1已提交 2已批改',
  `score` TINYINT DEFAULT NULL,
  `feedback` VARCHAR(500) DEFAULT NULL,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assign_stu` (`assignment_id`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- ---------- AI 创意写作记录 ----------
CREATE TABLE IF NOT EXISTS `ai_writing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `topic` VARCHAR(100) NOT NULL,
  `style` VARCHAR(30) DEFAULT NULL,
  `content` TEXT,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已提交',
  `score` TINYINT DEFAULT NULL,
  `feedback` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创意写作表';

-- ---------- 闯关题库 ----------
CREATE TABLE IF NOT EXISTS `quiz_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `level` VARCHAR(30) NOT NULL COMMENT '关卡名',
  `question` VARCHAR(200) NOT NULL,
  `option_a` VARCHAR(100) NOT NULL,
  `option_b` VARCHAR(100) NOT NULL,
  `option_c` VARCHAR(100) DEFAULT NULL,
  `option_d` VARCHAR(100) DEFAULT NULL,
  `answer` CHAR(1) NOT NULL COMMENT 'A/B/C/D',
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闯关题库表';

-- ---------- 闯关记录 ----------
CREATE TABLE IF NOT EXISTS `quiz_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `level` VARCHAR(30) NOT NULL,
  `score` INT NOT NULL DEFAULT 0,
  `correct_count` INT NOT NULL DEFAULT 0,
  `total_count` INT NOT NULL DEFAULT 0,
  `finished_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闯关记录表';

-- ---------- 答疑记录 ----------
CREATE TABLE IF NOT EXISTS `qa_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `question` VARCHAR(500) NOT NULL,
  `answer` TEXT COMMENT '回答（AI 或教师）',
  `answer_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1AI 2教师',
  `teacher_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答疑记录表';

-- ---------- 公告 ----------
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) DEFAULT NULL,
  `tag` VARCHAR(20) NOT NULL DEFAULT '公告' COMMENT '公告/活动/维护',
  `is_top` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ---------- 站内消息通知 ----------
-- ⚠️ 列名必须用 is_read，因为 read 是 MySQL 保留字
CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) DEFAULT 'info' COMMENT 'task任务/grade批改/announce公告/apply入班/info',
  `title` VARCHAR(100) DEFAULT NULL,
  `content` VARCHAR(255) DEFAULT NULL,
  `link` VARCHAR(120) DEFAULT NULL COMMENT '点击跳转的前端路由',
  `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ---------- 经验流水（经验值与等级体系的账本）----------
-- 总经验 = 同一 student_id 下所有 exp 之和；只增不减，负数仅用于管理员撤销。
-- (student_id, source_key) 唯一 → 同一件事重复触发只记一次，天然防重复提交刷分。
CREATE TABLE IF NOT EXISTS `exp_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `source_type` VARCHAR(30) NOT NULL COMMENT 'daily/course/project/writing/quiz/submission/streak/backfill',
  `source_key` VARCHAR(80) NOT NULL COMMENT '幂等键，如 course:3:done',
  `exp` INT NOT NULL COMMENT '本次经验，正数为获得，负数为撤销',
  `remark` VARCHAR(120) DEFAULT NULL COMMENT '展示给学生的一句话说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_source` (`student_id`,`source_key`),
  KEY `idx_student_created` (`student_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经验流水表';

-- ---------- 操作日志 ----------
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_role` TINYINT DEFAULT NULL,
  `op_type` VARCHAR(30) NOT NULL COMMENT '操作类型',
  `detail` VARCHAR(300) DEFAULT NULL,
  `ip` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ---------- AI 接口配置（单行表） ----------
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider` VARCHAR(30) NOT NULL DEFAULT 'deepseek',
  `api_key` VARCHAR(200) NOT NULL COMMENT '密钥（仅后端持有）',
  `base_url` VARCHAR(200) DEFAULT 'https://api.deepseek.com/v1',
  `model` VARCHAR(50) DEFAULT 'deepseek-flash',
  `timeout_sec` INT NOT NULL DEFAULT 60,
  `max_concurrency` INT NOT NULL DEFAULT 5,
  `rate_limit_per_min` INT NOT NULL DEFAULT 20,
  `content_filter` TINYINT NOT NULL DEFAULT 1 COMMENT '内容安全运行模式：0完全关闭 1正常 2观察模式(只记录不拦截)',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 接口配置表';

-- ---------- 敏感词库 ----------
-- category 同时是未成年人适宜性评估的权重来源；level 决定处置动作（1提示 2替换 3拦截）
CREATE TABLE IF NOT EXISTS `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `category` TINYINT NOT NULL DEFAULT 7 COMMENT '1涉政 2色情 3暴力 4赌博毒品 5迷信诈骗 6广告导流 7其他',
  `level` TINYINT NOT NULL DEFAULT 2 COMMENT '1提示 2替换 3拦截',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `remark` VARCHAR(120) DEFAULT NULL COMMENT '备注，说明收录原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';

-- ---------- AI 交互日志 / 内容复核 ----------
-- 每次 AI 调用都写一行（通过/拦截/降级都写），hit_stage 标明在哪一层被处理
CREATE TABLE IF NOT EXISTS `ai_interaction_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发起调用的用户（通常是学生）',
  `user_role` TINYINT DEFAULT NULL COMMENT '0管理员 1教师 2学生',
  `scene` VARCHAR(20) NOT NULL COMMENT 'writing创意写作 / chat智能答疑 / quiz知识闯关',
  `input_text` TEXT COMMENT '学生输入（写作主题或提问原文）',
  `output_text` TEXT COMMENT 'AI 原始返回（未处置前）',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '本次调用的模型名',
  `hit_stage` TINYINT NOT NULL DEFAULT 0 COMMENT '0通过 1输入侧检测 2敏感词 3适宜性 4准确性 5网络降级',
  `hit_words` VARCHAR(255) DEFAULT NULL COMMENT '命中的敏感词，逗号分隔',
  `risk_score` INT NOT NULL DEFAULT 0 COMMENT '未成年人适宜性风险分 0-100',
  `action` VARCHAR(20) NOT NULL DEFAULT 'pass' COMMENT 'pass/replace/block/fallback',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '处置原因（展示给复核教师）',
  `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0无需复核 1待复核 2通过 3驳回',
  `reviewer_id` BIGINT DEFAULT NULL,
  `review_remark` VARCHAR(255) DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `elapsed_ms` INT DEFAULT NULL COMMENT '模型调用耗时',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  KEY `idx_review` (`review_status`,`created_at`),
  KEY `idx_stage` (`hit_stage`),
  KEY `idx_scene_created` (`scene`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 交互日志与内容复核表';

-- ---------- AI 实验场景策略 ----------
-- 与 ai_config.content_filter 的关系：content_filter 是总开关，为 0 时本表策略一律不生效
CREATE TABLE IF NOT EXISTS `ai_scene_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scene` VARCHAR(20) NOT NULL COMMENT 'writing/chat/quiz',
  `scene_name` VARCHAR(40) NOT NULL COMMENT '展示名，如「创意写作实验室」',
  `default_style` VARCHAR(30) DEFAULT NULL COMMENT '该实验的默认风格',
  `gen_count` INT NOT NULL DEFAULT 0 COMMENT '单次生成数量：quiz=出题数，writing=生成篇数，chat=不适用(0)',
  `answer_limit_sec` INT NOT NULL DEFAULT 0 COMMENT '答题限时（秒），仅 quiz 生效，0=不限时',
  `input_filter` TINYINT NOT NULL DEFAULT 1 COMMENT '输入侧合规检测',
  `output_sensitive` TINYINT NOT NULL DEFAULT 1 COMMENT '输出敏感词过滤',
  `accuracy_check` TINYINT NOT NULL DEFAULT 1 COMMENT '生成内容准确性校验',
  `minor_suitability` TINYINT NOT NULL DEFAULT 1 COMMENT '未成年人适宜性评估',
  `auto_block` TINYINT NOT NULL DEFAULT 1 COMMENT '命中即自动拦截（关闭则只替换/记录）',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene` (`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 实验场景安全策略';

-- ---------- 初始数据 ----------
INSERT INTO `user` (username, password, real_name, nickname, avatar, role, subject) VALUES
('admin', '$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '平台管理员', '管理员', '🛠️', 0, NULL),
('teacher01', '$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '秦老师', '秦老师', '👩‍🏫', 1, '信息技术'),
('student01', '$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22', '小明', '小明', '🦊', 2, NULL);
-- 说明：上面是 BCrypt("123456")，演示账号统一密码 123456
-- ⚠️ 校验标准：三个账号都能登录成功（接口返回 code=0）才算正确。
--    历史坑：早期这里的 hash 实际对应字符串 "password" 而非 "123456"，导致演示账号一律登录失败，
--    当时靠同目录的 fix_password.sql 手工补救；该修复已合并进本文件，fix_password.sql 可作废。

-- 显式指定 id：course.category_id 依赖这 5 个值，隐式自增在重复执行时会漂移
INSERT INTO `course_category` (id, name, emoji, sort) VALUES
(1,'AI 入门','🚀',1),(2,'图形化编程','🧩',2),(3,'机器学习','🤖',3),(4,'AI 与生活','🏠',4),(5,'趣味实验','🧪',5);

-- 编程模板：学生端「编程实验室」的可选模板，管理端「实验资源」页可启停（enabled）
INSERT INTO `blockly_template` (id, name, description, level, emoji, enabled) VALUES
(1, '小猫咪动起来', '让小猫走到屏幕中央', '入门', '🐱', 1),
(2, '会算数的机器人', '机器人帮你算加减法', '入门', '🤖', 1),
(3, '小星星循环舞', '用循环画出一片星空', '进阶', '✨', 1),
(4, '自动巡逻车', '条件判断：碰到墙就转弯', '进阶', '🚗', 1);

-- AI 接口配置（单行表，id 固定为 1）。api_key 留空：部署后在管理端「实验资源」页填写；
-- 留空时 AI 三个接口统一返回业务码 1101（未配置或不可用）。
INSERT INTO `ai_config` (id, provider, api_key, base_url, model, timeout_sec, max_concurrency, rate_limit_per_min, content_filter) VALUES
(1, 'deepseek', '', 'https://api.deepseek.com/v1', 'deepseek-flash', 60, 3, 20, 1);

-- AI 实验场景安全策略：管理端「实验资源」页三个实验卡片的真实数据源。
-- 缺行时后端会自动补默认行，因此这里少写一行也不会导致 NPE。
INSERT INTO `ai_scene_policy`
  (scene, scene_name, default_style, gen_count, answer_limit_sec,
   input_filter, output_sensitive, accuracy_check, minor_suitability, auto_block)
VALUES
  ('writing', '创意写作实验室', '童话风',   1,  0, 1, 1, 1, 1, 1),
  ('chat',    '智能答疑实验室', '友好助教', 0,  0, 1, 1, 1, 1, 1),
  ('quiz',    '知识闯关实验室', '竞赛风',   10, 30, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `scene_name` = VALUES(`scene_name`);

-- 演示用初始敏感词库（同 content-safety.sql）。
-- ⚠️ 仅为毕业设计演示词表，措辞克制、不含露骨内容；「涉政」分类只建立分类定义、
--    不预置任何词条，实际词表需按《未成年人网络保护条例》等规范另行维护。
INSERT INTO `sensitive_word` (`word`, `category`, `level`, `enabled`, `remark`) VALUES
  ('色情',     2, 3, 1, '色情类内容'),
  ('情色',     2, 3, 1, '色情类内容'),
  ('黄色网站', 2, 3, 1, '色情站点导流'),
  ('成人网站', 2, 3, 1, '色情站点导流'),
  ('裸聊',     2, 3, 1, '色情类内容'),
  ('一夜情',   2, 3, 1, '色情类内容'),
  ('约炮',     2, 3, 1, '色情类内容'),
  ('暴力',     3, 2, 1, '暴力描写'),
  ('血腥',     3, 3, 1, '血腥描写'),
  ('凶杀',     3, 3, 1, '凶杀描写'),
  ('砍人',     3, 3, 1, '暴力伤害'),
  ('虐待',     3, 3, 1, '暴力伤害'),
  ('自杀',     3, 3, 1, '自伤风险信号，命中后必须进教师复核队列'),
  ('自残',     3, 3, 1, '自伤风险信号，命中后必须进教师复核队列'),
  ('恐怖袭击', 3, 3, 1, '恐怖主义内容'),
  ('恐怖主义', 3, 3, 1, '恐怖主义内容'),
  ('爆炸物',   3, 3, 1, '危险物品'),
  ('枪械',     3, 3, 1, '危险物品'),
  ('枪支',     3, 3, 1, '危险物品'),
  ('弹药',     3, 3, 1, '危险物品'),
  ('赌博',     4, 3, 1, '赌博内容'),
  ('赌场',     4, 3, 1, '赌博内容'),
  ('赌球',     4, 3, 1, '赌博内容'),
  ('六合彩',   4, 2, 1, '博彩内容'),
  ('毒品',     4, 3, 1, '毒品内容'),
  ('吸毒',     4, 3, 1, '毒品内容'),
  ('冰毒',     4, 3, 1, '毒品内容'),
  ('大麻',     4, 3, 1, '毒品内容'),
  ('违禁药物', 4, 3, 1, '毒品内容'),
  ('迷信',     5, 2, 1, '封建迷信，注意科学类语境存在误判可能'),
  ('算命',     5, 2, 1, '封建迷信'),
  ('看风水',   5, 1, 1, '封建迷信，仅记分提示'),
  ('诈骗',     5, 3, 1, '诈骗内容'),
  ('电信诈骗', 5, 2, 1, '诈骗内容'),
  ('刷单',     5, 2, 1, '网络诈骗'),
  ('传销',     5, 3, 1, '非法传销'),
  ('校园贷',   5, 3, 1, '非法借贷'),
  ('高利贷',   5, 3, 1, '非法借贷'),
  ('加微信',   6, 2, 1, '站外导流'),
  ('加QQ',     6, 2, 1, '站外导流'),
  ('扫码进群', 6, 2, 1, '站外导流'),
  ('免费领取', 6, 1, 1, '广告话术，仅记分提示'),
  ('私聊我',   6, 2, 1, '站外导流'),
  ('代写作业', 6, 2, 1, '学业作弊'),
  ('游戏外挂', 6, 2, 1, '作弊工具')
ON DUPLICATE KEY UPDATE `word` = VALUES(`word`);
