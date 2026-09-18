-- ============================================================
-- 历史编程作品的任务关联回填（一次性）
--
-- 背景：blockly_project 原先没有 assignment_id，老作品无法判断该不该被批改。
-- 这里按「submission.content = blockly_project.code_text」反推它们为哪个编程任务所提交。
--
-- ⚠️ 内容匹配是有歧义的，必须保守处理 —— 干跑时真实遇到两种情况：
--    ① 同一个学生保存过多份代码完全相同的作品，会同时匹配到同一条 submission；
--    ② 草稿（status=0）也会匹配上，但草稿并没有真正提交，不该关联任务、更不该有分数。
--    因此这里只取「每个（学生, 任务）里最新的一份 status=1 已提交作品」。
-- ============================================================

-- ① 关联来源任务（只处理已提交的作品，且每个学生每个任务只认最新一份）
UPDATE `blockly_project` bp
JOIN (
    SELECT bp2.student_id, s.assignment_id, MAX(bp2.id) AS keep_id
    FROM `blockly_project` bp2
    JOIN `submission` s ON s.student_id = bp2.student_id AND s.content = bp2.code_text
    JOIN `assignment` a ON a.id = s.assignment_id AND a.type = 2
    WHERE bp2.status = 1 AND bp2.assignment_id IS NULL
    GROUP BY bp2.student_id, s.assignment_id
) t ON t.keep_id = bp.id
SET bp.assignment_id = t.assignment_id;

-- ② 已被老师批改过的任务，把分数与评语同步回作品
UPDATE `blockly_project` bp
JOIN `submission` s ON s.student_id = bp.student_id AND s.assignment_id = bp.assignment_id
SET bp.score = s.score, bp.feedback = s.feedback
WHERE bp.score IS NULL AND s.status = 2 AND s.score IS NOT NULL;
