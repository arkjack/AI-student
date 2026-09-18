<template>
  <div class="k-page t-progress">
    <div class="page-head">
      <h1 class="k-h1-icon"><ChartLineUp :size="24" weight="bold" /> 进度追踪</h1>
      <p class="sub">按学生或按任务查看学习进度</p>
    </div>

    <!-- 视图切换 -->
    <div class="pv-toolbar">
      <el-radio-group v-model="viewMode">
        <el-radio-button value="student">按学生</el-radio-button>
        <el-radio-button value="task">按任务</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 按学生 -->
    <template v-if="viewMode === 'student'">
      <div class="pv-grid">
        <aside class="k-card pv-side">
          <div class="side-head"><h3><Users :size="16" weight="bold" /> 班级筛选</h3></div>
          <el-select v-model="selectedClassId" placeholder="选择班级" class="side-select">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="class-list">
            <div
              v-for="c in classes"
              :key="c.id"
              class="class-item"
              :class="{ active: selectedClassId === c.id }"
              @click="selectedClassId = c.id"
            >
              <span class="ci-name">{{ c.name }}</span>
              <span class="ci-count">{{ classStudentCount(c.id) }} 人</span>
            </div>
          </div>
          <div class="side-tip">
            <ChartLineUp :size="15" weight="bold" />
            班级平均进度 {{ classAvgProgress }}%
          </div>
        </aside>

        <section class="k-card pv-main">
          <div class="pv-search">
            <el-input v-model="studentSearch" placeholder="搜索学生姓名 / 学号..." clearable>
              <template #prefix><MagnifyingGlass :size="16" /></template>
            </el-input>
            <span class="pv-count">共 {{ visibleStudents.length }} 人</span>
          </div>
          <el-table :data="visibleStudents" :height="460" style="width: 100%">
            <el-table-column label="姓名" width="130">
              <template #default="{ row }">
                <div class="stu-cell">
                  <span class="stu-avatar">{{ row.avatar }}</span>
                  <b>{{ row.name }}</b>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="sid" label="学号" width="120" />
            <el-table-column label="课程进度" min-width="180">
              <template #default="{ row }">
                <div class="prog-cell">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ transform: 'scaleX(' + (row.progress / 100) + ')' }"></div>
                  </div>
                  <span class="prog-num">{{ row.progress }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="expCount" label="实验完成" width="96" align="center" />
            <el-table-column prop="hwCount" label="作业提交" width="96" align="center" />
            <el-table-column prop="lastTime" label="最近学习" min-width="150" />
          </el-table>
        </section>
      </div>
    </template>

    <!-- 按任务 -->
    <template v-else>
      <div class="pv-task-head">
        <h3><PushPin :size="16" weight="bold" /> {{ currentTask.name }}</h3>
        <el-select v-model="selectedTaskId" placeholder="选择任务" class="task-select">
          <el-option v-for="t in tasks" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>

      <!-- 单任务汇总 -->
      <div class="pv-task-sum">
        <div class="k-card pv-sum-card">
          <span class="pv-sum-icon green"><CheckCircle :size="22" weight="bold" /></span>
          <div class="pv-sum-num"><b>{{ submittedCount }}</b><span>已提交</span></div>
        </div>
        <div class="k-card pv-sum-card">
          <span class="pv-sum-icon blue"><Users :size="22" weight="bold" /></span>
          <div class="pv-sum-num"><b>{{ totalCount }}</b><span>总人数</span></div>
        </div>
        <div class="k-card pv-sum-card">
          <span class="pv-sum-icon orange"><Clock :size="22" weight="bold" /></span>
          <div class="pv-sum-num"><b>{{ pendingReviewCount }}</b><span>待批改</span></div>
        </div>
      </div>

      <section class="k-card">
        <div class="pv-task-table-head">
          <span class="k-tag">{{ currentTask.type }}</span>
          <span class="pv-task-desc">完成情况一览</span>
        </div>
        <el-table :data="activeTaskRows" :height="400" style="width: 100%">
          <el-table-column prop="name" label="学生姓名" min-width="140" />
          <el-table-column label="提交状态" width="130">
            <template #default="{ row }">
              <span class="k-tag" :class="statusClass(row.status)">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.score != null" class="score">{{ row.score }}</span>
              <span v-else class="score-none">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="提交时间" min-width="160" />
        </el-table>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhChartLineUp as ChartLineUp,
  PhUsers as Users,
  PhPushPin as PushPin,
  PhMagnifyingGlass as MagnifyingGlass,
  PhCheckCircle as CheckCircle,
  PhClock as Clock
} from '@phosphor-icons/vue'

const viewMode = ref('student')
const selectedClassId = ref(null)
const selectedTaskId = ref(null)
const studentSearch = ref('')

watch(viewMode, (mode) => {
  ElMessage.info(mode === 'student' ? '已切换到按学生查看' : '已切换到按任务查看')
})

// ===== FALLBACK 数据 =====
const FALLBACK_CLASSES = [
  { id: 'c1', name: '五年级(1)班' },
  { id: 'c2', name: '五年级(2)班' },
  { id: 'c3', name: '四年级(1)班' }
]

const FALLBACK_STUDENTS = [
  { id: 1, classId: 'c1', name: '小明', sid: '20240105', avatar: '🦊', progress: 88, expCount: 3, hwCount: 4, lastTime: '今天 09:12' },
  { id: 2, classId: 'c1', name: '朵朵', sid: '20240112', avatar: '🐰', progress: 72, expCount: 2, hwCount: 3, lastTime: '今天 08:47' },
  { id: 3, classId: 'c1', name: '小航', sid: '20240126', avatar: '🐼', progress: 96, expCount: 3, hwCount: 4, lastTime: '昨天 19:03' },
  { id: 4, classId: 'c1', name: '糖糖', sid: '20240133', avatar: '🐱', progress: 55, expCount: 2, hwCount: 2, lastTime: '昨天 17:40' },
  { id: 5, classId: 'c1', name: '果果', sid: '20240141', avatar: '🐻', progress: 34, expCount: 1, hwCount: 2, lastTime: '09-04 20:15' },
  { id: 6, classId: 'c1', name: '乐乐', sid: '20240152', avatar: '🦁', progress: 100, expCount: 3, hwCount: 4, lastTime: '09-03 16:22' },

  { id: 7, classId: 'c2', name: '安安', sid: '20240208', avatar: '🐨', progress: 64, expCount: 2, hwCount: 3, lastTime: '今天 10:20' },
  { id: 8, classId: 'c2', name: '圆圆', sid: '20240215', avatar: '🐶', progress: 81, expCount: 3, hwCount: 3, lastTime: '昨天 21:05' },
  { id: 9, classId: 'c2', name: '石头', sid: '20240223', avatar: '🦖', progress: 47, expCount: 1, hwCount: 2, lastTime: '09-05 18:40' },
  { id: 10, classId: 'c2', name: '可可', sid: '20240237', avatar: '🐹', progress: 92, expCount: 3, hwCount: 4, lastTime: '09-04 12:30' },
  { id: 11, classId: 'c2', name: '点点', sid: '20240244', avatar: '🐷', progress: 28, expCount: 1, hwCount: 2, lastTime: '09-02 15:18' },
  { id: 12, classId: 'c2', name: '悦悦', sid: '20240251', avatar: '🐸', progress: 75, expCount: 2, hwCount: 3, lastTime: '09-01 09:45' },

  { id: 13, classId: 'c3', name: '豆豆', sid: '20240306', avatar: '🐤', progress: 60, expCount: 2, hwCount: 3, lastTime: '今天 11:08' },
  { id: 14, classId: 'c3', name: '茉莉', sid: '20240319', avatar: '🐝', progress: 87, expCount: 3, hwCount: 4, lastTime: '昨天 17:52' },
  { id: 15, classId: 'c3', name: '木木', sid: '20240327', avatar: '🐢', progress: 43, expCount: 1, hwCount: 2, lastTime: '09-05 10:11' },
  { id: 16, classId: 'c3', name: '团团', sid: '20240335', avatar: '🐧', progress: 70, expCount: 2, hwCount: 3, lastTime: '09-04 19:26' },
  { id: 17, classId: 'c3', name: '跳跳', sid: '20240348', avatar: '🦄', progress: 100, expCount: 3, hwCount: 4, lastTime: '09-03 08:36' }
]

const FALLBACK_TASKS = [
  { id: 't1', name: '观看《什么是人工智能？》', type: '课程学习' },
  { id: 't2', name: '积木作品：小星星循环舞', type: '编程作品' },
  { id: 't3', name: 'AI 写作：我的未来学校', type: 'AI 写作' },
  { id: 't4', name: '课程问答：什么是人工智能', type: '课程问答' }
]

const FALLBACK_TASK_SUBS = {
  t1: [
    { studentId: 1, status: '已批改', score: 95, time: '09-08 09:30' },
    { studentId: 2, status: '已提交', score: null, time: '09-08 09:12' },
    { studentId: 3, status: '已批改', score: 88, time: '09-07 20:15' },
    { studentId: 4, status: '已提交', score: null, time: '09-07 18:40' },
    { studentId: 5, status: '已提交', score: null, time: '09-06 19:02' },
    { studentId: 6, status: '已批改', score: 96, time: '09-05 12:30' },
    { studentId: 7, status: '已提交', score: null, time: '09-08 10:20' },
    { studentId: 8, status: '已批改', score: 92, time: '09-06 21:05' },
    { studentId: 10, status: '已批改', score: 90, time: '09-04 12:30' },
    { studentId: 12, status: '已提交', score: null, time: '09-01 09:45' },
    { studentId: 13, status: '已提交', score: null, time: '09-08 11:08' },
    { studentId: 14, status: '已批改', score: 89, time: '09-05 17:52' },
    { studentId: 15, status: '已提交', score: null, time: '09-05 10:11' },
    { studentId: 16, status: '已提交', score: null, time: '09-04 19:26' },
    { studentId: 17, status: '已批改', score: 98, time: '09-03 08:36' }
  ],
  t2: [
    { studentId: 1, status: '已批改', score: 90, time: '09-12 15:40' },
    { studentId: 2, status: '已提交', score: null, time: '09-12 09:12' },
    { studentId: 3, status: '已提交', score: null, time: '09-11 19:03' },
    { studentId: 4, status: '已提交', score: null, time: '09-11 17:40' },
    { studentId: 6, status: '已批改', score: 86, time: '09-10 18:22' },
    { studentId: 7, status: '已提交', score: null, time: '09-12 10:20' },
    { studentId: 8, status: '已提交', score: null, time: '09-11 21:05' },
    { studentId: 10, status: '已批改', score: 88, time: '09-10 12:30' },
    { studentId: 12, status: '已提交', score: null, time: '09-09 09:45' },
    { studentId: 13, status: '已提交', score: null, time: '09-12 11:08' },
    { studentId: 14, status: '已提交', score: null, time: '09-10 17:52' },
    { studentId: 16, status: '已批改', score: 84, time: '09-09 19:26' },
    { studentId: 17, status: '已批改', score: 92, time: '09-08 08:36' }
  ],
  t3: [
    { studentId: 1, status: '已提交', score: null, time: '09-14 09:30' },
    { studentId: 2, status: '已批改', score: 78, time: '09-14 08:47' },
    { studentId: 4, status: '已提交', score: null, time: '09-13 19:40' },
    { studentId: 6, status: '已提交', score: null, time: '09-13 16:02' },
    { studentId: 7, status: '已提交', score: null, time: '09-14 10:20' },
    { studentId: 8, status: '已提交', score: null, time: '09-14 09:05' },
    { studentId: 10, status: '已提交', score: null, time: '09-13 12:30' },
    { studentId: 12, status: '已提交', score: null, time: '09-12 09:45' },
    { studentId: 13, status: '已批改', score: 82, time: '09-14 11:08' },
    { studentId: 14, status: '已提交', score: null, time: '09-13 17:52' },
    { studentId: 16, status: '已提交', score: null, time: '09-12 19:26' },
    { studentId: 17, status: '已提交', score: null, time: '09-11 08:36' }
  ],
  t4: [
    { studentId: 1, status: '已批改', score: 85, time: '09-15 09:30' },
    { studentId: 3, status: '已提交', score: null, time: '09-15 08:20' },
    { studentId: 6, status: '已批改', score: 80, time: '09-14 16:22' },
    { studentId: 7, status: '已提交', score: null, time: '09-15 10:20' },
    { studentId: 10, status: '已提交', score: null, time: '09-14 12:30' },
    { studentId: 13, status: '已提交', score: null, time: '09-15 11:08' },
    { studentId: 14, status: '已批改', score: 79, time: '09-14 17:52' },
    { studentId: 17, status: '已批改', score: 90, time: '09-13 08:36' }
  ]
}

const classes = ref([...FALLBACK_CLASSES])
const students = ref([...FALLBACK_STUDENTS])
const tasks = ref([...FALLBACK_TASKS])
const activeTaskRows = ref([])
const classCounts = ref({})

const AVAILABLE_EMOJI = ['🦊', '🐰', '🐼', '🐱', '🐻', '🦁', '🐨', '🐶', '🦖', '🐹', '🐷', '🐸', '🐤', '🐝', '🐢', '🐧', '🦄']

function mapStudentProgress(s, classId, index) {
  const completed = s.completedCourses != null ? s.completedCourses : 0
  let p = s.progress != null ? s.progress : completed * 20
  if (typeof p !== 'number' || Number.isNaN(p)) p = 0
  p = Math.max(0, Math.min(100, p))
  return {
    id: s.studentId,
    classId,
    name: s.realName || s.nickname || s.username || '学生',
    sid: s.username || s.studentId || '—',
    avatar: s.avatar || AVAILABLE_EMOJI[index % AVAILABLE_EMOJI.length],
    progress: p,
    expCount: completed,
    hwCount: s.submissions ?? s.submissionCount ?? 0,
    lastTime: s.lastStudyAt || s.submittedAt || '—'
  }
}

// ===== 学生视图逻辑 =====
const visibleStudents = computed(() => {
  let list = students.value.filter(s => s.classId === selectedClassId.value)
  const kw = studentSearch.value.trim()
  if (kw) list = list.filter(s => s.name.includes(kw) || String(s.sid).includes(kw))
  return list
})

const classStudentCount = (classId) => {
  if (classCounts.value[classId] != null) return classCounts.value[classId]
  return students.value.filter(s => s.classId === classId).length
}

const classAvgProgress = computed(() => {
  const list = students.value.filter(s => s.classId === selectedClassId.value)
  if (!list.length) return 0
  return Math.round(list.reduce((sum, s) => sum + (s.progress || 0), 0) / list.length)
})

async function loadClasses() {
  try {
    const data = await request.get('/teacher/classes')
    const list = Array.isArray(data) ? data : []
    classes.value = list.map(c => ({ id: c.id, name: c.name }))
    if (list.length) selectedClassId.value = list[0].id
    const counts = {}
    const results = await Promise.all(
      list.map(c => request.get(`/teacher/classes/${c.id}/students`).catch(() => []))
    )
    list.forEach((c, i) => {
      counts[c.id] = Array.isArray(results[i]) ? results[i].length : 0
    })
    classCounts.value = counts
    return true
  } catch (e) {
    return false
  }
}

async function loadStudents() {
  if (selectedClassId.value == null) return
  try {
    const data = await request.get('/teacher/progress/students', { params: { classId: selectedClassId.value } })
    const rows = Array.isArray(data) ? data : []
    students.value = rows.map((s, i) => mapStudentProgress(s, selectedClassId.value, i))
  } catch (e) {
    // 回退 FALLBACK—若 selectedClassId 已是真实 id，mock 无法匹配则列表为空
  }
}

// ===== 任务视图逻辑 =====
const typeNumToLabel = (n) => ({ 1: '课程学习', 2: '编程作品', 3: 'AI 写作', 4: '知识闯关' })[n] || '未知类型'

async function loadTasks() {
  try {
    const data = await request.get('/teacher/assignments')
    const list = Array.isArray(data) ? data : []
    tasks.value = list.map(a => ({ id: a.id, name: a.title, type: typeNumToLabel(Number(a.type)) }))
    if (list.length) selectedTaskId.value = list[0].id
    return true
  } catch (e) {
    return false
  }
}

async function loadTaskRows() {
  if (selectedTaskId.value == null) return
  try {
    const data = await request.get(`/teacher/progress/assignment/${selectedTaskId.value}`)
    const rows = Array.isArray(data) ? data : []
    activeTaskRows.value = rows.map(r => {
      const submitted = r.submitted
      let status
      if (submitted === false) status = '未提交'
      else if (r.status) status = r.status
      else status = r.score != null ? '已批改' : '已提交'
      return {
        name: r.studentName || '学生',
        status,
        score: r.score != null ? r.score : null,
        time: r.submittedAt || '—'
      }
    })
  } catch (e) {
    activeTaskRows.value = fallbackTaskRows(selectedTaskId.value)
  }
}

function fallbackTaskRows(taskId) {
  const subs = FALLBACK_TASK_SUBS[taskId] || []
  const map = {}
  subs.forEach(s => { map[s.studentId] = s })
  return FALLBACK_STUDENTS.map(s => {
    const sub = map[s.id]
    return {
      name: s.name,
      status: sub ? sub.status : '未提交',
      score: sub ? (sub.score ?? null) : null,
      time: sub ? sub.time : '—'
    }
  })
}

const currentTask = computed(() => tasks.value.find(t => t.id === selectedTaskId.value) || tasks.value[0] || { name: '', type: '' })

const submittedCount = computed(() => activeTaskRows.value.filter(r => r.status !== '未提交').length)
const totalCount = computed(() => activeTaskRows.value.length)
const pendingReviewCount = computed(() => activeTaskRows.value.filter(r => r.status === '已提交' || r.status === '待批改').length)

const statusClass = (status) => {
  if (status === '已批改') return 'k-tag--green'
  if (status === '未提交') return 'k-tag--yellow'
  return ''
}

watch(selectedClassId, () => loadStudents())
watch(selectedTaskId, () => loadTaskRows())

onMounted(async () => {
  const classOk = await loadClasses()
  const taskOk = await loadTasks()
  if (!classOk) loadStudents()
  if (!taskOk) loadTaskRows()
})
</script>

<style scoped>
.t-progress .sub { color: var(--ink-2); margin: 4px 0 0; }

.pv-toolbar {
  margin-bottom: var(--space-5);
}

/* ===== 学生视图 ===== */
.pv-grid {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: var(--space-5);
  align-items: start;
}

.pv-side { padding: var(--space-5); }

.side-head h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 16px;
}
.side-head h3 svg { color: var(--brand); }

.side-select {
  width: 100%;
  margin-bottom: 14px;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.class-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.18s;
  font-size: 14px;
}

.class-item:hover { border-color: var(--brand); }

.class-item.active {
  background: var(--brand-soft);
  border-color: var(--brand);
  color: var(--brand-deep);
}

.ci-name { font-weight: 600; }
.ci-count { font-size: 12px; color: var(--ink-3); }

.side-tip {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  color: var(--ink-2);
  font-size: 12.5px;
}
.side-tip svg { color: var(--brand); }

.pv-main { padding: var(--space-5); }

.pv-search {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.pv-search .el-input { max-width: 280px; }
.pv-count { font-size: 12.5px; color: var(--ink-3); }

.stu-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.stu-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--surface-soft);
  font-size: 16px;
  flex-shrink: 0;
}

/* 进度条 */
.prog-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.progress-bar {
  flex: 1;
  height: 8px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--brand);
  border-radius: 999px;
  transform-origin: left;
}
.prog-num {
  font-size: 12.5px;
  color: var(--ink-2);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  min-width: 38px;
  text-align: right;
}

/* ===== 任务视图 ===== */
.pv-task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}
.pv-task-head h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
}
.pv-task-head h3 svg { color: var(--brand); }
.task-select { width: 280px; }

.pv-task-sum {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.pv-sum-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 16px 18px;
}

.pv-sum-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.pv-sum-icon.green { background: #e4f4ec; color: #1f7a52; }
.pv-sum-icon.blue { background: var(--brand-soft); color: var(--brand); }
.pv-sum-icon.orange { background: var(--accent-soft); color: var(--accent-deep); }

.pv-sum-num b {
  display: block;
  font-size: 24px;
  line-height: 1.1;
}
.pv-sum-num span { font-size: 12.5px; color: var(--ink-3); }

.pv-task-table-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.pv-task-desc { font-size: 13px; color: var(--ink-3); }

.score {
  font-weight: 700;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.score-none { color: var(--ink-3); }

@media (max-width: 1000px) {
  .pv-grid { grid-template-columns: 1fr; }
  .pv-task-sum { grid-template-columns: 1fr; }
  .task-select { width: 100%; }
}
</style>
