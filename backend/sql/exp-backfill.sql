-- ============================================================
-- 一次性回填历史经验（只在启用经验体系时执行一次）
--
-- 背景：旧版本的经验是「实时推导」的（完成课程×50 + 作品×20 + 已批改×10 + 闯关×10），
-- 没有流水。切到账本制后如果不回填，所有老学生会从 0 经验重新开始。
--
-- 做法：按旧公式算出每人当前应得的总量，写成一条 backfill 流水作为起点。
-- source_key 带版本号（backfill:v1），保证重复执行不会重复发。
--
-- ⚠️ created_at 必须落在「昨天或更早」：ExpService 的单日上限是按 created_at 的日期统计的，
--    如果回填记成今天，老学生今天的额度会被一次性占满（450 > 300），当天再也拿不到任何经验。
-- ============================================================
INSERT INTO `exp_log` (`student_id`, `source_type`, `source_key`, `exp`, `remark`, `created_at`)
SELECT sid,
       'backfill',
       CONCAT('backfill:v1:', sid),
       c * 50 + w * 20 + g * 10 + q * 10,
       CONCAT('历史学习成果折算：完成课程 ', c, ' 门、作品 ', w, ' 个、已批改作业 ', g, ' 次、闯关 ', q, ' 次'),
       DATE_SUB(CURDATE(), INTERVAL 1 DAY)
FROM (
    SELECT u.id AS sid,
           (SELECT COUNT(*) FROM `course_progress` cp
             WHERE cp.student_id = u.id AND (cp.progress >= 100 OR cp.completed = 1)) AS c,
           ((SELECT COUNT(*) FROM `blockly_project` bp
              WHERE bp.student_id = u.id AND bp.status = 1)
            + (SELECT COUNT(*) FROM `ai_writing` aw
                WHERE aw.student_id = u.id AND aw.status = 1)) AS w,
           (SELECT COUNT(*) FROM `submission` s
             WHERE s.student_id = u.id AND s.status = 2 AND s.score IS NOT NULL) AS g,
           (SELECT COUNT(*) FROM `quiz_record` qr WHERE qr.student_id = u.id) AS q
    FROM `user` u
    WHERE u.role = 2
) t
WHERE c * 50 + w * 20 + g * 10 + q * 10 > 0;
