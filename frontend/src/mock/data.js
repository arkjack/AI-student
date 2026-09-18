/* ============================================================
   高保真原型 Mock 数据
   后续接入后端后，这些数据将替换为 API 返回
   ============================================================ */

export const currentUser = {
  id: 1,
  username: 'xiaoming',
  nickname: '小明',
  role: 'student',
  class: '五年级(2)班',
  avatar: '🦊',
  level: 6,
  exp: 1860,
  expNext: 2000,
  streakDays: 12,
  studyMinutes: 462,
  completedCourses: 5,
  worksCount: 4,
  avgScore: 92
}

export const announcements = [
  { id: 1, title: '新学期 AI 挑战赛开始啦！', content: '完成 3 个 AI 实验即可获得「未来科学家」徽章，还能赢取积分奖励哦～', time: '2026-09-01', tag: '活动' },
  { id: 2, title: '平台公告', content: '新增课程《认识机器学习》上线，欢迎大家学习！', time: '2026-08-28', tag: '公告' },
  { id: 3, title: '系统维护通知', content: '本周六 22:00-24:00 系统维护，请合理安排学习时间。', time: '2026-08-25', tag: '维护' }
]

export const courseCategories = [
  { id: 0, name: '全部', emoji: '🌈' },
  { id: 1, name: 'AI 入门', emoji: '🚀' },
  { id: 2, name: '图形化编程', emoji: '🧩' },
  { id: 3, name: '机器学习', emoji: '🤖' },
  { id: 4, name: 'AI 与生活', emoji: '🏠' },
  { id: 5, name: '趣味实验', emoji: '🧪' }
]

export const courses = [
  {
    id: 1, title: '什么是人工智能？', category: 'AI 入门', difficulty: 1,
    duration: 18, coverEmoji: '🤖', coverBg: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    summary: '和机器人小智一起认识 AI 世界的奇妙旅程！',
    progress: 100, completed: true, views: 1284, teacher: '秦老师',
    videoUrl: 'douyin://play/视频ID-001'
  },
  {
    id: 2, title: '有趣的图像识别', category: 'AI 入门', difficulty: 2,
    duration: 25, coverEmoji: '👀', coverBg: 'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    summary: '为什么手机能认出你的脸？图像识别原理大揭秘。',
    progress: 60, completed: false, views: 986, teacher: '秦老师',
    videoUrl: 'douyin://play/视频ID-002'
  },
  {
    id: 3, title: '方块编程第一课', category: '图形化编程', difficulty: 1,
    duration: 30, coverEmoji: '🧩', coverBg: 'linear-gradient(135deg,#2f5cd8,#163a9e)',
    summary: '拖一拖、拼一拼，用积木让角色动起来！',
    progress: 0, completed: false, views: 1523, teacher: '刘老师',
    videoUrl: 'douyin://play/视频ID-003'
  },
  {
    id: 4, title: '认识机器学习', category: '机器学习', difficulty: 3,
    duration: 35, coverEmoji: '🧠', coverBg: 'linear-gradient(135deg,#f5a35c,#e06e1f)',
    summary: '机器是怎么学习的？像训练小狗一样训练 AI。',
    progress: 0, completed: false, views: 872, teacher: '秦老师',
    videoUrl: 'douyin://play/视频ID-004'
  },
  {
    id: 5, title: '语音助手会说话', category: 'AI 与生活', difficulty: 2,
    duration: 20, coverEmoji: '🎙️', coverBg: 'linear-gradient(135deg,#ff8a3d,#e06e1f)',
    summary: '你好呀！看看语音助手是怎么听懂你的话。',
    progress: 0, completed: false, views: 764, teacher: '王老师',
    videoUrl: 'douyin://play/视频ID-005'
  },
  {
    id: 6, title: 'AI 魔法画师', category: '趣味实验', difficulty: 2,
    duration: 22, coverEmoji: '🎨', coverBg: 'linear-gradient(135deg,#2f5cd8,#223a75)',
    summary: '输入一句话，AI 就能画出你心中的神奇画面！',
    progress: 0, completed: false, views: 1105, teacher: '刘老师',
    videoUrl: 'douyin://play/视频ID-006'
  }
]

export const blocklyTemplates = [
  { id: 1, name: '小猫咪动起来', desc: '让小猫走到屏幕中央', level: '入门', code: '小猫说: 你好!' },
  { id: 2, name: '会算数的机器人', desc: '机器人帮你算加减法', level: '入门', code: '机器人的数学题' },
  { id: 3, name: '小星星循环舞', desc: '用循环画出一片星空', level: '进阶', code: '循环画星星' },
  { id: 4, name: '自动巡逻车', desc: '条件判断：碰到墙就转弯', level: '进阶', code: '自动巡逻车' }
]

export const blocklyProjects = [
  { id: 1, name: '小猫咪动起来', status: '已提交', score: 95, feedback: '写得很棒！可以尝试让小猫转个圈～', time: '2026-08-30', submit: true },
  { id: 2, name: '会算数的机器人', status: '已提交', score: 88, feedback: '思路清晰，注意积木的连接顺序哦。', time: '2026-08-26', submit: true },
  { id: 3, name: '我的第一个作品', status: '草稿', score: null, feedback: null, time: '2026-08-20', submit: false }
]

export const assignments = [
  {
    id: 1, title: '观看《什么是人工智能？》并回答问题', type: '课程', typeEmoji: '📖',
    content: '观看本节课程后，请回答：人工智能能帮我们做哪些事情？请列举 3 个例子。',
    deadline: '2026-09-08 20:00', status: '待提交', score: null, feedback: null,
    submitAt: null
  },
  {
    id: 2, title: '完成积木作品：小星星循环舞', type: '编程', typeEmoji: '🧩',
    content: '使用循环积木完成星空绘制作品并提交，注意循环次数设置。',
    deadline: '2026-09-12 20:00', status: '待提交', score: null, feedback: null,
    submitAt: null
  },
  {
    id: 3, title: 'AI 创意写作：我的未来学校', type: 'AI 实验', typeEmoji: '✨',
    content: '用 AI 创意写作实验室，写一篇 200 字左右的《我的未来学校》，主题风格自定。',
    deadline: '2026-09-05 20:00', status: '已批改', score: 92,
    feedback: '想象力丰富！如果用上比喻句会更好哦～', submitAt: '2026-09-03 18:22'
  },
  {
    id: 4, title: '知识闯关：AI 基础 10 题', type: 'AI 实验', typeEmoji: '🏆',
    content: '完成 AI 知识闯关实验室的「AI 基础」关卡，并截图保存成绩。',
    deadline: '2026-08-30 20:00', status: '已批改', score: 100,
    feedback: '满分！太棒啦！', submitAt: '2026-08-29 19:05'
  }
]

export const quizBank = [
  { q: '人工智能的英文缩写是？', options: ['AI', 'AR', 'VR', 'IT'], answer: 0 },
  { q: '下面哪个是人工智能的应用？', options: ['人脸识别', '电风扇', '自行车', '微波炉'], answer: 0 },
  { q: '机器人认识世界需要靠什么？', options: ['传感器', '颜料', '橡皮', '胶水'], answer: 0 },
  { q: '机器学习中，机器学习的“原料”是什么？', options: ['数据', '西瓜', '面粉', '颜料'], answer: 0 },
  { q: '语音助手能听懂我们说话，靠的是？', options: ['语音识别', '光能', '风力', '重力'], answer: 0 }
]

export const qaHistory = [
  { q: 'AI 会自己思考吗？', a: '现在的 AI 更多是在“模仿”人类的思考方式，它们通过大量数据学习规律，还不能像人一样真正地思考和感受哦！', time: '昨天' },
  { q: '我以后能学会编程吗？', a: '当然可以！编程就像搭积木，从最简单的积木块开始，一步一步来，你一定能学会！', time: '3 天前' }
]

export const writingRecords = [
  { id: 1, title: '如果我是风', topic: '风', style: '童话风', content: '如果我是风，我要去森林里和树叶捉迷藏……', status: '已提交', score: 92, time: '2026-08-28' },
  { id: 2, title: '会飞的书包', topic: '书包', style: '科幻风', content: '我的书包上有两个小翅膀，每天放学它带着我飞过操场……', status: '草稿', score: null, time: '2026-08-31' }
]

export const statsData = {
  weekMinutes: [35, 58, 42, 76, 90, 110, 51], // 周一~周日
  weekLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  courseRate: 83, // 课程完成率
  categoryMinutes: [
    { name: 'AI 科普课程', value: 260 },
    { name: '图形化编程', value: 120 },
    { name: 'AI 交互实验', value: 82 }
  ],
  scores: [85, 88, 92, 90, 95, 100], // 最近作业得分
  scoreLabels: ['作业1', '作业2', '闯关', '写作', '编程', '闯关']
}

export const achievements = [
  { emoji: '🚀', name: '初来乍到', desc: '完成注册', got: true },
  { emoji: '📚', name: '学习达人', desc: '累计学习 5 小时', got: true },
  { emoji: '🧩', name: '积木大师', desc: '提交 3 个编程作品', got: true },
  { emoji: '✨', name: 'AI 探险家', desc: '完成 5 个 AI 实验', got: false },
  { emoji: '🏆', name: '闯关王者', desc: '单次闯关 10 题全对', got: false }
]

export const classNotices = [
  { id: 1, title: '下周一交作业啦', content: '请大家记得完成《什么是人工智能？》的问答作业。', teacher: '秦老师', time: '2026-09-02' },
  { id: 2, title: '本周学习之星：小明', content: '小明本周完成 3 个 AI 实验，继续保持哦！', teacher: '秦老师', time: '2026-09-01' }
]
