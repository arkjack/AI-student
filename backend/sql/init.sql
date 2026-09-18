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
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(50) DEFAULT NULL COMMENT '头像（emoji）',
  `role` TINYINT NOT NULL DEFAULT 2 COMMENT '0管理员 1教师 2学生',
  `subject` VARCHAR(50) DEFAULT NULL COMMENT '教师学科',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
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
  `model` VARCHAR(50) DEFAULT 'deepseek-chat',
  `timeout_sec` INT NOT NULL DEFAULT 60,
  `max_concurrency` INT NOT NULL DEFAULT 5,
  `rate_limit_per_min` INT NOT NULL DEFAULT 20,
  `content_filter` TINYINT NOT NULL DEFAULT 1 COMMENT '未成年内容过滤开关',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 接口配置表';

-- ---------- 初始数据 ----------
INSERT INTO `user` (username, password, real_name, nickname, avatar, role, subject) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '平台管理员', '管理员', '🛠️', 0, NULL),
('teacher01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '秦老师', '秦老师', '👩‍🏫', 1, '信息技术'),
('student01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '小明', '小明', '🦊', 2, NULL);
-- 说明：上面是 BCrypt("123456")，演示账号统一密码 123456

INSERT INTO `course_category` (name, emoji, sort) VALUES
('AI 入门','🚀',1),('图形化编程','🧩',2),('机器学习','🤖',3),('AI 与生活','🏠',4),('趣味实验','🧪',5);
