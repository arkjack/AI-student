-- 演示数据：课程 / 公告 / 闯关题 / 班级学生
USE ai_enlighten;

INSERT INTO course (title, category_id, difficulty, duration_minutes, cover_emoji, cover_image, summary, video_url, teacher, views, status) VALUES
('什么是人工智能？', 1, 1, 18, '🤖', NULL, '和机器人小智一起认识 AI 世界的奇妙旅程！', 'douyin://play/ID-001', '秦老师', 1284, 1),
('有趣的图像识别', 1, 2, 25, '👀', NULL, '为什么手机能认出你的脸？图像识别原理大揭秘。', 'douyin://play/ID-002', '秦老师', 986, 1),
('方块编程第一课', 2, 1, 30, '🧩', NULL, '拖一拖、拼一拼，用积木让角色动起来！', 'douyin://play/ID-003', '刘老师', 1523, 1),
('认识机器学习', 3, 3, 35, '🧠', NULL, '机器是怎么学习的？像训练小狗一样训练 AI。', 'douyin://play/ID-004', '秦老师', 872, 1),
('语音助手会说话', 4, 2, 20, '🎙️', NULL, '看看语音助手是怎么听懂你的话。', 'douyin://play/ID-005', '王老师', 764, 1),
('AI 魔法画师', 5, 2, 22, '🎨', NULL, '输入一句话，AI 就能画出你心中的神奇画面！', 'douyin://play/ID-006', '刘老师', 1105, 1);

INSERT INTO announcement (title, content, tag, is_top) VALUES
('新学期 AI 挑战赛开始啦！', '完成 3 个 AI 实验即可获得「未来科学家」徽章。', '活动', 1),
('新增课程《认识机器学习》', '欢迎大家学习。', '公告', 0),
('系统维护通知', '本周六 22:00-24:00 系统维护。', '维护', 0);

INSERT INTO quiz_question (level, question, option_a, option_b, option_c, option_d, answer, sort) VALUES
('AI 基础', '人工智能的英文缩写是？', 'AI', 'AR', 'VR', 'IT', 'A', 1),
('AI 基础', '下面哪个是人工智能的应用？', '人脸识别', '电风扇', '自行车', '微波炉', 'A', 2),
('AI 基础', '机器人认识世界需要靠什么？', '传感器', '颜料', '橡皮', '胶水', 'A', 3),
('AI 基础', '机器学习中，机器学习的“原料”是什么？', '数据', '西瓜', '面粉', '颜料', 'A', 4),
('AI 基础', '语音助手能听懂我们说话，靠的是？', '语音识别', '光能', '风力', '重力', 'A', 5);

-- 班级学生关联（student01 加入教师01 的班级 id=1；再插入几名演示学生）
INSERT INTO class_student (class_id, student_id) SELECT 1, id FROM `user` WHERE username = 'student01';
