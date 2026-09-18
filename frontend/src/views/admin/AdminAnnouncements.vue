<template>
  <div class="k-page a-ann">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><Megaphone :size="24" weight="bold" /> 公告与日志</h1>
        <p class="sub">发布平台公告与活动通知，查看系统操作日志</p>
      </div>
    </div>

    <!-- 发布公告 -->
    <section class="k-section-head">
      <h2><Megaphone :size="22" weight="bold" /> 发布公告</h2>
    </section>
    <div class="k-card pub-card">
      <el-form :model="pubForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="pubForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="类型">
            <el-select v-model="pubForm.type" class="w-full">
              <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否置顶">
            <div class="top-row">
              <el-switch v-model="pubForm.pinned" />
              <span class="top-label">{{ pubForm.pinned ? '置顶展示' : '普通展示' }}</span>
            </div>
          </el-form-item>
        </div>
        <el-form-item label="内容">
          <el-input v-model="pubForm.content" type="textarea" :rows="3" placeholder="请输入公告内容" />
        </el-form-item>
        <button class="k-btn k-btn--orange" @click="publish">
          <Megaphone :size="17" weight="bold" /> 发布
        </button>
      </el-form>
    </div>

    <!-- 公告列表 -->
    <section class="k-section-head">
      <h2><Newspaper :size="22" weight="bold" /> 公告列表</h2>
    </section>
    <div class="k-card list-card">
      <div v-for="a in announceList" :key="a.id" class="ann-item">
        <div class="ann-info">
          <div class="ann-row">
            <span class="k-tag" :class="typeClass(a.type)">{{ a.type }}</span>
            <span v-if="a.pinned" class="k-tag k-tag--orange">置顶</span>
          </div>
          <h3 class="ann-title">{{ a.title }}</h3>
          <p class="ann-content">{{ a.content }}</p>
          <span class="ann-time">{{ a.time }}</span>
        </div>
        <button class="mini-btn is-danger" @click="removeAnnounce(a)">删除</button>
      </div>
      <el-empty v-if="announceList.length === 0" description="暂无公告" />
    </div>

    <!-- 操作日志 -->
    <section class="k-section-head">
      <h2><ListBullets :size="22" weight="bold" /> 操作日志</h2>
    </section>
    <div class="k-card table-card">
      <div class="log-toolbar">
        <el-input
          v-model="logKeyword"
          placeholder="搜索操作人"
          clearable
          class="search"
        >
          <template #prefix><MagnifyingGlass :size="16" weight="bold" /></template>
        </el-input>
      </div>
      <el-table :data="logs" style="width: 100%">
        <el-table-column prop="time" label="时间" width="165" />
        <el-table-column label="操作人" width="160">
          <template #default="{ row }">
            <span class="u-name">{{ row.user }}</span>
            <span class="role">{{ row.role }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" width="110" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="opClass(row.op)">{{ row.op }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="230" />
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
      <div class="table-foot">
        <span class="total">共 {{ logTotal }} 条</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="logTotal"
          :page-size="logPageSize"
          v-model:current-page="logPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhMegaphone as Megaphone,
  PhNewspaper as Newspaper,
  PhListBullets as ListBullets,
  PhMagnifyingGlass as MagnifyingGlass
} from '@phosphor-icons/vue'

const typeOptions = ['公告', '活动', '维护']

// 发布公告表单
const pubForm = ref({ title: '', type: '公告', content: '', pinned: false })

// ---- 回退数据（后端不可用时保底）----
const FALLBACK_ANNOUNCEMENTS = [
  { id: 1, title: '新学期 AI 挑战赛开始啦！', type: '活动', content: '完成 3 个 AI 实验即可获得「未来科学家」徽章，还能赢取积分奖励哦～', pinned: true, time: '2026-09-01' },
  { id: 2, title: '新增课程《认识机器学习》', type: '公告', content: '新课程《认识机器学习》已上线，欢迎大家来学习！', pinned: false, time: '2026-08-28' },
  { id: 3, title: '本周六系统维护通知', type: '维护', content: '本周六 22:00-24:00 系统维护，请合理安排学习时间。', pinned: false, time: '2026-08-25' }
]
const announceList = ref(FALLBACK_ANNOUNCEMENTS.map((a) => ({ ...a })))

function normalizeAnnounce(a) {
  return {
    id: a.id,
    title: a.title || '',
    type: a.tag || a.type || '公告',
    content: a.content || '',
    pinned: !!a.isTop,
    time: a.createdAt ? String(a.createdAt).slice(0, 10) : ''
  }
}

async function fetchAnnouncements() {
  try {
    const data = await request.get('/admin/announcements')
    announceList.value = (Array.isArray(data) ? data : []).map(normalizeAnnounce)
  } catch (e) { /* 回退 mock */ }
}

const typeClass = (t) => (t === '活动' ? 'k-tag--orange' : t === '维护' ? 'k-tag--pink' : 'k-tag--teal')

const publish = async () => {
  const f = pubForm.value
  if (!f.title.trim() || !f.content.trim()) {
    ElMessage.warning('请填写公告标题与内容')
    return
  }
  try {
    await request.post('/admin/announcements', {
      title: f.title.trim(),
      content: f.content.trim(),
      tag: f.type,
      isTop: f.pinned ? 1 : 0
    })
    pubForm.value = { title: '', type: '公告', content: '', pinned: false }
    ElMessage.success('公告发布成功')
    fetchAnnouncements()
  } catch (e) { /* 拦截器已提示 */ }
}

const removeAnnounce = async (a) => {
  try {
    await request.delete(`/admin/announcements/${a.id}`)
    ElMessage.success('公告已删除')
    fetchAnnouncements()
  } catch (e) { /* 拦截器已提示 */ }
}

// 操作日志
const logKeyword = ref('')
const logPage = ref(1)
const logPageSize = ref(8)

const FALLBACK_LOGS = [
  { id: 1, time: '2026-09-01 10:22', user: '王老师', role: '(教师)', op: '下架', detail: '课程《旧版 AI 入门》', ip: '192.168.1.24' },
  { id: 2, time: '2026-08-31 16:05', user: '管理员', role: '(管理员)', op: '配置', detail: 'DeepSeek API Key 已轮换', ip: '192.168.1.12' },
  { id: 3, time: '2026-08-30 14:48', user: '管理员', role: '(管理员)', op: '导入', detail: '五年级(2)班 42 名学生', ip: '192.168.1.12' },
  { id: 4, time: '2026-08-30 11:03', user: '秦老师', role: '(教师)', op: '上架', detail: '课程《什么是人工智能？》', ip: '192.168.1.36' },
  { id: 5, time: '2026-08-29 09:41', user: '刘老师', role: '(教师)', op: '更新', detail: '实验《小星星循环舞》模板', ip: '192.168.1.40' },
  { id: 6, time: '2026-08-28 15:27', user: '管理员', role: '(管理员)', op: '发布', detail: '公告《认识机器学习》', ip: '192.168.1.12' },
  { id: 7, time: '2026-08-27 20:15', user: '陈老师', role: '(教师)', op: '禁用', detail: '学生账号「xiaoting」', ip: '192.168.1.58' },
  { id: 8, time: '2026-08-27 08:30', user: '管理员', role: '(管理员)', op: '删除', detail: '过期活动公告', ip: '192.168.1.12' }
]
const logs = ref(FALLBACK_LOGS.slice(0, logPageSize))
const logTotal = ref(FALLBACK_LOGS.length)

function roleLabel(r) {
  if (r === 0 || r === '0' || r === 'admin' || r === '管理员') return '管理员'
  if (r === 1 || r === '1' || r === 'teacher' || r === '教师') return '教师'
  if (r === 2 || r === '2' || r === 'student' || r === '学生') return '学生'
  return r != null ? String(r) : ''
}

function normalizeLog(l) {
  const role = roleLabel(l.operatorRole)
  return {
    id: l.id,
    time: l.createdAt ? (String(l.createdAt).length >= 16 ? String(l.createdAt).slice(0, 16) : String(l.createdAt)) : '',
    user: l.operatorName || l.operator || l.user || (role ? `${role}#${l.operatorId ?? ''}` : '—'),
    role: role ? `(${role})` : '',
    op: l.opType || l.op || '操作',
    detail: l.detail || '',
    ip: l.ip || ''
  }
}

function loadLogFallback() {
  const k = logKeyword.value.trim().toLowerCase()
  const filtered = FALLBACK_LOGS.filter((l) => !k || l.user.toLowerCase().includes(k))
  logTotal.value = filtered.length
  const start = (logPage.value - 1) * logPageSize.value
  logs.value = filtered.slice(start, start + logPageSize.value)
}

async function fetchLogs() {
  try {
    const params = { page: logPage.value, size: logPageSize.value }
    if (logKeyword.value.trim()) params.operator = logKeyword.value.trim()
    const data = await request.get('/admin/logs', { params })
    const records = data && Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    logs.value = records.map(normalizeLog)
    logTotal.value = data && data.total != null ? data.total : records.length
  } catch (e) {
    loadLogFallback()
  }
}

const opClass = (op) => {
  if (op === '发布' || op === '上架') return 'k-tag--orange'
  if (op === '下架' || op === '删除' || op === '禁用') return 'k-tag--pink'
  if (op === '更新' || op === '配置') return 'k-tag--teal'
  return 'k-tag'
}

watch(logKeyword, () => {
  logPage.value = 1
})
watch([logKeyword, logPage], fetchLogs)

onMounted(() => {
  fetchAnnouncements()
  fetchLogs()
})
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

.k-section-head h2 svg { color: var(--brand); }

.pub-card .el-form-item { margin-bottom: 18px; }

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-row .el-select,
.w-full { width: 100%; }

.top-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 32px;
}

.top-label { font-size: 13.5px; color: var(--ink-2); }

.list-card { padding: 0 var(--space-2); }

.ann-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: var(--space-5) var(--space-4) 0;
}

.ann-item + .ann-item { padding-top: var(--space-5); }

.ann-item:last-child { padding-bottom: var(--space-4); }

.ann-info { flex: 1; }

.ann-row { display: flex; gap: 8px; margin-bottom: 8px; }

.ann-title {
  margin: 0 0 6px;
  font-size: 16.5px;
}

.ann-content {
  margin: 0 0 8px;
  color: var(--ink-2);
  font-size: 13.5px;
  line-height: 1.6;
}

.ann-time { font-size: 12px; color: var(--ink-3); }

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
  flex-shrink: 0;
}

.mini-btn:hover { background: var(--brand); color: #fff; }

.mini-btn.is-danger { background: #fceaf1; color: #b03a63; }
.mini-btn.is-danger:hover { background: #e05c8a; color: #fff; }

.table-card { padding: 0; overflow: hidden; }

.log-toolbar { padding: 16px 20px; }

.search { width: 240px; }

.u-name { font-weight: 600; color: var(--ink); }

.role { margin-left: 6px; font-size: 12px; color: var(--ink-3); }

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
