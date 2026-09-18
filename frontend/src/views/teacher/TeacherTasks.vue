<template>
  <div class="k-page t-tasks">
    <div class="page-head">
      <div class="head-row">
        <div class="head-title">
          <h1 class="k-h1-icon"><PushPin :size="24" weight="bold" /> 任务布置</h1>
          <p class="sub">选用课程、编程实验或 AI 互动任务作为课后作业</p>
        </div>
        <button class="k-btn" @click="openCreate">
          <Plus :size="16" weight="bold" /> 发布新任务
        </button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <div class="status-tabs">
        <button v-for="s in statusTabs" :key="s.value" class="s-tab" :class="{ active: statusFilter === s.value }" @click="statusFilter = s.value">
          {{ s.label }}
        </button>
      </div>
      <div class="type-chips">
        <button v-for="t in typeChips" :key="t.value" class="t-chip" :class="{ active: typeFilter === t.value }" @click="typeFilter = t.value">
          {{ t.label }}
        </button>
      </div>
    </div>

    <!-- 任务列表 -->
    <section class="k-card task-card">
      <div class="task-card-head">
        <h3><ListChecks :size="17" weight="bold" /> 任务列表</h3>
        <span class="task-count">共 {{ filteredTasks.length }} 个任务</span>
      </div>
      <el-table :data="filteredTasks" class="task-table">
        <el-table-column label="任务名称" min-width="260">
          <template #default="{ row }">
            <div class="tt-name">
              <span class="tt-icon"><component :is="typeIcon(row.type)" :size="16" weight="bold" /></span>
              <div class="tt-name-text">
                <b>{{ row.name }}</b>
                <span>{{ row.resource }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="128" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="typeCls(row.type)">{{ typeLabel(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布班级" width="120" align="center">
          <template #default="{ row }">{{ classLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span class="tt-deadline">
              <CalendarBlank :size="14" weight="bold" />
              {{ row.deadline }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="row.status === 'active' ? 'k-tag--orange' : ''">
              {{ row.status === 'active' ? '进行中' : '已截止' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="提交数" width="92" align="center">
          <template #default="{ row }">
            <b class="tt-sub">{{ row.submitted }}/{{ row.total }}</b>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="204" align="center" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
              <el-button v-if="row.status === 'active'" link type="warning" @click="closeTask(row)">截止</el-button>
              <el-button link type="danger" @click="removeTask(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 发布新任务 -->
    <el-dialog v-model="createVisible" title="发布新任务" width="560px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="88px" label-position="left" @submit.prevent>
        <el-form-item label="任务名称">
          <el-input v-model="createForm.name" placeholder="给任务起个名字，如：观看《什么是人工智能？》" maxlength="40" />
        </el-form-item>
        <el-form-item label="发布班级">
          <el-select v-model="createForm.classes" multiple placeholder="选择发布班级" class="tk-select">
            <el-option v-for="c in classes" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.type" class="tk-radio">
            <el-radio-button value="course">课程任务</el-radio-button>
            <el-radio-button value="program">编程实验</el-radio-button>
            <el-radio-button value="ai">AI 互动实验</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联资源">
          <el-select v-model="createForm.resource" placeholder="选择关联资源" class="tk-select">
            <el-option v-for="r in resourceOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务要求">
          <el-input v-model="createForm.requirement" type="textarea" :rows="3" placeholder="补充任务说明，如：看完视频后回答 3 道小问题。" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止时间" value-format="YYYY-MM-DD HH:mm" class="tk-date" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">发布</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情 -->
    <el-dialog v-model="detailVisible" width="560px">
      <template #header>
        <span class="detail-title">{{ detailTask ? detailTask.name : '' }}</span>
      </template>
      <div v-if="detailTask" class="detail-body">
        <div class="detail-row">
          <span class="detail-label"><Tag :size="15" weight="bold" /> 任务类型</span>
          <span class="k-tag" :class="typeCls(detailTask.type)">{{ typeLabel(detailTask.type) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label"><Student :size="15" weight="bold" /> 发布班级</span>
          <span>{{ detailTask.classes.join('、') }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label"><FileText :size="15" weight="bold" /> 关联资源</span>
          <span>{{ detailTask.resource }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label"><CalendarBlank :size="15" weight="bold" /> 截止时间</span>
          <span>{{ detailTask.deadline }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label"><ClipboardText :size="15" weight="bold" /> 任务要求</span>
          <span>{{ detailTask.requirement || '暂无补充说明' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label"><ChartLine :size="15" weight="bold" /> 提交情况</span>
          <span class="sub-line">
            {{ detailTask.submitted }}/{{ detailTask.total }} 名学生已提交
            <el-button link type="primary" class="view-sub-btn" @click="openSubmitDetail">查看明细 ›</el-button>
          </span>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 提交明细 -->
    <el-dialog v-model="submitDetailVisible" title="提交明细" width="540px">
      <div class="sub-detail-list">
        <div v-for="r in submitRows" :key="r.studentId" class="sub-detail-item">
          <span class="sd-name">{{ r.studentName || '学生' }}</span>
          <el-tag :type="r.submitted ? 'success' : 'info'" round size="small">
            {{ r.submitted ? '已提交' : '未提交' }}
          </el-tag>
          <span v-if="r.submitted && r.score != null" class="sd-score">{{ r.score }} 分</span>
          <span v-if="r.submitted && r.submittedAt" class="sd-time">{{ (r.submittedAt || '').slice(0, 16) }}</span>
        </div>
        <el-empty v-if="submitRows.length === 0" description="暂无学生明细" />
      </div>
      <template #footer>
        <el-button type="primary" @click="submitDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { PhPushPin as PushPin, PhPlus as Plus, PhPlayCircle as PlayCircle, PhCpu as Cpu, PhRobot as Robot, PhListChecks as ListChecks, PhCalendarBlank as CalendarBlank, PhTag as Tag, PhStudent as Student, PhFileText as FileText, PhClipboardText as ClipboardText, PhChartLine as ChartLine } from '@phosphor-icons/vue'

// ===== FALLBACK 数据 =====
const FALLBACK_CLASSES = ['五年级(2)班', '四年级(1)班', '三年级(3)班']
const FALLBACK_CLASS_COUNTS = { '五年级(2)班': 42, '四年级(1)班': 38, '三年级(3)班': 46 }

const FALLBACK_TASKS = [
  { id: 1, name: '观看《什么是人工智能？》', type: 'course', classes: ['五年级(2)班', '四年级(1)班'], resource: '什么是人工智能？', requirement: '看完视频后回答 3 道小问题。', deadline: '2025-09-08 23:59', status: 'closed', submitted: 32, total: 80 },
  { id: 2, name: '积木作品：小星星循环舞', type: 'program', classes: ['五年级(2)班'], resource: '小星星循环舞', requirement: '使用循环积木完成星星闪烁效果并提交。', deadline: '2025-09-12 23:59', status: 'active', submitted: 18, total: 42 },
  { id: 3, name: 'AI 写作：我的未来学校', type: 'ai', classes: ['五年级(2)班', '四年级(1)班', '三年级(3)班'], resource: '创意写作：我的未来学校', requirement: '和 AI 一起写下你心中的未来学校。', deadline: '2025-09-16 23:59', status: 'active', submitted: 46, total: 126 },
  { id: 4, name: '知识闯关：AI 基础 10 题', type: 'ai', classes: ['三年级(3)班'], resource: '知识闯关：AI 基础 10 题', requirement: '完成 AI 基础知识闯关挑战。', deadline: '2025-09-05 23:59', status: 'closed', submitted: 40, total: 46 },
  { id: 5, name: '图形化编程：小小天气预报员', type: 'program', classes: ['四年级(1)班', '三年级(3)班'], resource: '小小天气预报员', requirement: '制作一个可以选择天气并播放提示的小程序。', deadline: '2025-09-18 23:59', status: 'active', submitted: 12, total: 84 }
]

const classes = ref([...FALLBACK_CLASSES])
const classStudentCount = ref({ ...FALLBACK_CLASS_COUNTS })
let classMap = {} // 班级名 -> id

const TYPE_META = {
  course: { label: '课程任务', cls: '', icon: PlayCircle },
  program: { label: '编程实验', cls: 'k-tag--teal', icon: Cpu },
  ai: { label: 'AI 互动实验', cls: 'k-tag--orange', icon: Robot }
}

const typeLabel = (t) => (TYPE_META[t.type] || TYPE_META.course).label
const typeCls = (t) => (TYPE_META[t.type] || TYPE_META.course).cls
const typeIcon = (t) => (TYPE_META[t.type] || TYPE_META.course).icon
const classLabel = (row) => (row.classes.length > 1 ? `${row.classes.length} 个班` : row.classes[0])

const statusTabs = [
  { value: 'all', label: '全部' },
  { value: 'active', label: '进行中' },
  { value: 'closed', label: '已截止' }
]

const typeChips = [
  { value: 'all', label: '全部类型' },
  { value: 'course', label: '课程' },
  { value: 'program', label: '编程' },
  { value: 'ai', label: 'AI 实验' }
]

const statusFilter = ref('all')
const typeFilter = ref('all')

const tasks = ref([...FALLBACK_TASKS])

const filteredTasks = computed(() => {
  return tasks.value.filter(t => {
    const okStatus = statusFilter.value === 'all' || t.status === statusFilter.value
    const okType = typeFilter.value === 'all' || t.type === typeFilter.value
    return okStatus && okType
  })
})

// 后端 type：1课程 2编程 3AI实验 4闯关
const NUM_TO_TYPEKEY = { 1: 'course', 2: 'program', 3: 'ai', 4: 'ai' }

function mapAssignment(a) {
  const isCourse = Number(a.type) === 1
  let resource = a.resourceName || '未选择'
  let requirement = a.content || ''
  // 非课程任务无资源表，布置时把资源名拼进 content（资源名；要求），此处拆解
  if (!isCourse && a.content && a.content.includes('；')) {
    const parts = a.content.split('；')
    resource = parts[0] || '未选择'
    requirement = parts.slice(1).join('；')
  }
  return {
    id: a.id,
    name: a.title,
    type: NUM_TO_TYPEKEY[Number(a.type)] || 'course',
    classes: a.className ? [a.className] : ['未发布班级'],
    resource,
    requirement,
    deadline: a.deadline || '未设置',
    status: Number(a.status) === 1 ? 'active' : 'closed',
    submitted: Number(a.submissionCount ?? 0),
    total: Number(a.totalCount ?? 0)
  }
}

async function loadTasks() {
  try {
    const params = {}
    if (statusFilter.value === 'active') params.status = 1
    else if (statusFilter.value === 'closed') params.status = 0
    const data = await request.get('/teacher/assignments', { params })
    const list = Array.isArray(data) ? data : []
    tasks.value = list.map(mapAssignment)
  } catch (e) {
    // 回退 FALLBACK
  }
}

watch(statusFilter, () => loadTasks())

async function loadClasses() {
  try {
    const data = await request.get('/teacher/classes')
    const list = Array.isArray(data) ? data : []
    classMap = {}
    list.forEach(c => {
      if (c.name) classMap[c.name] = c.id
    })
    classes.value = list.map(c => c.name).filter(Boolean)
    const counts = {}
    const results = await Promise.all(
      list.map(c => request.get(`/teacher/classes/${c.id}/students`).catch(() => []))
    )
    list.forEach((c, i) => {
      counts[c.name] = Array.isArray(results[i]) ? results[i].length : 0
    })
    classStudentCount.value = counts
  } catch (e) {}
}

/* ---------- 发布新任务 ---------- */
const createVisible = ref(false)
const createForm = ref({ name: '', classes: [], type: 'course', resource: '', requirement: '', deadline: '' })

// 课程资源列表（type=course 时用真实课程，可正确关联 resourceId）
const courseOptions = ref([])
async function loadCourses() {
  try {
    const list = await request.get('/course/list')
    courseOptions.value = (list || []).map(c => ({ id: c.id, title: c.title }))
  } catch (e) {}
}

// 编程模板资源列表（type=program 时用启用的积木模板，停用后不可关联）
const programOptions = ref([])
async function loadProgramOptions() {
  try {
    const list = await request.get('/project/templates')
    programOptions.value = (Array.isArray(list) ? list : []).map(t => t.name)
  } catch (e) {}
}

const resourceOptions = computed(() => {
  if (createForm.value.type === 'course') {
    return courseOptions.value.map(c => c.title)
  }
  if (createForm.value.type === 'program') {
    return programOptions.value
  }
  const map = {
    ai: ['创意写作：我的未来学校', '知识闯关：AI 基础 10 题', '智能问答机器人']
  }
  return map[createForm.value.type] || []
})

const TYPE_NUM = { course: 1, program: 2, ai: 3 }

function openCreate() {
  createForm.value = { name: '', classes: [], type: 'course', resource: '', requirement: '', deadline: '' }
  createVisible.value = true
}

function submitCreate() {
  const f = createForm.value
  if (!f.name.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  if (!f.classes.length) {
    ElMessage.warning('请选择发布班级')
    return
  }
  if (!f.resource) {
    ElMessage.warning('请选择关联资源')
    return
  }
  const classIds = f.classes.map(name => classMap[name]).filter(id => id != null)
  let resourceId = null
  if (f.type === 'course') {
    const c = courseOptions.value.find(c => c.title === f.resource)
    resourceId = c ? c.id : null
  }
  // 课程任务资源名由 resourceId 关联课程；编程/AI 任务无资源表，资源名拼进 content
  const content = f.type === 'course'
    ? f.requirement
    : [f.resource, f.requirement].filter(Boolean).join('；')
  const payload = {
    title: f.name.trim(),
    classIds,
    type: TYPE_NUM[f.type] || 1,
    resourceId,
    content,
    deadline: f.deadline || null
  }
  request.post('/teacher/assignments', payload)
    .then(() => {
      createVisible.value = false
      ElMessage.success('新任务已发布')
      loadTasks()
    })
    .catch(() => {})
}

/* ---------- 行操作 ---------- */
const detailVisible = ref(false)
const detailTask = ref(null)
const submitDetailVisible = ref(false)
const submitRows = ref([])

function openDetail(row) {
  detailTask.value = { ...row, submitRows: [] }
  detailVisible.value = true
  // 拉取该任务提交情况，刷新详情与列表的提交数
  request.get(`/teacher/progress/assignment/${row.id}`)
    .then((data) => {
      if (!detailTask.value || detailTask.value.id !== row.id) return
      const rows = Array.isArray(data) ? data : []
      detailTask.value.submitRows = rows
      detailTask.value.submitted = rows.filter(r => r.submitted !== false).length
      detailTask.value.total = rows.length
      const found = tasks.value.find(t => t.id === row.id)
      if (found) {
        found.submitted = detailTask.value.submitted
        found.total = detailTask.value.total
      }
    })
    .catch(() => {})
}

const openSubmitDetail = () => {
  submitRows.value = detailTask.value?.submitRows || []
  submitDetailVisible.value = true
}

function closeTask(row) {
  request.put(`/teacher/assignments/${row.id}/close`)
    .then(() => {
      row.status = 'closed'
      ElMessage.success(`任务「${row.name}」已截止`)
    })
    .catch(() => {})
}

function removeTask(row) {
  ElMessageBox.confirm(`确定删除任务「${row.name}」吗？删除后不可恢复。`, '删除任务', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(() => {
      request.delete(`/teacher/assignments/${row.id}`)
        .then(() => {
          tasks.value = tasks.value.filter(x => x.id !== row.id)
          ElMessage.success('任务已删除')
        })
        .catch(() => {})
    })
    .catch(() => {})
}

onMounted(() => {
  loadClasses()
  loadTasks()
  loadCourses()
  loadProgramOptions()
})
</script>

<style scoped>
.t-tasks .sub { color: var(--ink-2); margin: 4px 0 0; }

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.status-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--surface-soft);
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 4px;
}

.s-tab {
  border: none;
  background: transparent;
  color: var(--ink-2);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  padding: 7px 18px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.s-tab:hover { color: var(--brand); }

.s-tab.active {
  background: var(--brand);
  color: #fff;
  box-shadow: 0 3px 10px rgba(30, 79, 216, 0.25);
}

.type-chips {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.t-chip {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s;
}

.t-chip:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.t-chip.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent-deep);
}

/* ---------- 任务列表 ---------- */
.task-card {
  padding: var(--space-5) var(--space-5) var(--space-5);
}

.task-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.task-card-head h3 {
  margin: 0;
  font-size: 16.5px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.task-card-head h3 svg { color: var(--brand); }

.task-count {
  font-size: 13px;
  color: var(--ink-3);
}

.task-table :deep(.el-table) {
  --el-table-border-color: var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.task-table :deep(.el-table th.el-table__cell) {
  background: var(--surface-soft);
  color: var(--ink-2);
  font-weight: 600;
}

.task-table :deep(.el-table td.el-table__cell) {
  font-size: 14px;
}

.tt-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tt-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--brand-soft);
  color: var(--brand);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tt-name-text b {
  display: block;
  font-size: 14.5px;
  line-height: 1.3;
}

.tt-name-text span {
  font-size: 12px;
  color: var(--ink-3);
}

.tt-deadline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-2);
  font-size: 13px;
}

.tt-deadline svg { color: var(--ink-3); }

.tt-sub {
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

/* ---------- 表单 / 详情 ---------- */
.tk-select { width: 100%; }
.tk-date { width: 100%; }

.tk-radio :deep(.el-radio-button__inner) {
  font-weight: 600;
}

.detail-title {
  font-family: var(--font-title);
  font-size: 17px;
  color: var(--ink);
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.detail-row:last-child { border-bottom: none; }

.detail-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-3);
  font-weight: 500;
  width: 110px;
  flex-shrink: 0;
}

.detail-label svg { color: var(--brand); }
.detail-row > span:last-child { color: var(--ink); }

/* 操作列按钮横排 */
.row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}
.row-actions .el-button + .el-button {
  margin-left: 2px;
}

/* 提交情况明细 */
.sub-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.view-sub-btn {
  font-size: 13px;
}
.sub-detail-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 60vh;
  overflow-y: auto;
}
.sub-detail-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #f8faff;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 13.5px;
}
.sd-name {
  flex: 1;
  font-weight: 600;
  color: var(--ink);
}
.sd-score {
  color: var(--brand);
  font-weight: 700;
}
.sd-time {
  color: var(--ink-3);
  font-size: 12.5px;
}

@media (max-width: 640px) {
  .head-row { flex-direction: column; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
}
</style>
