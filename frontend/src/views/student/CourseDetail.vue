<template>
  <div class="k-page" v-if="course">
    <div class="crumb" @click="$router.back()">‹ 返回课程中心</div>

    <!-- 课程头部 -->
    <section class="head-card k-card">
      <div class="cover" :style="{ background: course.coverBg }">
        <img v-if="coverOk" :src="coverSrc" class="cover-img" @error="coverOk = false" />
        <span v-else class="cover-emoji">{{ course.coverEmoji }}</span>
      </div>
      <div class="info">
        <div class="tags">
          <span class="k-tag">{{ course.category }}</span>
          <span class="k-tag k-tag--orange" v-if="course.difficulty === 1">入门</span>
          <span class="k-tag k-tag--teal" v-if="course.difficulty === 2">进阶</span>
          <span class="k-tag k-tag--orange" v-if="course.difficulty === 3">挑战</span>
        </div>
        <h1>{{ course.title }}</h1>
        <p class="summary">{{ course.summary }}</p>
        <div class="meta">
          <span class="k-stars">★★★★★</span>
          <span class="meta-item"><Timer weight="bold" /> {{ course.duration }} 分钟</span>
          <span class="meta-item"><User weight="bold" /> {{ course.teacher }}</span>
          <span class="meta-item"><ChartLineUp weight="bold" /> {{ course.views }} 人在学</span>
        </div>
        <div class="progress-line">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ transform: 'scaleX(' + (course.progress / 100) + ')' }"></div>
          </div>
          <span>{{ course.completed ? '已完成' : `已学习 ${course.progress}%` }}</span>
        </div>
        <button class="k-btn play-btn" @click="togglePlay">
          <component :is="course.completed ? ArrowsClockwise : playing ? Pause : Play" weight="bold" />
          {{ course.completed ? '再看一遍' : playing ? '暂停学习' : '开始学习' }}
        </button>
      </div>
    </section>

    <!-- 视频播放区 -->
    <section class="video-card k-card">
      <div class="video-head">
        <h3><Video weight="bold" /> 课程视频</h3>
        <a class="k-tag k-tag--orange douyin-link" :href="DOUYIN_URL" target="_blank" rel="noopener">去抖音观看原视频 ↗</a>
      </div>

      <div class="player">
        <video
          v-if="playableUrl"
          ref="videoEl"
          class="player-video"
          :src="playableUrl"
          controls
          playsinline
          @loadedmetadata="onLoaded"
          @timeupdate="onTimeUpdate"
          @ended="onEnded"
          @play="onPlay"
          @pause="onPause"
        ></video>
        <div v-else class="player-no-video">
          <Video :size="40" weight="fill" color="#94a3b8" />
          <p>该课程暂无站内视频</p>
          <a v-if="course.videoUrl" :href="course.videoUrl" target="_blank" rel="noopener">前往观看原视频 ↗</a>
        </div>
      </div>

      <div class="watch-progress">
        <div class="bar">
          <div class="fill" :style="{ transform: 'scaleX(' + (course.progress / 100) + ')' }"></div>
        </div>
        <span class="pct">{{ course.completed ? '已学完 100%' : '已学习 ' + course.progress + '%' }}</span>
      </div>
      <p class="watch-tip">观看视频自动记录学习进度，完整播放到结尾后进度达 100%，教师端对应任务即显示完成。</p>
    </section>

    <!-- 课程大纲 -->
    <section class="k-card outline-card">
      <h3><ListDashes weight="bold" /> 课程大纲</h3>
      <div v-for="(s, i) in outline" :key="i" class="outline-item" :class="{ done: i < doneCount }">
        <span class="idx">{{ i + 1 }}</span>
        <span class="txt">{{ s }}</span>
        <span class="st">
          <CheckCircle v-if="i < doneCount" :size="16" weight="fill" color="#2ecc71" />
          <LockKey v-else :size="16" weight="fill" color="#c9d8f2" />
        </span>
      </div>
    </section>

    <!-- 推荐 -->
    <div class="k-section-head">
      <h2>猜你喜欢</h2>
    </div>
    <section class="k-grid rec-grid">
      <CourseCard v-for="c in related" :key="c.id" :course="c" :show-progress="false" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import CourseCard from '@/components/CourseCard.vue'
import request from '@/api/request'
import { PhTimer as Timer, PhUser as User, PhChartLineUp as ChartLineUp, PhArrowsClockwise as ArrowsClockwise, PhPause as Pause, PhPlay as Play, PhVideo as Video, PhListDashes as ListDashes, PhCheckCircle as CheckCircle, PhLockKey as LockKey } from '@phosphor-icons/vue'

const route = useRoute()

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_COURSES = [
  {
    id: 1, title: '什么是人工智能？', category: 'AI 入门', categoryId: 1, difficulty: 1,
    durationMinutes: 18, coverEmoji: '🤖', coverBg: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    summary: '和机器人小智一起认识 AI 世界的奇妙旅程！',
    videoUrl: '/videos/what-is-ai.mp4',
    progress: 100, completed: true, views: 1284, teacher: '秦老师'
  },
  {
    id: 2, title: '有趣的图像识别', category: 'AI 入门', categoryId: 1, difficulty: 2,
    durationMinutes: 25, coverEmoji: '👀', coverBg: 'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    summary: '为什么手机能认出你的脸？图像识别原理大揭秘。',
    progress: 60, completed: false, views: 986, teacher: '秦老师'
  },
  {
    id: 3, title: '方块编程第一课', category: '图形化编程', categoryId: 2, difficulty: 1,
    durationMinutes: 30, coverEmoji: '🧩', coverBg: 'linear-gradient(135deg,#2f5cd8,#163a9e)',
    summary: '拖一拖、拼一拼，用积木让角色动起来！',
    progress: 0, completed: false, views: 1523, teacher: '刘老师'
  },
  {
    id: 4, title: '认识机器学习', category: '机器学习', categoryId: 3, difficulty: 3,
    durationMinutes: 35, coverEmoji: '🧠', coverBg: 'linear-gradient(135deg,#f5a35c,#e06e1f)',
    summary: '机器是怎么学习的？像训练小狗一样训练 AI。',
    progress: 0, completed: false, views: 872, teacher: '秦老师'
  },
  {
    id: 5, title: '语音助手会说话', category: 'AI 与生活', categoryId: 4, difficulty: 2,
    durationMinutes: 20, coverEmoji: '🎙️', coverBg: 'linear-gradient(135deg,#ff8a3d,#e06e1f)',
    summary: '你好呀！看看语音助手是怎么听懂你的话。',
    progress: 0, completed: false, views: 764, teacher: '王老师'
  },
  {
    id: 6, title: 'AI 魔法画师', category: '趣味实验', categoryId: 5, difficulty: 2,
    durationMinutes: 22, coverEmoji: '🎨', coverBg: 'linear-gradient(135deg,#2f5cd8,#223a75)',
    summary: '输入一句话，AI 就能画出你心中的神奇画面！',
    progress: 0, completed: false, views: 1105, teacher: '刘老师'
  }
]

// 后端字段 → 页面字段 映射
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

// 先用 fallback 兜底，保证页面不空白；随后从接口加载
const course = ref(mapCourse(FALLBACK_COURSES.find(c => c.id === Number(route.params.id)) || FALLBACK_COURSES[0]))
const coverOk = ref(true)
const playing = ref(false)
const videoEl = ref(null)
const related = ref([])

// 抖音原视频链接（已下载 mp4 到 public/videos 供站内播放，此处保留原片入口）
const DOUYIN_URL = 'https://www.douyin.com/user/self?modal_id=7640460138452782336'
// 可播放的视频源：仅接受站内路径或 http(s) 地址，douyin:// 等协议不可播放
const playableUrl = computed(() => {
  const u = course.value?.videoUrl
  if (!u) return ''
  return (u.startsWith('/') || u.startsWith('http')) ? u : ''
})

const covers = import.meta.glob('/src/assets/images/course-*.jpg', { eager: true, import: 'default' })
const coverSrc = computed(() => course.value ? (covers[`/src/assets/images/course-${course.value.id}.jpg`] || '') : '')

const outline = [
  'AI 是什么？为什么神奇？',
  '身边的 AI 发现之旅',
  'AI 是怎么学习的？',
  '有趣的 AI 小实验',
  '总结与课后小任务'
]

const doneCount = computed(() => course.value ? Math.round(course.value.progress / 100 * outline.length) : 0)

const loadCourse = async () => {
  const id = Number(route.params.id)
  try {
    const c = await request.get(`/course/${id}`)
    course.value = mapCourse(c)
  } catch (e) {
    course.value = mapCourse(FALLBACK_COURSES.find(x => x.id === id) || FALLBACK_COURSES[0])
  }
  loadRelated(course.value.id)
}

const loadRelated = async (curId) => {
  try {
    const list = await request.get('/course/list')
    related.value = (list || []).filter(c => c.id !== curId).slice(0, 3).map(mapCourse)
  } catch (e) {
    related.value = FALLBACK_COURSES.filter(c => c.id !== curId).slice(0, 3).map(mapCourse)
  }
}

// 视频进度上报节流
let lastReportedPct = 0

const onLoaded = () => {
  // 恢复上次学习位置
  const v = videoEl.value
  if (v && v.duration && course.value.progress > 0 && course.value.progress < 100) {
    v.currentTime = (course.value.progress / 100) * v.duration
  }
}

const onTimeUpdate = () => {
  const v = videoEl.value
  if (!v || !v.duration) return
  const pct = Math.floor((v.currentTime / v.duration) * 100)
  if (pct > lastReportedPct && (pct - lastReportedPct >= 5 || pct >= 100)) {
    lastReportedPct = pct
    course.value.progress = pct
    if (pct >= 100) course.value.completed = true
    saveProgress(pct, Math.floor(v.currentTime))
  }
}

const onEnded = () => {
  const v = videoEl.value
  lastReportedPct = 100
  course.value.progress = 100
  course.value.completed = true
  saveProgress(100, Math.floor(v?.duration || 0))
  ElMessage.success('🎉 课程学习完成！学习进度已达 100%')
}

const onPlay = () => { playing.value = true }
const onPause = () => { playing.value = false }

const saveProgress = async (progress, watchSeconds) => {
  const pid = course.value?.id
  if (!pid) return
  try {
    await request.post('/progress/save', { courseId: pid, progress, watchSeconds })
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}

const togglePlay = () => {
  const v = videoEl.value
  if (!v) return
  if (v.paused) v.play()
  else v.pause()
  document.querySelector('.video-card')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
  loadCourse()
})
</script>

<style scoped>
.crumb {
  font-size: 14px;
  color: var(--ink-3);
  cursor: pointer;
  margin-bottom: 14px;
}

.crumb:hover {
  color: var(--brand);
}

.head-card {
  display: flex;
  gap: 26px;
  padding: 24px;
}

.cover {
  width: 320px;
  height: 220px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-emoji {
  font-size: 76px;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.18));
}

.info {
  flex: 1;
}

.tags {
  display: flex;
  gap: 8px;
}

.info h1 {
  margin: 10px 0 6px;
  font-size: 28px;
}

.summary {
  color: var(--ink-2);
  margin: 0 0 12px;
}

.meta {
  display: flex;
  gap: 16px;
  color: var(--ink-3);
  font-size: 13px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.meta-item svg {
  color: var(--brand);
  opacity: 0.75;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  max-width: 420px;
}

.progress-bar {
  flex: 1;
  height: 10px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--brand-grad);
  border-radius: 999px;
  transform-origin: left;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.progress-line span {
  font-size: 13px;
  color: var(--brand);
  font-weight: 600;
  white-space: nowrap;
}

.play-btn {
  font-size: 16px;
  padding: 12px 34px;
}

/* 视频区 */
.video-card {
  margin-top: 20px;
}

.video-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.video-head h3 {
  margin: 0;
  font-size: 18px;
}

.player {
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #0f172a;
}

.douyin-link {
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.18s;
}
.douyin-link:hover { opacity: 0.85; }

.player-video {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: contain;
  background: #0f172a;
}

.player-no-video {
  aspect-ratio: 16 / 9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  background: linear-gradient(135deg, #1e293b, #0f172a);
}
.player-no-video p { margin: 0; }
.player-no-video a {
  color: var(--accent, #ff8a3d);
  font-size: 13px;
  text-decoration: none;
}

.watch-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
.watch-progress .bar {
  flex: 1;
  height: 10px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}
.watch-progress .fill {
  height: 100%;
  background: var(--brand-grad);
  border-radius: 999px;
  transform-origin: left;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
.watch-progress .pct {
  font-size: 13px;
  color: var(--brand);
  font-weight: 600;
  white-space: nowrap;
}

.watch-tip {
  margin: 8px 0 0;
  font-size: 12.5px;
  color: var(--ink-3);
}

/* 大纲 */
.outline-card {
  margin-top: 20px;
}

.outline-card h3 {
  margin: 0 0 14px;
  font-size: 18px;
}

.outline-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.outline-item:hover {
  background: var(--brand-grad-soft);
}

.outline-item + .outline-item {
  margin-top: 6px;
}

.idx {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.outline-item.done .idx {
  background: var(--brand-grad);
  color: #fff;
}

.txt {
  flex: 1;
  font-size: 14px;
}

.rec-grid {
  grid-template-columns: repeat(3, 1fr);
}

@media (max-width: 900px) {
  .head-card { flex-direction: column; }
  .cover { width: 100%; }
}
</style>
