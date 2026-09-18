-- ============================================================
-- 经验值与等级体系：经验流水表
-- 设计要点：
--   ① 账本为唯一真相：总经验 = SUM(exp)，不再用「实时推导」的方式算分
--   ② 只增不减：正常业务只插入正数；负数仅留给管理员撤销
--   ③ 幂等：(student_id, source_key) 唯一，同一件事重复触发只记一次
-- ============================================================
CREATE TABLE IF NOT EXISTS `exp_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `source_type` VARCHAR(30) NOT NULL COMMENT 'daily/course/project/writing/quiz/submission/streak/backfill',
  `source_key` VARCHAR(80) NOT NULL COMMENT '幂等键，如 course:3:done；同一学生同一 key 只记一次',
  `exp` INT NOT NULL COMMENT '本次经验，正数为获得，负数为撤销',
  `remark` VARCHAR(120) DEFAULT NULL COMMENT '展示给学生的一句话说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_source` (`student_id`,`source_key`),
  KEY `idx_student_created` (`student_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经验流水表';
