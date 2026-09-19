-- ============================================================
-- AI 启蒙星球 · 演示数据
-- 依赖：必须先执行 init.sql（用户 / 课程分类 / 编程模板 / AI 配置已就位）
-- 用法：mysql -u root -p ai_enlighten < demo-data.sql
-- ============================================================
USE ai_enlighten;

-- ---------- 班级 ----------
-- teacher_id 用子查询取 teacher01 的真实主键，避免硬编码用户 id
INSERT INTO `class_info` (id, name, grade, teacher_id, description)
SELECT 1, '五年级(2)班', '五年级', id, 'AI 启蒙班' FROM `user` WHERE username = 'teacher01';

-- ---------- 学生入班 ----------
-- 历史坑：早期版本只插了本表却没插 class_info，产生指向不存在班级的孤儿数据，
--        教师端「班级管理」因此为空。本脚本已修正插入顺序。
INSERT INTO `class_student` (class_id, student_id)
SELECT 1, id FROM `user` WHERE username = 'student01';

-- ---------- 课程 ----------
-- video_url 约定：只有以 `/` 开头（站内，如 /videos/xxx.mp4）或 `http(s)://` 开头（外链）
--   才会被前端当作可播放视频（见 CourseDetail.vue 的 isPlayable 判断）；
--   其他值一律显示「该课程暂无站内视频」。示教用的 `douyin://play/ID-00x` 是占位值，
--   浏览器无法打开，补到真实视频前建议留空或换成外链。
-- ⚠️ 必须显式指定 id 为 1~6。
--    前端 CourseCard.vue / CourseDetail.vue 是按 `course-{id}.jpg` 去匹配封面插画的，
--    id 一旦漂移（例如本脚本被重复执行、自增基数被推高），封面会静默退化成 emoji 兜底，
--    且不报任何错，非常难排查。
INSERT INTO `course` (id, title, category_id, difficulty, duration_minutes, cover_emoji, cover_image, summary, video_url, teacher, views, status) VALUES
(1, '什么是人工智能？', 1, 1, 3, '🤖', NULL, '和机器人小智一起认识 AI 世界的奇妙旅程！', '/videos/what-is-ai.mp4', '秦老师', 1284, 1),
(2, '有趣的图像识别', 1, 2, 25, '👀', NULL, '为什么手机能认出你的脸？图像识别原理大揭秘。', 'douyin://play/ID-002', '秦老师', 986, 1),
(3, '方块编程第一课', 2, 1, 30, '🧩', NULL, '拖一拖、拼一拼，用积木让角色动起来！', 'douyin://play/ID-003', '刘老师', 1523, 1),
(4, '认识机器学习', 3, 3, 35, '🧠', NULL, '机器是怎么学习的？像训练小狗一样训练 AI。', 'douyin://play/ID-004', '秦老师', 872, 1),
(5, '语音助手会说话', 4, 2, 20, '🎙️', NULL, '看看语音助手是怎么听懂你的话。', 'douyin://play/ID-005', '王老师', 764, 1),
(6, 'AI 魔法画师', 5, 2, 22, '🎨', NULL, '输入一句话，AI 就能画出你心中的神奇画面！', 'douyin://play/ID-006', '刘老师', 1105, 1);

-- ---------- 公告 ----------
INSERT INTO `announcement` (title, content, tag, is_top) VALUES
('新学期 AI 挑战赛开始啦！', '完成 3 个 AI 实验即可获得「未来科学家」徽章。', '活动', 1),
('新增课程《认识机器学习》', '欢迎大家学习。', '公告', 0),
('系统维护通知', '本周六 22:00-24:00 系统维护。', '维护', 0);

-- ---------- 闯关题库 ----------
INSERT INTO `quiz_question` (level, question, option_a, option_b, option_c, option_d, answer, sort) VALUES
('AI 基础', '人工智能的英文缩写是？', 'AI', 'AR', 'VR', 'IT', 'A', 1),
('AI 基础', '下面哪个是人工智能的应用？', '人脸识别', '电风扇', '自行车', '微波炉', 'A', 2),
('AI 基础', '机器人认识世界需要靠什么？', '传感器', '颜料', '橡皮', '胶水', 'A', 3),
('AI 基础', '机器学习中，机器学习的“原料”是什么？', '数据', '西瓜', '面粉', '颜料', 'A', 4),
('AI 基础', '语音助手能听懂我们说话，靠的是？', '语音识别', '光能', '风力', '重力', 'A', 5);
