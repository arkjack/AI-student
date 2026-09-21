-- ============================================================
-- AI 生成内容安全审查体系：敏感词库 + 交互日志/复核 + 场景策略
--
-- 对应毕业论文任务书新增要求：
--   「AI 生成内容安全审核机制」——敏感词过滤、科普内容准确性校验、
--   未成年人适宜性评估三重前置过滤；输入合规检测；教师人工复核；
--   交互日志留存可追溯。
--
-- 设计要点：
--   ① 词库落库而不是写死在 Java 常量里，管理端可增删改查、可启停
--   ② 日志与复核合用一张 ai_interaction_log：
--      每次 AI 调用都写一行（通过/拦截/降级都写），hit_stage 标明在哪一层被处理，
--      review_status 标明是否需要教师人工复核 —— 一张表就能回答「这次调用发生了什么」
--   ③ 场景策略一行一个 scene，把管理端「实验资源」页的开关接到真实的库表上
--
-- 执行方式：mysql -u<user> -p ai_enlighten < content-safety.sql
-- 全新安装无需执行本文件，init.sql 已包含同样的结构。
-- ============================================================

-- ---------- 1. 敏感词库 ----------
-- category 不只是分类标签，也是未成年人适宜性评估的权重来源（见 ContentSafetyServiceImpl）
-- level 决定处置动作：1 仅记分提示 / 2 替换为安全文案 / 3 直接拦截
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

-- ---------- 2. AI 交互日志 / 内容复核 ----------
-- hit_stage：0 通过 / 1 输入侧检测 / 2 敏感词过滤 / 3 适宜性评估 / 4 准确性校验 / 5 网络降级
-- action   ：pass 放行 / replace 替换 / block 拦截 / fallback 降级
-- review_status：0 无需复核 / 1 待复核 / 2 复核通过 / 3 复核驳回
CREATE TABLE IF NOT EXISTS `ai_interaction_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发起调用的用户（通常是学生）',
  `user_role` TINYINT DEFAULT NULL COMMENT '0管理员 1教师 2学生',
  `scene` VARCHAR(20) NOT NULL COMMENT 'writing创意写作 / chat智能答疑 / quiz知识闯关',
  `input_text` TEXT COMMENT '学生输入（写作主题或提问原文）',
  `output_text` TEXT COMMENT 'AI 原始返回（未处置前）',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '本次调用的模型名',
  `hit_stage` TINYINT NOT NULL DEFAULT 0 COMMENT '0通过 1输入拦截 2敏感词 3适宜性 4准确性 5网络降级',
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

-- ---------- 3. AI 实验场景策略 ----------
-- 与 ai_config.content_filter 的关系：content_filter 是运行模式（0完全关闭 / 1正常 / 2观察模式），
-- 为 0 时本表所有策略一律不生效；为 1 时逐场景生效；为 2 时策略照读但「自动拦截」统一关闭
-- （检测、打分、日志全在，只是不拦学生——用来排查误杀）。
-- 注意：老库的 content_filter 若还是「1开 0关」的语义，无需迁移，取值天然兼容。
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

-- ---------- 4. 场景策略默认数据 ----------
INSERT INTO `ai_scene_policy`
  (scene, scene_name, default_style, gen_count, answer_limit_sec,
   input_filter, output_sensitive, accuracy_check, minor_suitability, auto_block)
VALUES
  ('writing', '创意写作实验室', '童话风',   1,  0, 1, 1, 1, 1, 1),
  ('chat',    '智能答疑实验室', '友好助教', 0,  0, 1, 1, 1, 1, 1),
  ('quiz',    '知识闯关实验室', '竞赛风',   10, 30, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `scene_name` = VALUES(`scene_name`);

-- ---------- 5. 演示用初始敏感词库 ----------
-- ⚠️ 说明：以下仅为毕业设计演示用的示意词表，措辞克制、不含露骨内容。
--    「涉政」分类只建立分类定义、不预置任何词条，实际词表需按
--    《未成年人网络保护条例》等规范与平台运营要求另行维护。
--    停用（enabled=0）的词不会参与匹配，管理端可随时启停或增删。
INSERT INTO `sensitive_word` (`word`, `category`, `level`, `enabled`, `remark`) VALUES
  -- 色情（category=2）
  ('色情',     2, 3, 1, '色情类内容'),
  ('情色',     2, 3, 1, '色情类内容'),
  ('黄色网站', 2, 3, 1, '色情站点导流'),
  ('成人网站', 2, 3, 1, '色情站点导流'),
  ('裸聊',     2, 3, 1, '色情类内容'),
  ('一夜情',   2, 3, 1, '色情类内容'),
  ('约炮',     2, 3, 1, '色情类内容'),
  -- 暴力（category=3）
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
  -- 赌博毒品（category=4）
  ('赌博',     4, 3, 1, '赌博内容'),
  ('赌场',     4, 3, 1, '赌博内容'),
  ('赌球',     4, 3, 1, '赌博内容'),
  ('六合彩',   4, 2, 1, '博彩内容'),
  ('毒品',     4, 3, 1, '毒品内容'),
  ('吸毒',     4, 3, 1, '毒品内容'),
  ('冰毒',     4, 3, 1, '毒品内容'),
  ('大麻',     4, 3, 1, '毒品内容'),
  ('违禁药物', 4, 3, 1, '毒品内容'),
  -- 迷信诈骗（category=5）
  ('迷信',     5, 2, 1, '封建迷信，注意科学类语境存在误判可能'),
  ('算命',     5, 2, 1, '封建迷信'),
  ('看风水',   5, 1, 1, '封建迷信，仅记分提示'),
  ('诈骗',     5, 3, 1, '诈骗内容'),
  ('电信诈骗', 5, 2, 1, '诈骗内容'),
  ('刷单',     5, 2, 1, '网络诈骗'),
  ('传销',     5, 3, 1, '非法传销'),
  ('校园贷',   5, 3, 1, '非法借贷'),
  ('高利贷',   5, 3, 1, '非法借贷'),
  -- 广告导流（category=6）
  ('加微信',   6, 2, 1, '站外导流'),
  ('加QQ',     6, 2, 1, '站外导流'),
  ('扫码进群', 6, 2, 1, '站外导流'),
  ('免费领取', 6, 1, 1, '广告话术，仅记分提示'),
  ('私聊我',   6, 2, 1, '站外导流'),
  ('代写作业', 6, 2, 1, '学业作弊'),
  ('游戏外挂', 6, 2, 1, '作弊工具')
ON DUPLICATE KEY UPDATE `word` = VALUES(`word`);
