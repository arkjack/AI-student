<template>
  <div class="k-page t-workbench">
    <div class="page-head">
      <h1 class="k-h1-icon"><MoonStars :size="24" weight="bold" /> 工作台</h1>
      <p class="sub">早上好，{{ user.nickname }}！有 {{ stats.todo }} 份作业等待批改。</p>
    </div>

    <!-- 概览 -->
    <section class="ov-strip">
      <div class="ov-item">
        <span class="ov-icon orange"><NotePencil :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ stats.todo }}</b><span>待批改作业</span></div>
      </div>
      <div class="ov-item">
        <span class="ov-icon blue"><Users :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ stats.classes }}</b><span>我的班级</span></div>
      </div>
      <div class="ov-item">
        <span class="ov-icon green"><Student :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ stats.students }}</b><span>班级学生</span></div>
      </div>
      <div class="ov-item">
        <span class="ov-icon purple"><PushPin :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ stats.tasks }}</b><span>进行中任务</span></div>
      </div>
    </section>

    <div class="t-grid">
      <!-- 待批改 -->
      <section class="k-card">
        <div class="card-head">
          <h3><NotePencil :size="17" weight="bold" /> 待批改（{{ stats.todo }}）</h3>
          <span class="more">查看全部 ›</span>
        </div>
        <div class="r-list">
          <div v-for="r in reviews" :key="r.id" class="r-item">
            <span class="r-avatar">{{ r.avatar }}</span>
            <div class="r-info">
              <b>{{ r.name }} · {{ r.typeLabel }}</b>
              <span>{{ r.task }} · 提交于 {{ r.time }}</span>
            </div>
            <el-button size="small" type="primary" round @click="$router.push('/teacher/review')">批改</el-button>
          </div>
        </div>
      </section>

      <!-- 快捷操作 -->
      <section class="k-card">
        <div class="card-head">
          <h3><Lightning :size="17" weight="bold" /> 快捷操作</h3>
        </div>
        <div class="q-actions">
          <button class="q-btn" @click="$router.push('/teacher/tasks')">
            <PushPin :size="18" weight="bold" />
            <span>布置任务</span>
          </button>
          <button class="q-btn" @click="$router.push('/teacher/classes')">
            <UploadSimple :size="18" weight="bold" />
            <span>导入学生</span>
          </button>
          <button class="q-btn" @click="$router.push('/teacher/qa')">
            <ChatCircleDots :size="18" weight="bold" />
            <span>班级答疑</span>
          </button>
          <button class="q-btn" @click="$router.push('/teacher/progress')">
            <ChartLineUp :size="18" weight="bold" />
            <span>进度追踪</span>
          </button>
        </div>

        <div class="recent-t">
          <h4>最近布置</h4>
          <div v-for="t in recentTasks" :key="t.id" class="rt-item">
            <span class="rt-emoji">{{ t.emoji }}</span>
            <div class="rt-info">
              <b>{{ t.title }}</b>
              <span>{{ t.className }} · 截止 {{ t.deadline }}</span>
            </div>
            <span class="rt-status" :class="{ done: t.status === '已收齐' }">{{ t.status }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 平台公告（与管理端发布、学生端首页同源） -->
    <div class="k-section-head">
      <h2>平台公告</h2>
    </div>
    <section class="k-card ann-card">
      <div class="ann-list">
        <div v-for="a in announcements" :key="a.id" class="ann-item">
          <span class="ann-tag" :class="{ top: a.isTop }">{{ a.tag }}</span>
          <span class="ann-title">{{ a.title }}</span>
          <span class="ann-content">{{ a.content }}</span>
          <span class="ann-time">{{ a.time }}</span>
        </div>
        <div v-if="announcements.length === 0" class="ann-empty">暂无公告</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { onMounted } from 'vue'
import request from '@/api/request'
import { PhMoonStars as MoonStars, PhNotePencil as NotePencil, PhUsers as Users, PhStudent as Student, PhPushPin as PushPin, PhLightning as Lightning, PhUploadSimple as UploadSimple, PhChatCircleDots as ChatCircleDots, PhChartLineUp as ChartLineUp } from '@phosphor-icons/vue'

// 从 localStorage.userInfo 读取当前用户（JSON.parse，防御式取值）
function readUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (e) {
    return {}
  }
}
const info = readUserInfo()
const user = ref({
  nickname: info.nickname || info.realName || info.username || '秦老师',
  avatar: info.avatar || '👩‍🏫',
  roleLabel: '教师'
})

// ===== FALLBACK 数据（后端加载失败时使用） =====
const FALLBACK_STATS = { todo: 8, classes: 3, students: 126, tasks: 5 }
const FALLBACK_REVIEWS = [
  { id: 1, name: '小明', avatar: '🦊', typeLabel: '编程作品', task: '小星星循环舞', time: '今天 09:12' },
  { id: 2, name: '朵朵', avatar: '🐰', typeLabel: 'AI 写作', task: '我的未来学校', time: '今天 08:47' },
  { id: 3, name: '小航', avatar: '🐼', typeLabel: '课程问答', task: '什么是人工智能', time: '昨天 19:03' },
  { id: 4, name: '糖糖', avatar: '🐱', typeLabel: '闯关记录', task: 'AI 基础 10 题', time: '昨天 17:40' }
]
const FALLBACK_RECENT_TASKS = [
  { id: 1, title: '观看《什么是人工智能？》', className: '五年级(2)班', deadline: '09-08', emoji: '📖', status: '已收齐' },
  { id: 2, title: '积木作品：小星星循环舞', className: '五年级(2)班', deadline: '09-12', emoji: '🧩', status: '进行中' },
  { id: 3, title: 'AI 写作：我的未来学校', className: '四年级(1)班', deadline: '09-09', emoji: '✨', status: '进行中' }
]
const FALLBACK_ANNOUNCEMENTS = [
  { id: 1, tag: '活动', title: '新学期 AI 挑战赛开始啦！', content: '完成 3 个 AI 实验即可获得徽章。', time: '09-01', isTop: 1 },
  { id: 2, tag: '公告', title: '新增课程《认识机器学习》', content: '欢迎大家学习。', time: '08-28', isTop: 0 }
]
const announcements = ref([...FALLBACK_ANNOUNCEMENTS])

const stats = ref({ ...FALLBACK_STATS })
const reviews = ref([...FALLBACK_REVIEWS])
const recentTasks = ref([...FALLBACK_RECENT_TASKS])

const TYPE_EMOJI = { 1: '📖', 2: '🧩', 3: '✨', 4: '🏆' }

function mapAssignment(a) {
  return {
    id: a.id,
    title: a.title,
    className: a.className || '未发布班级',
    deadline: (a.deadline || '').slice(0, 10) || '无',
    emoji: TYPE_EMOJI[Number(a.type)] || '📖',
    status: Number(a.status) === 1 ? '进行中' : '已收齐'
  }
}

function mapQa(q) {
  const question = q.question || ''
  return {
    id: q.id,
    name: q.studentName || '学生',
    avatar: '🎓',
    typeLabel: '答疑问题',
    task: question.length > 18 ? question.slice(0, 18) + '…' : (question || '（无标题）'),
    time: q.createdAt || ''
  }
}

async function loadWorkbench() {
  try {
    const [pending, classes, assignments, qa, anns] = await Promise.all([
      request.get('/teacher/review/pending-count').catch(() => null),
      request.get('/teacher/classes').catch(() => []),
      request.get('/teacher/assignments').catch(() => []),
      request.get('/teacher/qa/list').catch(() => []),
      request.get('/announcement/list').catch(() => [])
    ])

    // 平台公告（与管理端发布、学生端首页同源）
    if (Array.isArray(anns) && anns.length) {
      announcements.value = anns.map((a) => ({
        id: a.id,
        tag: a.tag || '公告',
        title: a.title || '',
        content: a.content || '',
        time: (a.createdAt || '').slice(0, 10),
        isTop: Number(a.isTop) === 1
      }))
    }

    const s = { ...FALLBACK_STATS }
    if (typeof pending === 'number') s.todo = pending
    if (Array.isArray(classes) && classes.length) s.classes = classes.length
    if (Array.isArray(assignments) && assignments.length) {
      s.tasks = assignments.filter(a => Number(a.status) === 1).length
    }
    stats.value = s

    // 班级学生数：汇总各班级学生
    if (Array.isArray(classes) && classes.length) {
      const lists = await Promise.all(
        classes.map(c => request.get(`/teacher/classes/${c.id}/students`).catch(() => []))
      )
      stats.value.students = lists.reduce((sum, list) => sum + (Array.isArray(list) ? list.length : 0), 0)
    }

    if (Array.isArray(assignments) && assignments.length) {
      recentTasks.value = assignments.slice(0, 3).map(mapAssignment)
    }
    if (Array.isArray(qa) && qa.length) {
      reviews.value = qa.slice(0, 3).map(mapQa)
    }
  } catch (e) {
    // 数据加载失败，保持 FALLBACK
  }
}

onMounted(loadWorkbench)
</script>

<style scoped>
.t-workbench .sub { color: var(--ink-2); margin: 4px 0 0; }

.ov-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.ov-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
}

.ov-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
}

.ov-icon.orange { background: var(--accent-soft); color: var(--accent-deep); }
.ov-icon.blue { background: var(--brand-soft); color: var(--brand); }
.ov-icon.green { background: #e4f4ec; color: #1f7a52; }
.ov-icon.purple { background: var(--brand-soft); color: var(--brand); }

.ov-num b {
  display: block;
  font-size: 24px;
  line-height: 1.1;
}

.ov-num span {
  font-size: 12.5px;
  color: var(--ink-3);
}

.t-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: var(--space-5);
}

/* 平台公告 */
.ann-card {
  padding: var(--space-4) var(--space-5);
}

.ann-list {
  display: flex;
  flex-direction: column;
}

.ann-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 2px;
  border-bottom: 1px dashed var(--line);
  font-size: 14px;
}

.ann-item:last-child {
  border-bottom: none;
}

.ann-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-deep);
  background: var(--brand-soft);
  border-radius: 999px;
  padding: 2px 10px;
  flex-shrink: 0;
}

.ann-tag.top {
  color: var(--accent-deep);
  background: var(--accent-soft);
}

.ann-title {
  font-weight: 600;
  color: var(--ink);
  flex-shrink: 0;
}

.ann-content {
  flex: 1;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ann-time {
  font-size: 12.5px;
  color: var(--ink-3);
  flex-shrink: 0;
}

.ann-empty {
  color: var(--ink-3);
  text-align: center;
  padding: 10px 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-head h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16.5px;
}

.card-head h3 svg { color: var(--brand); }

.more {
  font-size: 13px;
  color: var(--ink-3);
  cursor: pointer;
}

.r-list { display: flex; flex-direction: column; }

.r-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid var(--line);
}

.r-item:last-child { border-bottom: none; }

.r-avatar { font-size: 24px; }

.r-info { flex: 1; display: flex; flex-direction: column; line-height: 1.4; }
.r-info b { font-size: 14px; }
.r-info span { font-size: 12px; color: var(--ink-3); }

.q-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.q-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  background: var(--surface-soft);
  border-radius: var(--radius-md);
  padding: 13px 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  cursor: pointer;
  transition: all 0.18s;
  font-family: inherit;
}

.q-btn:hover {
  border-color: var(--brand);
  background: var(--brand-grad-soft);
  color: var(--brand);
}

.q-btn svg { color: var(--brand); }

.recent-t {
  margin-top: 22px;
  border-top: 1px solid var(--line);
  padding-top: 16px;
}

.recent-t h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-family: var(--font-title);
}

.rt-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13.5px;
}

.rt-item:last-child { border-bottom: none; }

.rt-emoji { font-size: 18px; }
.rt-info { flex: 1; display: flex; flex-direction: column; line-height: 1.35; }
.rt-info span { font-size: 12px; color: var(--ink-3); }

.rt-status {
  font-size: 12px;
  color: var(--c-orange);
  background: var(--accent-soft);
  border-radius: 999px;
  padding: 2px 10px;
  font-weight: 700;
}

.rt-status.done {
  color: #1f7a52;
  background: #e4f4ec;
}

@media (max-width: 1100px) {
  .ov-strip { grid-template-columns: repeat(2, 1fr); }
  .t-grid { grid-template-columns: 1fr; }
}
</style>
