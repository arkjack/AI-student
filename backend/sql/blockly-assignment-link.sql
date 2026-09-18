-- ============================================================
-- 编程作品来源标记：区分「教师布置的编程任务」与「学生自己提交的作品」
--
-- 为什么需要：作品集原本只有 score 一个信号，无法判断一个作品该不该被批改。
--   · 教师布置的编程任务（assignment.type=2）→ 老师会批改 → 应显示「待批改」/「XX 分」
--   · 学生自己平时提交的作品            → 老师不批改   → 只应显示「已提交」
-- 有了 assignment_id 之后，前端才能给出正确的状态，老师批改也才有回写目标。
-- ============================================================
ALTER TABLE `blockly_project`
  ADD COLUMN `assignment_id` BIGINT DEFAULT NULL COMMENT '来源任务 id（教师布置的编程任务）；自己提交的作品为 NULL' AFTER `student_id`,
  ADD KEY `idx_assignment` (`assignment_id`);
