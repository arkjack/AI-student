<template>
  <div class="k-page home" ref="root">
    <!-- ========== Hero 区（超宽场景背景，左侧留空供文字） ========== -->
    <section class="hero">
      <div class="hero-left">
        <div class="hero-greet">
          <h1>Hi，{{ user.nickname }}！</h1>
        </div>
        <p class="hero-sub">图像识别第 2 课已经更新，接着学吧。</p>
        <div class="hero-btns">
          <button class="k-btn" @click="$router.push('/student/courses')">
            <Play weight="bold" /> 继续学习
          </button>
          <button class="k-btn k-btn--orange" @click="$router.push('/student/ai?tab=quiz')">
            <Trophy weight="bold" /> 去闯关
          </button>
        </div>
      </div>
    </section>

    <!-- ========== 学习数据条（无卡片，大数字 + 分隔） ========== -->
    <!-- TODO: 学习统计暂无对应统计接口，目前为静态数字，待后端提供 /stats 后替换 -->
    <section class="stats-strip">
      <div class="strip-item">
        <b><span class="num" data-count="12">0</span><span class="unit">天</span></b>
        <span>连续学习</span>
      </div>
      <div class="strip-item">
        <b><span class="num" data-count="7.7" data-decimals="1">0</span><span class="unit">h</span></b>
        <span>累计学习</span>
      </div>
      <div class="strip-item">
        <b><span class="num" data-count="5">0</span><span class="unit">门</span></b>
        <span>完成课程</span>
      </div>
      <div class="strip-item">
        <b><span class="num" data-count="4">0</span><span class="unit">个</span></b>
        <span>我的作品</span>
      </div>
    </section>

    <!-- ========== 通知区 ========== -->
    <section class="notice-grid">
      <div class="k-card notice-card">
        <div class="card-head">
          <h3><Bell weight="bold" /> 平台公告</h3>
          <span class="more" @click="announceDialog = true">更多 ›</span>
        </div>
        <div v-for="n in announcements.slice(0, 2)" :key="n.id" class="notice-item">
          <span class="dot"></span>
          <span class="notice-title" :title="n.content">{{ n.title }}</span>
          <span class="k-tag" :class="tagClass(n.tag)">{{ n.tag }}</span>
        </div>
      </div>

      <div class="k-card notice-card">
        <div class="card-head">
          <h3><GraduationCap weight="bold" /> 班级通知</h3>
          <span class="more" @click="$router.push('/student/homework')">更多 ›</span>
        </div>
        <div v-for="n in classNotices" :key="n.id" class="notice-item">
          <span class="dot"></span>
          <span class="notice-title" :title="n.content">{{ n.title }}</span>
          <span class="notice-teacher">{{ n.teacher }}</span>
        </div>
      </div>
    </section>

    <!-- ========== 快捷入口 ========== -->
    <div class="k-section-head">
      <h2>快捷入口</h2>
    </div>
    <section class="quick-grid">
      <div class="quick-card q-writing" @click="$router.push('/student/ai?tab=writing')">
        <img v-if="writingOk" src="@/assets/images/ai-writing.jpg" class="quick-img" @error="writingOk = false" />
        <div class="quick-body">
          <h3>AI 创意写作</h3>
          <p>和 AI 一起写故事</p>
        </div>
      </div>
      <div class="quick-card q-quiz" @click="$router.push('/student/ai?tab=quiz')">
        <img v-if="quizOk" src="@/assets/images/ai-quiz.jpg" class="quick-img" @error="quizOk = false" />
        <div class="quick-body">
          <h3>知识闯关</h3>
          <p>挑战 AI 知识题库</p>
        </div>
      </div>
      <div class="quick-card q-lab" @click="$router.push('/student/lab')">
        <img v-if="labOk" src="@/assets/images/quick-lab.jpg" class="quick-img" @error="labOk = false" />
        <div v-else class="quick-emoji">
          <PuzzlePiece :size="34" weight="bold" />
        </div>
        <div class="quick-body">
          <h3>编程实验室</h3>
          <p>搭建你的积木作品</p>
        </div>
      </div>
      <div class="quick-card q-ask" @click="$router.push('/student/ai?tab=chat')">
        <img v-if="askOk" src="@/assets/images/quick-ask.jpg" class="quick-img" @error="askOk = false" />
        <div v-else class="quick-emoji">
          <ChatCircleDots :size="34" weight="bold" />
        </div>
        <div class="quick-body">
          <h3>智能答疑</h3>
          <p>想问什么都可以</p>
        </div>
      </div>
    </section>

    <!-- ========== 继续学习 ========== -->
    <div class="k-section-head">
      <h2>继续学习</h2>
      <span class="more" @click="$router.push('/student/courses')">查看全部课程 ›</span>
    </div>
    <section class="k-grid continue-grid">
      <CourseCard v-for="c in continueCourses" :key="c.id" :course="c" />
    </section>

    <!-- ========== 推荐课程 ========== -->
    <div class="k-section-head">
      <h2>为你推荐</h2>
      <span class="more" @click="$router.push('/student/courses')">更多 ›</span>
    </div>
    <section class="k-grid rec-grid">
      <CourseCard v-for="c in recommendCourses" :key="c.id" :course="c" />
    </section>

    <!-- ========== 今日成就（无卡：标签轻量排布） ========== -->
    <div class="k-section-head">
      <h2>我的成就</h2>
    </div>
    <section class="achie-list">
      <div v-for="(a, idx) in achievements" :key="a.name" class="achie-item" :class="{ locked: !a.got }" :style="{ animationDelay: (idx * 0.06) + 's' }">
        <div class="achie-emoji">{{ a.emoji }}</div>
        <b>{{ a.name }}</b>
        <span>{{ a.desc }}</span>
      </div>
    </section>

    <!-- ========== 公告完整列表弹窗 ========== -->
    <el-dialog v-model="announceDialog" title="平台公告" width="560px" align-center class="ann-dialog">
      <div class="ann-dialog-list">
        <div v-for="a in announcements" :key="a.id" class="ann-dialog-item">
          <div class="ann-dialog-head">
            <span class="k-tag" :class="tagClass(a.tag)">{{ a.tag }}</span>
            <span v-if="a.isTop" class="k-tag k-tag--orange">置顶</span>
            <span class="ann-dialog-time">{{ a.time }}</span>
          </div>
          <h4 class="ann-dialog-title">{{ a.title }}</h4>
          <p class="ann-dialog-content">{{ a.content }}</p>
        </div>
        <el-empty v-if="announcements.length === 0" description="暂无公告" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import CourseCard from '@/components/CourseCard.vue'
import request from '@/api/request'
import { gsap, ScrollTrigger, countUpGroup } from '@/utils/motion'
import { PhPlay as Play, PhTrophy as Trophy, PhBell as Bell, PhGraduationCap as GraduationCap, PhPuzzlePiece as PuzzlePiece, PhChatCircleDots as ChatCircleDots } from '@phosphor-icons/vue'

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_USER = {
  id: 1, username: 'xiaoming', nickname: '小明', role: 'student',
  class: '五年级(2)班', avatar: '🦊', level: 6, exp: 1860, expNext: 2000,
  streakDays: 12, studyMinutes: 462, completedCourses: 5, worksCount: 4, avgScore: 92
}
const FALLBACK_ANNOUNCEMENTS = [
  { id: 1, title: '新学期 AI 挑战赛开始啦！', content: '完成 3 个 AI 实验即可获得「未来科学家」徽章，还能赢取积分奖励哦～', time: '2026-09-01', tag: '活动' },
  { id: 2, title: '平台公告', content: '新增课程《认识机器学习》上线，欢迎大家学习！', time: '2026-08-28', tag: '公告' },
  { id: 3, title: '系统维护通知', content: '本周六 22:00-24:00 系统维护，请合理安排学习时间。', time: '2026-08-25', tag: '维护' }
]
const FALLBACK_CLASS_NOTICES = [
  { id: 1, title: '下周一交作业啦', content: '请大家记得完成《什么是人工智能？》的问答作业。', teacher: '秦老师', time: '2026-09-02' },
  { id: 2, title: '本周学习之星：小明', content: '小明本周完成 3 个 AI 实验，继续保持哦！', teacher: '秦老师', time: '2026-09-01' }
]
const FALLBACK_ACHIEVEMENTS = [
  { emoji: '🚀', name: '初来乍到', desc: '完成注册', got: true },
  { emoji: '📚', name: '学习达人', desc: '累计学习 5 小时', got: true },
  { emoji: '🧩', name: '积木大师', desc: '提交 3 个编程作品', got: true },
  { emoji: '✨', name: 'AI 探险家', desc: '完成 5 个 AI 实验', got: false },
  { emoji: '🏆', name: '闯关王者', desc: '单次闯关 10 题全对', got: false }
]
const FALLBACK_COURSES = [
  {
    id: 1, title: '什么是人工智能？', category: 'AI 入门', difficulty: 1,
    duration: 18, coverEmoji: '🤖', coverBg: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    summary: '和机器人小智一起认识 AI 世界的奇妙旅程！',
    progress: 100, completed: true, views: 1284, teacher: '秦老师'
  },
  {
    id: 2, title: '有趣的图像识别', category: 'AI 入门', difficulty: 2,
    duration: 25, coverEmoji: '👀', coverBg: 'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    summary: '为什么手机能认出你的脸？图像识别原理大揭秘。',
    progress: 60, completed: false, views: 986, teacher: '秦老师'
  },
  {
    id: 3, title: '方块编程第一课', category: '图形化编程', difficulty: 1,
    duration: 30, coverEmoji: '🧩', coverBg: 'linear-gradient(135deg,#2f5cd8,#163a9e)',
    summary: '拖一拖、拼一拼，用积木让角色动起来！',
    progress: 0, completed: false, views: 1523, teacher: '刘老师'
  },
  {
    id: 4, title: '认识机器学习', category: '机器学习', difficulty: 3,
    duration: 35, coverEmoji: '🧠', coverBg: 'linear-gradient(135deg,#f5a35c,#e06e1f)',
    summary: '机器是怎么学习的？像训练小狗一样训练 AI。',
    progress: 0, completed: false, views: 872, teacher: '秦老师'
  },
  {
    id: 5, title: '语音助手会说话', category: 'AI 与生活', difficulty: 2,
    duration: 20, coverEmoji: '🎙️', coverBg: 'linear-gradient(135deg,#ff8a3d,#e06e1f)',
    summary: '你好呀！看看语音助手是怎么听懂你的话。',
    progress: 0, completed: false, views: 764, teacher: '王老师'
  },
  {
    id: 6, title: 'AI 魔法画师', category: '趣味实验', difficulty: 2,
    duration: 22, coverEmoji: '🎨', coverBg: 'linear-gradient(135deg,#2f5cd8,#223a75)',
    summary: '输入一句话，AI 就能画出你心中的神奇画面！',
    progress: 0, completed: false, views: 1105, teacher: '刘老师'
  }
]

// 后端字段 → 页面字段 映射（CourseCard 所需）
const defaultCoverBg = (c) => {
  const grads = [
    'linear-gradient(135deg,#60a5fa,#3b82f6)',
    'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    'linear-gradient(135deg,#2f5cd8,#163a9e)',
    'linear-gradient(135deg,#f5a35c,#e06e1f)',
    'linear-gradient(135deg,#ff8a3d,#e06e1f)'
  ]
  return grads[(c.categoryId || c.id || 0) % grads.length]
}
const mapCourse = (c) => ({
  id: c.id,
  title: c.title,
  category: c.categoryName || c.category || '',
  categoryId: c.categoryId,
  difficulty: c.difficulty || 1,
  duration: c.durationMinutes || c.duration || 0,
  coverEmoji: c.coverEmoji,
  coverImage: c.coverImage,
  coverBg: c.coverBg || defaultCoverBg(c),
  summary: c.summary || '',
  videoUrl: c.videoUrl,
  teacher: c.teacher,
  views: c.views,
  status: c.status,
  createdAt: c.createdAt,
  progress: typeof c.progress === 'number' ? c.progress : 0,
  completed: !!c.completed
})

// 用户信息：优先 localStorage，回退 mock
const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
const user = ref({ ...FALLBACK_USER, ...storedUser })

const root = ref(null)
const writingOk = ref(true)
const quizOk = ref(true)
const labOk = ref(true)
const askOk = ref(true)

// 公告走 API；无班级通知接口 → 保持静态 mock
const announcements = ref(FALLBACK_ANNOUNCEMENTS)
const announceDialog = ref(false)
const route = useRoute()
const classNotices = ref(FALLBACK_CLASS_NOTICES)
// 无成就接口 → 保持静态 mock
const achievements = ref(FALLBACK_ACHIEVEMENTS)

// 课程列表：进度来自 /progress/my，未开始与进行中分开
const courseList = ref([])
const continueCourses = computed(() => courseList.value.filter(c => c.progress > 0 && !c.completed))
const recommendCourses = computed(() => courseList.value.filter(c => c.progress === 0).slice(0, 3))

const tagClass = (tag) => {
  if (tag === '活动') return 'k-tag--orange'
  if (tag === '维护') return 'k-tag--teal'
  return ''
}

const loadAnnouncements = async () => {
  try {
    const list = await request.get('/announcement/list')
    // 接口成功但为空时也回退到 mock，避免「平台公告」卡空白
    announcements.value = (list && list.length)
      ? list.map(n => ({ id: n.id, title: n.title, content: n.content, tag: n.tag, time: n.createdAt ? String(n.createdAt).slice(0, 10) : '', isTop: !!n.isTop }))
      : FALLBACK_ANNOUNCEMENTS
  } catch (e) {
    announcements.value = FALLBACK_ANNOUNCEMENTS
  }
}

const loadCourses = async () => {
  try {
    const [list, progressList] = await Promise.all([
      request.get('/course/list'),
      request.get('/progress/my')
    ])
    const proMap = {}
    ;(progressList || []).forEach(p => { proMap[p.courseId] = p })
    courseList.value = (list || []).map(c => {
      const p = proMap[c.id]
      return {
        ...mapCourse(c),
        progress: p ? (p.progress || 0) : 0,
        completed: p ? !!p.completed : false
      }
    })
  } catch (e) {
    courseList.value = FALLBACK_COURSES.map(mapCourse)
  }
}

/* ============ GSAP 动效编排 ============ */
let ctx

onMounted(() => {
  loadAnnouncements()
  loadCourses()
  if (route.query.announce === '1') announceDialog.value = true

  if (!root.value) return

  ctx = gsap.context(() => {
    const mm = gsap.matchMedia()

    mm.add('(prefers-reduced-motion: no-preference)', () => {
      // 主导时刻：Hero 内容依次浮现
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
      tl.from('.hero h1', { y: 26, autoAlpha: 0, duration: 0.55 })
        .from('.hero-sub', { y: 18, autoAlpha: 0, duration: 0.5 }, '-=0.38')
        .from('.hero-btns .k-btn', { y: 14, autoAlpha: 0, duration: 0.45, stagger: 0.09 }, '-=0.32')

      // 数据条：数字滚动 + 进场
      countUpGroup(root.value, { duration: 1.4, stagger: 0.12 })
      gsap.from('.stats-strip .strip-item', {
        y: 18,
        autoAlpha: 0,
        duration: 0.5,
        stagger: 0.08,
        ease: 'power2.out',
        delay: 0.3
      })

      // 滚动进场：课程卡片交错浮现（容器进入视口 85% 时播放一次，
      // paused timeline 保证滚动过程中动画有完整的可感知时长）
      gsap.utils.toArray('.continue-grid, .rec-grid').forEach((grid) => {
        const tl = gsap.timeline({ paused: true, defaults: { ease: 'power3.out' } })
        tl.from(grid.querySelectorAll('.course-card'), {
          y: 36,
          autoAlpha: 0,
          duration: 0.65,
          stagger: 0.15,
          clearProps: 'all'
        })
        ScrollTrigger.create({
          trigger: grid,
          start: 'top 85%',
          once: true,
          onEnter: () => tl.play()
        })
      })

      // 成就徽章轻快入场
      gsap.from('.achie-item', {
        y: 16,
        autoAlpha: 0,
        duration: 0.45,
        stagger: 0.06,
        ease: 'back.out(1.8)',
        delay: 0.5
      })
    })

    // 滚动触发位置在内容变化后校准一次
    requestAnimationFrame(() => ScrollTrigger.refresh())
  }, root.value)
})

onUnmounted(() => {
  ctx?.revert()
})
</script>

<style scoped>
.home {
  padding-top: 20px;
}

/* ---------- Hero（超宽场景背景：右侧插画 + 左侧留空遮罩） ---------- */
.hero {
  position: relative;
  border-radius: var(--radius-lg);
  background:
    linear-gradient(90deg, rgba(21, 40, 92, 0.92) 0%, rgba(21, 40, 92, 0.72) 34%, rgba(21, 40, 92, 0.25) 60%, rgba(21, 40, 92, 0.05) 78%),
    url('@/assets/images/hero-bg-wide.jpg') center / cover no-repeat;
  color: #fff;
  display: flex;
  align-items: center;
  padding: var(--space-7) var(--space-7);
  min-height: 300px;
  overflow: hidden;
  box-shadow: 0 10px 34px rgba(21, 40, 92, 0.28);
}

.hero-left {
  max-width: 520px;
}

.hero-greet h1 {
  margin: 0;
  font-size: 40px;
  color: #fff;
  text-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
}

.hero-sub {
  margin: 12px 0 var(--space-6);
  font-size: 16px;
  opacity: 0.92;
  max-width: 36ch;
}

.hero-btns {
  display: flex;
  gap: var(--space-3);
}

.hero-btns .k-btn {
  background: #fff;
  color: var(--brand);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.15);
  font-size: 15px;
}

.hero-btns .k-btn--orange {
  background: linear-gradient(135deg, #ffb45c, #ff8f3d);
  color: #fff;
}

/* ---------- 数据条（无卡片，大数字 + 分隔） ---------- */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: var(--space-6);
  padding: var(--space-5) var(--space-3);
  background: transparent;
}

.strip-item {
  text-align: center;
  position: relative;
}

.strip-item + .strip-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 1.5px;
  background: var(--line);
}

.strip-item b {
  display: block;
  font-size: 34px;
  line-height: 1.1;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.strip-item .unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-2);
  margin-left: 3px;
}

.strip-item span:last-child {
  font-size: 13px;
  color: var(--ink-3);
}

/* ---------- 通知区 ---------- */
.notice-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.notice-card {
  padding: 18px 22px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-head h3 {
  margin: 0;
  font-size: var(--fs-h3);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.card-head h3 svg {
  color: var(--brand);
}

.more {
  font-size: 13px;
  color: var(--ink-3);
  cursor: pointer;
}

.more:hover {
  color: var(--brand);
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 14px;
}

.notice-item:last-child {
  border-bottom: none;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--brand);
  flex-shrink: 0;
}

.notice-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink);
}

.notice-teacher {
  font-size: 12px;
  color: var(--ink-3);
}

/* ---------- 快捷入口 ---------- */
.quick-grid {
  display: grid;
  grid-template-columns: 1.4fr 1.4fr 1fr 1fr;
  gap: 20px;
}

.quick-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: transform 0.25s, box-shadow 0.25s;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.quick-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
}

.quick-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.q-writing {
  background: linear-gradient(135deg, #ffa35c, #ff8a3d);
}
.q-quiz {
  background: linear-gradient(135deg, #f58a3d, #e06e1f);
}
.q-lab {
  background: linear-gradient(135deg, #2f5cd8, #1e4fd8);
}
.q-ask {
  background: linear-gradient(135deg, #3f62b0, #2c4a8f);
}

.quick-emoji {
  position: absolute;
  top: 18px;
  left: 18px;
  color: #fff;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.18));
  transition: transform 0.22s ease;
}

.quick-card:hover .quick-emoji {
  transform: scale(1.12) rotate(-4deg);
}

.quick-body {
  position: relative;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.35));
  color: #fff;
  padding: 14px 18px;
}

.quick-body h3 {
  margin: 0;
  font-size: 18px;
}

.quick-body p {
  margin: 2px 0 0;
  font-size: 13px;
  opacity: 0.9;
}

/* ---------- 网格 ---------- */
.continue-grid {
  grid-template-columns: repeat(3, 1fr);
}

.rec-grid {
  grid-template-columns: repeat(3, 1fr);
}

/* ---------- 成就（无外层卡） ---------- */
.achie-list {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.achie-item {
  flex: 1;
  min-width: 130px;
  text-align: center;
  padding: var(--space-4) var(--space-3);
  border-radius: var(--radius-md);
  background: linear-gradient(160deg, #fff7e6, #fff1d6);
}

.achie-item b {
  display: block;
  font-size: 15px;
  margin-top: 6px;
  color: #b7791f;
}

.achie-item span {
  font-size: 12px;
  color: #b08a4f;
}

.achie-emoji {
  font-size: 34px;
}

.achie-item.locked {
  background: #f2f5fb;
  filter: grayscale(1);
  opacity: 0.75;
}

.achie-item.locked b { color: var(--ink-3); }
.achie-item.locked span { color: var(--ink-3); }

/* ---------- 公告弹窗 ---------- */
.ann-dialog-list {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;
}

.ann-dialog-item {
  padding: 14px 4px;
  border-bottom: 1px solid var(--line);
}

.ann-dialog-item:last-child { border-bottom: none; }

.ann-dialog-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.ann-dialog-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink-3);
}

.ann-dialog-title {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--ink);
}

.ann-dialog-content {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-2);
}

@media (max-width: 1000px) {
  .quick-grid { grid-template-columns: 1fr 1fr; }
  .notice-grid { grid-template-columns: 1fr; }
  .continue-grid, .rec-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-img { display: none; }
}
</style>
