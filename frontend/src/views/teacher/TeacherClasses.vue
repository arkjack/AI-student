<template>
  <div class="k-page t-classes">
    <div class="page-head">
      <div class="head-row">
        <div class="head-title">
          <h1 class="k-h1-icon"><Users :size="24" weight="bold" /> 班级管理</h1>
          <p class="sub">创建班级、导入学生名单，管理你的班级学生</p>
        </div>
        <div class="head-actions">
          <button class="k-btn k-btn--ghost" @click="openImport">
            <UploadSimple :size="16" weight="bold" /> 导入学生
          </button>
          <button class="k-btn" @click="openCreate">
            <Plus :size="16" weight="bold" /> 新建班级
          </button>
        </div>
      </div>
    </div>

    <!-- 班级卡片列表 -->
    <section v-if="classes.length" class="k-grid class-grid">
      <article v-for="c in classes" :key="c.id" class="k-card k-card--hover class-card">
        <span class="cc-bar" :class="c.bar"></span>
        <div class="cc-head">
          <span class="cc-emoji">{{ c.emoji }}</span>
          <div class="cc-title">
            <h3>{{ c.name }}</h3>
            <span class="cc-grade">{{ c.grade }}</span>
          </div>
        </div>
        <dl class="cc-meta">
          <div class="cc-meta-item">
            <dt><Student :size="15" weight="bold" /> 学生数</dt>
            <dd>{{ c.studentCount }} 人</dd>
          </div>
          <div class="cc-meta-item">
            <dt><UserCircle :size="15" weight="bold" /> 教师</dt>
            <dd>{{ c.teacher }}</dd>
          </div>
          <div class="cc-meta-item">
            <dt><CalendarBlank :size="15" weight="bold" /> 创建时间</dt>
            <dd>{{ c.createdAt }}</dd>
          </div>
        </dl>
        <div class="cc-actions">
          <el-button size="small" @click="openStudents(c)">
            <Eye :size="15" weight="bold" /><span>查看学生</span>
          </el-button>
          <el-button size="small" @click="openEdit(c)">
            <PencilSimple :size="15" weight="bold" /><span>编辑</span>
          </el-button>
          <el-button size="small" type="danger" plain @click="removeClass(c)">
            <Trash :size="15" weight="bold" /><span>删除</span>
          </el-button>
        </div>
      </article>
    </section>

    <el-empty v-else description="还没有班级，点击右上角「新建班级」创建你的第一个班级">
      <button class="k-btn" @click="openCreate">
        <Plus :size="16" weight="bold" /> 新建班级
      </button>
    </el-empty>

    <!-- 入班申请审批 -->
    <div class="k-section-head">
      <h2><BellRing :size="20" weight="bold" /> 入班申请</h2>
      <span v-if="pendingApplies.length" class="apply-badge">{{ pendingApplies.length }} 条待审批</span>
    </div>
    <section v-if="applies.length" class="k-card apply-card">
      <div v-for="a in applies" :key="a.id" class="apply-item">
        <span class="apply-avatar">🎓</span>
        <div class="apply-info">
          <b>{{ a.studentName || a.studentNickname || '学生' }}</b>
          <span>申请加入「{{ a.className }}」 · {{ a.createdAt }}</span>
        </div>
        <template v-if="a.status === 0">
          <el-button size="small" type="primary" @click="approveApply(a)">同意</el-button>
          <el-button size="small" @click="rejectApply(a)">拒绝</el-button>
        </template>
        <span v-else class="apply-status" :class="a.status === 1 ? 'ok' : 'no'">{{ a.status === 1 ? '已同意' : '已拒绝' }}</span>
      </div>
    </section>
    <div v-else class="k-card apply-empty">暂无入班申请</div>

    <!-- 新建 / 编辑班级 -->
    <el-dialog v-model="createVisible" :title="editingId ? '编辑班级' : '新建班级'" width="480px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="84px" label-position="left" @submit.prevent>
        <el-form-item label="班级名称">
          <el-input v-model="createForm.name" placeholder="如：五年级(2)班" maxlength="20" />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="createForm.grade" placeholder="选择年级" class="cc-select">
            <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级描述">
          <el-input v-model="createForm.description" placeholder="班级描述（可选）" maxlength="40" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">{{ editingId ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 导入学生 -->
    <el-dialog v-model="importVisible" title="导入学生" width="520px" :close-on-click-modal="false">
      <div class="imp-block">
        <label class="imp-label">选择班级</label>
        <el-select v-model="importClass" placeholder="选择要导入的班级" class="cc-select">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
      <el-upload drag action="#" :auto-upload="false" :limit="1" :show-file-list="false" class="imp-upload">
        <div class="imp-upload-inner">
          <FileXls :size="46" weight="thin" />
          <div class="up-txt">将 .xlsx 文件拖到此处，或 <em>点击选择文件</em></div>
          <div class="up-hint">支持 .xlsx 格式，模板见页面底部「下载学生导入模板」</div>
        </div>
      </el-upload>
      <div class="imp-foot-bar">
        <span class="imp-foot-note">每行一名学生，请按模板列填写，导入成功后自动归入所选班级。</span>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 学生管理抽屉 -->
    <el-drawer v-model="studentsVisible" size="600px" :show-close="false">
      <template #header>
        <div class="dr-head">
          <span class="dr-title">{{ currentClass ? currentClass.name : '' }} · 学生管理</span>
          <button class="k-btn k-btn--ghost" @click="openAddStudent">
            <UserPlus :size="16" weight="bold" /> 添加学生
          </button>
        </div>
      </template>
      <p class="dr-sub">共 {{ currentClass ? currentClass.studentCount : 0 }} 名学生，下方展示班级学生名单。</p>
      <el-table :data="currentClass ? currentClass.students : []" class="stu-table">
        <el-table-column prop="name" label="姓名" min-width="90" />
        <el-table-column prop="studentNo" label="学号" min-width="110" />
        <el-table-column prop="lastStudy" label="最近学习时间" min-width="120" />
        <el-table-column prop="completedCourses" label="完成课程" width="92" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeStudent(row)">移出班级</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 添加学生 -->
    <el-dialog v-model="addVisible" title="添加学生" width="440px" :close-on-click-modal="false">
      <el-form :model="addForm" label-width="56px" label-position="left" @submit.prevent>
        <el-form-item label="学生">
          <el-select v-model="addForm.studentId" placeholder="从学生列表选择" class="cc-select" filterable>
            <el-option v-for="s in availableStudents" :key="s.studentId" :label="s.name" :value="s.studentId" />
          </el-select>
          <div class="imp-foot-note" style="margin-top: 6px;">学生列表来自接口 /teacher/qa/students</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { PhUsers as Users, PhPlus as Plus, PhUploadSimple as UploadSimple, PhFileXls as FileXls, PhEye as Eye, PhPencilSimple as PencilSimple, PhTrash as Trash, PhStudent as Student, PhUserPlus as UserPlus, PhUserCircle as UserCircle, PhCalendarBlank as CalendarBlank, PhBellRinging as BellRing } from '@phosphor-icons/vue'

const grades = ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级']

function readUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (e) {
    return {}
  }
}
const ui = readUserInfo()
const teacherName = ui.nickname || ui.realName || ui.username || '未设置'

const EMOJIS = ['🦊', '🐼', '🐯', '🐨', '🐰', '🐷']
const BARS = ['brand', 'accent']

// ===== FALLBACK 数据 =====
const FALLBACK_CLASSES = [
  {
    id: 1,
    name: '五年级(2)班',
    grade: '五年级',
    studentCount: 42,
    teacher: '秦老师',
    createdAt: '2024-09-01',
    emoji: '🦊',
    bar: 'brand',
    students: [
      { id: 101, name: '小明', studentNo: '20240501', lastStudy: '今天 09:12', completedCourses: 3 },
      { id: 102, name: '朵朵', studentNo: '20240502', lastStudy: '今天 08:47', completedCourses: 5 },
      { id: 103, name: '小航', studentNo: '20240503', lastStudy: '昨天 19:03', completedCourses: 2 },
      { id: 104, name: '糖糖', studentNo: '20240504', lastStudy: '今天 08:47', completedCourses: 4 },
      { id: 105, name: '果果', studentNo: '20240505', lastStudy: '昨天 20:10', completedCourses: 1 },
      { id: 106, name: '大壮', studentNo: '20240506', lastStudy: '3 天前', completedCourses: 6 },
      { id: 107, name: '小美', studentNo: '20240507', lastStudy: '今天 07:55', completedCourses: 2 },
      { id: 108, name: '乐乐', studentNo: '20240508', lastStudy: '昨天 17:30', completedCourses: 4 }
    ]
  },
  {
    id: 2,
    name: '四年级(1)班',
    grade: '四年级',
    studentCount: 38,
    teacher: '秦老师',
    createdAt: '2024-09-05',
    emoji: '🐼',
    bar: 'accent',
    students: [
      { id: 201, name: '阿宝', studentNo: '20240301', lastStudy: '今天 10:20', completedCourses: 4 },
      { id: 202, name: '妮妮', studentNo: '20240302', lastStudy: '昨天 16:42', completedCourses: 2 },
      { id: 203, name: '壮壮', studentNo: '20240303', lastStudy: '2 天前', completedCourses: 3 },
      { id: 204, name: '圆圆', studentNo: '20240304', lastStudy: '今天 09:05', completedCourses: 5 },
      { id: 205, name: '超超', studentNo: '20240305', lastStudy: '昨天 15:30', completedCourses: 1 },
      { id: 206, name: '玲玲', studentNo: '20240306', lastStudy: '3 天前', completedCourses: 2 }
    ]
  },
  {
    id: 3,
    name: '三年级(3)班',
    grade: '三年级',
    studentCount: 46,
    teacher: '李老师',
    createdAt: '2024-09-10',
    emoji: '🐯',
    bar: 'brand',
    students: [
      { id: 301, name: '康康', studentNo: '20240321', lastStudy: '今天 08:15', completedCourses: 3 },
      { id: 302, name: '婷婷', studentNo: '20240322', lastStudy: '昨天 18:33', completedCourses: 4 },
      { id: 303, name: '航航', studentNo: '20240323', lastStudy: '2 天前', completedCourses: 1 },
      { id: 304, name: '果果', studentNo: '20240324', lastStudy: '今天 07:40', completedCourses: 2 },
      { id: 305, name: '悦悦', studentNo: '20240325', lastStudy: '昨天 20:05', completedCourses: 6 },
      { id: 306, name: '乐乐', studentNo: '20240326', lastStudy: '3 天前', completedCourses: 3 },
      { id: 307, name: '杰杰', studentNo: '20240327', lastStudy: '今天 09:30', completedCourses: 2 }
    ]
  }
]

const classes = ref(JSON.parse(JSON.stringify(FALLBACK_CLASSES)))

function enrichClass(c, idx) {
  return {
    ...c,
    emoji: c.emoji || EMOJIS[idx % EMOJIS.length],
    bar: BARS[idx % BARS.length],
    teacher: c.teacher || teacherName,
    studentCount: c.studentCount || 0,
    students: c.students || []
  }
}

function mapStudent(s) {
  return {
    id: s.studentId,
    studentId: s.studentId,
    classStudentId: s.id ?? s.classStudentId ?? null,
    name: s.realName || s.nickname || s.username || '学生',
    studentNo: s.username || s.studentId || '—',
    lastStudy: '—',
    completedCourses: 0,
    avatar: s.avatar || '🎓'
  }
}

async function fetchClasses() {
  try {
    const data = await request.get('/teacher/classes')
    const list = Array.isArray(data) ? data : []
    classes.value = list.map((c, i) => {
      const enriched = enrichClass({ ...c, students: [] }, i)
      enriched.createdAt = (enriched.createdAt || '').slice(0, 10) || '未知'
      return enriched
    })
    // 预加载每个班级学生数
    const lists = await Promise.all(
      classes.value.map(c => request.get(`/teacher/classes/${c.id}/students`).catch(() => []))
    )
    classes.value.forEach((c, i) => {
      c.students = (Array.isArray(lists[i]) ? lists[i] : []).map(mapStudent)
      c.studentCount = c.students.length
    })
  } catch (e) {
    // 回退 FALLBACK
  }
}

/* ---------- 入班申请审批 ---------- */
const applies = ref([])
const pendingApplies = computed(() => applies.value.filter(a => a.status === 0))

async function loadApplies() {
  try {
    const list = await request.get('/teacher/classes/applies')
    applies.value = (Array.isArray(list) ? list : []).map(a => ({
      ...a,
      createdAt: (a.createdAt || '').slice(0, 16) || '—'
    }))
  } catch (e) {}
}

async function approveApply(a) {
  try {
    await request.post(`/teacher/classes/applies/${a.id}/approve`)
    ElMessage.success('已同意入班申请')
    loadApplies()
    fetchClasses()
  } catch (e) {}
}

async function rejectApply(a) {
  try {
    await request.post(`/teacher/classes/applies/${a.id}/reject`)
    ElMessage.success('已拒绝入班申请')
    loadApplies()
  } catch (e) {}
}

/* ---------- 新建 / 编辑班级 ---------- */
const createVisible = ref(false)
const editingId = ref(null)
const createForm = ref({ name: '', grade: '', description: '' })

function resetCreateForm() {
  createForm.value = { name: '', grade: '', description: '' }
}

function openCreate() {
  editingId.value = null
  resetCreateForm()
  createVisible.value = true
}

function openEdit(c) {
  editingId.value = c.id
  createForm.value = { name: c.name, grade: c.grade, description: c.description || '' }
  createVisible.value = true
}

function submitCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请填写班级名称')
    return
  }
  const payload = {
    name: createForm.value.name.trim(),
    grade: createForm.value.grade,
    description: createForm.value.description
  }
  if (editingId.value) {
    request.put(`/teacher/classes/${editingId.value}`, payload)
      .then(() => {
        ElMessage.success('班级信息已更新')
        createVisible.value = false
        fetchClasses()
      })
      .catch(() => {})
  } else {
    request.post('/teacher/classes', payload)
      .then(() => {
        ElMessage.success('班级创建成功')
        createVisible.value = false
        fetchClasses()
      })
      .catch(() => {})
  }
}

function removeClass(c) {
  ElMessageBox.confirm(`确定删除「${c.name}」吗？删除后该班学生名单将一并移除。`, '删除班级', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(() => {
      request.delete(`/teacher/classes/${c.id}`)
        .then(() => {
          classes.value = classes.value.filter(x => x.id !== c.id)
          ElMessage.success('班级已删除')
        })
        .catch(() => {})
    })
    .catch(() => {})
}

/* ---------- 导入学生 ---------- */
const importVisible = ref(false)
const importClass = ref(null)

function openImport() {
  importClass.value = null
  importVisible.value = true
}

function submitImport() {
  if (importClass.value == null) {
    ElMessage.warning('请选择要导入的班级')
    return
  }
  ElMessage.info('演示受限：需 EasyExcel 后端接入，当前为演示')
  importVisible.value = false
}

/* ---------- 学生管理 ---------- */
const studentsVisible = ref(false)
const currentClass = ref(null)

function openStudents(c) {
  currentClass.value = c
  studentsVisible.value = true
  const id = c.id
  request.get(`/teacher/classes/${id}/students`)
    .then((data) => {
      if (!currentClass.value || currentClass.value.id !== id) return
      const list = Array.isArray(data) ? data : []
      currentClass.value.students = list.map(mapStudent)
      currentClass.value.studentCount = currentClass.value.students.length
    })
    .catch(() => {})
}

function removeStudent(row) {
  if (!currentClass.value) return
  if (row.classStudentId == null) {
    ElMessage.info('演示受限：后端未返回 classStudentId，暂不支持移出')
    return
  }
  ElMessageBox.confirm(`确定将「${row.name}」移出班级吗？`, '移出班级', {
    type: 'warning',
    confirmButtonText: '移出',
    cancelButtonText: '取消'
  })
    .then(() => {
      request.delete(`/teacher/classes/stu/${row.classStudentId}`)
        .then(() => {
          currentClass.value.students = currentClass.value.students.filter(s => s.id !== row.id)
          currentClass.value.studentCount = Math.max(0, (currentClass.value.studentCount || 0) - 1)
          ElMessage.success('已移出班级')
        })
        .catch(() => {})
    })
    .catch(() => {})
}

/* ---------- 添加学生 ---------- */
const addVisible = ref(false)
const addForm = ref({ studentId: '' })
const availableStudents = ref([])

function openAddStudent() {
  addForm.value = { studentId: '' }
  addVisible.value = true
}

function submitAdd() {
  if (!currentClass.value) return
  if (!addForm.value.studentId) {
    ElMessage.warning('请选择学生')
    return
  }
  request.post(`/teacher/classes/${currentClass.value.id}/students`, { studentId: addForm.value.studentId })
    .then(() => {
      addVisible.value = false
      ElMessage.success('学生已添加')
      return request.get(`/teacher/classes/${currentClass.value.id}/students`)
    })
    .then((data) => {
      if (!currentClass.value) return
      const list = Array.isArray(data) ? data : []
      currentClass.value.students = list.map(mapStudent)
      currentClass.value.studentCount = currentClass.value.students.length
    })
    .catch(() => {})
}

async function loadAvailableStudents() {
  try {
    const data = await request.get('/teacher/qa/students')
    const list = Array.isArray(data) ? data : []
    availableStudents.value = list.map(s => ({
      studentId: s.studentId ?? s.id,
      name: s.realName || s.nickname || s.username || '学生'
    }))
  } catch (e) {}
}

onMounted(() => {
  fetchClasses()
  loadAvailableStudents()
  loadApplies()
})
</script>

<style scoped>
.t-classes .sub { color: var(--ink-2); margin: 4px 0 0; }

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.class-grid {
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}

.class-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 18px 20px 16px;
}

.cc-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  flex-shrink: 0;
}

.cc-bar.brand { background: var(--brand); }
.cc-bar.accent { background: var(--accent); }

.cc-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.cc-emoji {
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  border: 1px solid var(--line);
}

.cc-title h3 {
  margin: 0;
  font-size: 17px;
}

.cc-grade {
  font-size: 12.5px;
  color: var(--ink-3);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
}

.cc-meta {
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.cc-meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13.5px;
}

.cc-meta-item dt {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-3);
  font-weight: 500;
}

.cc-meta-item dt svg { color: var(--brand); }

.cc-meta-item dd {
  margin: 0;
  color: var(--ink);
  font-weight: 600;
}

.cc-actions {
  margin-top: auto;
  display: flex;
  gap: 8px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}

.cc-actions .el-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.cc-actions .el-button + .el-button { margin-left: 0; }

.cc-select { width: 100%; }

/* ---------- 导入 ---------- */
.imp-block {
  margin-bottom: var(--space-4);
}

.imp-label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-2);
  margin-bottom: 8px;
}

.imp-upload :deep(.el-upload-dragger) {
  border-radius: var(--radius-md);
  border: 1.5px dashed var(--brand);
  background: var(--surface-soft);
  padding: 30px 20px;
}

.imp-upload :deep(.el-upload-dragger:hover) {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.imp-upload-inner .up-txt {
  margin-top: 12px;
  color: var(--ink);
  font-size: 14.5px;
}

.imp-upload-inner .up-txt em {
  color: var(--brand);
  font-style: normal;
  font-weight: 600;
}

.imp-upload-inner .up-hint {
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 12.5px;
}

.imp-foot-bar {
  margin-top: var(--space-3);
  padding: 10px 14px;
  background: var(--surface-soft);
  border-radius: var(--radius-sm);
}

.imp-foot-note {
  font-size: 12.5px;
  color: var(--ink-3);
}

/* ---------- 学生抽屉 ---------- */
.dr-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.dr-title {
  font-family: var(--font-title);
  font-size: 17px;
  color: var(--ink);
}

.dr-sub {
  margin: 0 0 var(--space-4);
  color: var(--ink-3);
  font-size: 13px;
}

.stu-table :deep(.el-table) {
  --el-table-border-color: var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.stu-table :deep(.el-table th.el-table__cell) {
  background: var(--surface-soft);
  color: var(--ink-2);
  font-weight: 600;
}

.stu-table :deep(.el-table .el-table__row--striped td.el-table__cell) {
  background: #fbfcfe;
}

/* ---------- 入班申请 ---------- */
.apply-badge {
  background: var(--brand-grad);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 999px;
}

.apply-card {
  padding: 0;
  overflow: hidden;
}

.apply-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
}

.apply-item + .apply-item {
  border-top: 1px solid var(--line);
}

.apply-avatar {
  font-size: 30px;
  flex-shrink: 0;
}

.apply-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.apply-info b {
  font-size: 15px;
  color: var(--ink);
}

.apply-info span {
  font-size: 13px;
  color: var(--ink-3);
}

.apply-status {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 999px;
}

.apply-status.ok {
  background: #e8f7ee;
  color: #1f9d55;
}

.apply-status.no {
  background: #fdeeee;
  color: #c0392b;
}

.apply-empty {
  padding: 26px;
  text-align: center;
  color: var(--ink-3);
  font-size: 14px;
}

@media (max-width: 1000px) {
  .class-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .head-row { flex-direction: column; }
  .class-grid { grid-template-columns: 1fr; }
}
</style>
