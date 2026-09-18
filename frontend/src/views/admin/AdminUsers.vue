<template>
  <div class="k-page a-users">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><Users :size="24" weight="bold" /> 用户管理</h1>
        <p class="sub">管理学生、教师账号，按角色分配操作权限</p>
      </div>
    </div>

    <!-- 筛选与操作 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="tabs">
          <span
            v-for="t in roleTabs"
            :key="t.value"
            class="chip"
            :class="{ active: activeRole === t.value }"
            @click="activeRole = t.value"
          >{{ t.label }}</span>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索姓名 / 用户名"
          clearable
          class="search"
        >
          <template #prefix><MagnifyingGlass :size="16" weight="bold" /></template>
        </el-input>
      </div>
      <button class="k-btn k-btn--orange" @click="openCreate">
        <UserPlus :size="17" weight="bold" /> 新增账号
      </button>
    </div>

    <!-- 列表 -->
    <div class="k-card table-card">
      <el-table :data="users" style="width: 100%">
        <el-table-column label="姓名" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar">{{ row.avatar }}</span>
              <span class="u-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column :label="activeRole === 'student' ? '班级' : '学科'" min-width="130">
          <template #default="{ row }">
            <span class="k-tag">{{ activeRole === 'student' ? row.className : row.subject }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="row.status === 'active' ? 'k-tag--green' : ''">
              {{ row.status === 'active' ? '激活' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="150" />
        <el-table-column label="操作" width="200" align="right">
          <template #default="{ row }">
            <div class="ops">
              <button class="mini-btn" @click="openEdit(row)">编辑</button>
              <button
                class="mini-btn"
                :class="row.status === 'active' ? 'is-warn' : 'is-ok'"
                @click="toggleStatus(row)"
              >{{ row.status === 'active' ? '禁用' : '启用' }}</button>
              <button class="mini-btn is-danger" @click="removeUser(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-foot">
        <span class="total">共 {{ total }} 条记录</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="page"
        />
      </div>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVis" :title="dialogMode" width="460px" @closed="resetForm">
      <el-form :model="form" label-position="top">
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio value="student">学生</el-radio>
            <el-radio value="teacher">教师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="form.role === 'student'" label="班级">
          <el-select v-model="form.classId" placeholder="选择班级（可搜索）" clearable filterable>
            <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="学科">
          <el-input v-model="form.subject" placeholder="如：信息技术" />
        </el-form-item>
        <el-form-item v-if="dialogMode === '新增账号'" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码（6-20位）" />
        </el-form-item>
        <el-form-item v-else label="重置密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="dialogVis = false">取消</button>
        <button class="k-btn" @click="saveUser">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { PhUsers as Users, PhUserPlus as UserPlus, PhMagnifyingGlass as MagnifyingGlass } from '@phosphor-icons/vue'

// 后端角色编码：0=管理员 1=教师 2=学生
const ROLE_CODE = { student: 2, teacher: 1 }
const codeToRole = { 2: 'student', 1: 'teacher' }

const roleTabs = [
  { label: '学生', value: 'student' },
  { label: '教师', value: 'teacher' }
]
const activeRole = ref('student')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(8)

// ---- 回退数据（后端不可用时保底）----
const FALLBACK_STUDENTS = [
  { id: 1, role: 'student', name: '小明', username: 'xiaoming', className: '五年级(2)班', avatar: '🦊', status: 'active', createdAt: '2025-09-01' },
  { id: 2, role: 'student', name: '小红', username: 'xiaohong', className: '五年级(2)班', avatar: '🐰', status: 'active', createdAt: '2025-09-01' },
  { id: 3, role: 'student', name: '小刚', username: 'xiaogang', className: '四年级(1)班', avatar: '🐯', status: 'active', createdAt: '2025-09-05' },
  { id: 4, role: 'student', name: '小丽', username: 'xiaoli', className: '四年级(1)班', avatar: '🐱', status: 'active', createdAt: '2025-09-05' },
  { id: 5, role: 'student', name: '小强', username: 'xiaoqiang', className: '六年级(3)班', avatar: '🐻', status: 'active', createdAt: '2025-09-12' },
  { id: 6, role: 'student', name: '小美', username: 'xiaomei', className: '六年级(3)班', avatar: '🐨', status: 'active', createdAt: '2025-09-12' },
  { id: 7, role: 'student', name: '小宇', username: 'xiaoyu', className: '三年级(1)班', avatar: '🐸', status: 'active', createdAt: '2025-09-20' },
  { id: 8, role: 'student', name: '小婷', username: 'xiaoting', className: '五年级(4)班', avatar: '🐼', status: 'disabled', createdAt: '2025-09-20' }
]

const FALLBACK_TEACHERS = [
  { id: 1, role: 'teacher', name: '秦老师', username: 'qinteacher', subject: '信息技术', avatar: '🦉', status: 'active', createdAt: '2025-08-20' },
  { id: 2, role: 'teacher', name: '刘老师', username: 'liuteacher', subject: '科学', avatar: '🦄', status: 'active', createdAt: '2025-08-22' },
  { id: 3, role: 'teacher', name: '王老师', username: 'wangteacher', subject: '数学', avatar: '🐳', status: 'active', createdAt: '2025-08-25' },
  { id: 4, role: 'teacher', name: '陈老师', username: 'chenteacher', subject: '编程', avatar: '🐢', status: 'disabled', createdAt: '2025-08-28' }
]

const users = ref([])
const total = ref(0)

// 将后端记录归一化为列表行结构
function normalizeUser(u) {
  const roleCode = Number(u.role)
  const role = codeToRole[roleCode] || u.role || 'student'
  return {
    id: u.id,
    role,
    name: u.realName || u.nickname || u.username || '',
    username: u.username || '',
    nickname: u.nickname || '',
    avatar: u.avatar || (role === 'teacher' ? '🦉' : '🐱'),
    className: role === 'student' ? (u.className || '') : '',
    classId: role === 'student' ? (u.classId ?? null) : null,
    subject: role === 'teacher' ? (u.subject || '') : '',
    status: Number(u.status) === 0 ? 'disabled' : 'active',
    createdAt: u.createdAt || ''
  }
}

function loadFallback() {
  const list = activeRole.value === 'student' ? FALLBACK_STUDENTS : FALLBACK_TEACHERS
  const kw = keyword.value.trim().toLowerCase()
  const filtered = list.filter(
    (u) => !kw || u.name.toLowerCase().includes(kw) || u.username.toLowerCase().includes(kw)
  )
  total.value = filtered.length
  const start = (page.value - 1) * pageSize.value
  users.value = filtered.slice(start, start + pageSize.value)
}

async function fetchUsers() {
  try {
    const params = { page: page.value, size: pageSize.value }
    if (ROLE_CODE[activeRole.value] != null) params.role = ROLE_CODE[activeRole.value]
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await request.get('/admin/users', { params })
    const records = data && Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    users.value = records.map(normalizeUser)
    total.value = data && data.total != null ? data.total : records.length
  } catch (e) {
    loadFallback()
  }
}

// 切换角色 / 关键字搜索时重置页码，筛选/分页均服务端处理
watch([activeRole, keyword], () => { page.value = 1 })
watch([activeRole, keyword, page], fetchUsers, { immediate: true })

// 弹窗逻辑
const dialogVis = ref(false)
const dialogMode = ref('新增账号')
const form = ref({})

// 班级列表（新增/编辑选班用，与注册页同源）
const classOptions = ref([])
const loadClasses = async () => {
  try {
    const list = await request.get('/auth/classes')
    classOptions.value = Array.isArray(list) ? list : []
  } catch (e) {}
}
loadClasses()

const resetForm = () => {
  form.value = { id: null, role: 'student', username: '', realName: '', nickname: '', className: '', classId: null, subject: '', password: '' }
}

const openCreate = () => {
  dialogMode.value = '新增账号'
  resetForm()
  dialogVis.value = true
}

const openEdit = (row) => {
  dialogMode.value = '编辑账号'
  form.value = {
    id: row.id,
    role: row.role,
    username: row.username,
    realName: row.name,
    nickname: row.nickname || '',
    className: row.className || '',
    classId: row.classId ?? null,
    subject: row.subject || '',
    password: ''
  }
  dialogVis.value = true
}

const saveUser = async () => {
  const f = form.value
  if (!f.realName.trim() || !f.username.trim()) {
    ElMessage.warning('请填写用户名与姓名')
    return
  }
  try {
    if (dialogMode.value === '新增账号') {
      await request.post('/admin/users', {
        role: ROLE_CODE[f.role] ?? f.role,
        username: f.username.trim(),
        realName: f.realName.trim(),
        nickname: f.nickname.trim() || f.realName.trim(),
        subject: f.role === 'teacher' ? f.subject : '',
        className: f.role === 'student' ? f.className : '',
        classId: f.role === 'student' ? f.classId : null,
        password: f.password
      })
      ElMessage.success('账号创建成功')
    } else {
      await request.put(`/admin/users/${f.id}`, {
        realName: f.realName.trim(),
        nickname: f.nickname.trim() || f.realName.trim(),
        subject: f.role === 'teacher' ? f.subject : '',
        classId: f.role === 'student' ? f.classId : null,
        password: f.password || ''
      })
      ElMessage.success('账号信息已更新')
    }
    dialogVis.value = false
    fetchUsers()
  } catch (e) { /* 拦截器已提示 */ }
}

const toggleStatus = async (row) => {
  const toEnable = row.status !== 'active'
  try {
    await request.put(`/admin/users/${row.id}/status`, { status: toEnable ? 1 : 0 })
    row.status = toEnable ? 'active' : 'disabled'
    ElMessage.success(toEnable ? `已启用「${row.name}」` : `已禁用「${row.name}」`)
  } catch (e) { /* 拦截器已提示 */ }
}

const removeUser = (row) => {
  if (row.role === 'admin' || row.role === 0 || row.role === '0') {
    ElMessage.warning('管理员账号不可删除')
    return
  }
  ElMessageBox.confirm(`确定删除用户「${row.name}」吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await request.delete(`/admin/users/${row.id}`)
        ElMessage.success('用户已删除')
        fetchUsers()
      } catch (e) { /* 拦截器已提示 */ }
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.page-head h1 {
  margin: 4px 0 6px;
  font-size: 30px;
}

.sub { color: var(--ink-2); margin: 0; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.tabs { display: flex; gap: 8px; }

.chip {
  border: none;
  font-family: var(--font-body);
  padding: 7px 20px;
  border-radius: 999px;
  font-size: 14px;
  background: #fff;
  color: var(--ink-2);
  cursor: pointer;
  font-weight: 700;
  box-shadow: var(--shadow-card);
  transition: all 0.18s;
}

.chip:hover { color: var(--brand); }

.chip.active { background: var(--brand); color: #fff; }

.search { width: 250px; }

.table-card { padding: 0; overflow: hidden; }

.user-cell { display: flex; align-items: center; gap: 10px; }

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.u-name { font-weight: 600; color: var(--ink); }

.ops { display: inline-flex; gap: 6px; }

.mini-btn {
  border: none;
  background: var(--brand-soft);
  color: var(--brand-deep);
  border-radius: 999px;
  padding: 5px 13px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s;
}

.mini-btn:hover { background: var(--brand); color: #fff; }

.mini-btn.is-warn { background: var(--accent-soft); color: var(--accent-deep); }
.mini-btn.is-warn:hover { background: var(--accent); color: #fff; }

.mini-btn.is-ok { background: #e4f4ec; color: #1f7a52; }
.mini-btn.is-ok:hover { background: #2e9e6b; color: #fff; }

.mini-btn.is-danger { background: #fceaf1; color: #b03a63; }
.mini-btn.is-danger:hover { background: #e05c8a; color: #fff; }

.table-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-top: 1px solid var(--line);
}

.total { font-size: 13px; color: var(--ink-3); }

:deep(.el-table) {
  --el-table-row-hover-bg-color: #eef3fc;
  --el-table-border-color: var(--line);
  font-size: 13.5px;
  color: var(--ink);
}

:deep(.el-table th.el-table__cell) {
  background: var(--surface-soft);
  color: var(--ink-2);
  font-weight: 600;
  font-size: 13px;
}
</style>
