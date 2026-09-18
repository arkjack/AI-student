<template>
  <div class="k-page a-courses">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><BookOpen :size="24" weight="bold" /> 课程管理</h1>
        <p class="sub">课程上架、编辑、下架与分类管理，支持抖音视频链接嵌入</p>
      </div>
    </div>

    <!-- 筛选与操作 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="chips">
          <span
            v-for="c in catFilters"
            :key="c"
            class="chip"
            :class="{ active: activeCat === c }"
            @click="activeCat = c"
          >{{ c }}</span>
        </div>
        <div class="chips">
          <span
            v-for="s in statusFilters"
            :key="s.value"
            class="chip"
            :class="{ active: activeStatus === s.value }"
            @click="activeStatus = s.value"
          >{{ s.label }}</span>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索课程标题"
          clearable
          class="search"
        >
          <template #prefix><MagnifyingGlass :size="16" weight="bold" /></template>
        </el-input>
      </div>
      <button class="k-btn k-btn--orange" @click="openCreate">
        <Plus :size="17" weight="bold" /> 新增课程
      </button>
    </div>

    <!-- 列表 -->
    <div class="k-card table-card">
      <el-table :data="courses" style="width: 100%">
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <img class="cover" :src="coverSrc(row)" :alt="row.title" />
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="190">
          <template #default="{ row }"><span class="c-title">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }"><span class="k-tag k-tag--teal">{{ row.category }}</span></template>
        </el-table-column>
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="diffClass(row.difficulty)">{{ row.difficulty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="100" align="center">
          <template #default="{ row }"><span class="muted">{{ row.duration }} 分钟</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="row.online ? 'k-tag--green' : ''">
              {{ row.online ? '已上架' : '已下架' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="观看数" width="100" align="center">
          <template #default="{ row }"><span class="muted">{{ row.views }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="right">
          <template #default="{ row }">
            <div class="ops">
              <button class="mini-btn" @click="openEdit(row)">编辑</button>
              <button
                class="mini-btn"
                :class="row.online ? 'is-warn' : 'is-ok'"
                @click="toggleOnline(row)"
              >{{ row.online ? '下架' : '上架' }}</button>
              <button class="mini-btn is-danger" @click="removeCourse(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="courses.length === 0" description="没有符合条件的课程" />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVis" :title="dialogMode" width="560px" @closed="resetForm">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入课程标题" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="form.category" placeholder="选择分类" class="w-full">
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" placeholder="选择难度" class="w-full">
              <el-option v-for="d in difficultyOptions" :key="d" :label="d" :value="d" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="form.duration" :min="5" :max="240" :step="5" controls-position="right" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="简要介绍课程内容" />
        </el-form-item>
        <el-form-item label="抖音视频链接">
          <el-input
            v-model="form.videoUrl"
            placeholder="粘贴抖音分享的复制的链接"
            clearable
          >
            <template #prefix><VideoCamera :size="16" weight="bold" /></template>
          </el-input>
          <p class="field-hint">复制抖音视频分享文案中的链接粘贴到此处，保存后自动嵌入。</p>
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-box">
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" :alt="form.title" />
            <span class="cover-note">使用平台封面库</span>
          </div>
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="form.online" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="dialogVis = false">取消</button>
        <button class="k-btn" @click="saveCourse">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import {
  PhBookOpen as BookOpen,
  PhPlus as Plus,
  PhMagnifyingGlass as MagnifyingGlass,
  PhVideoCamera as VideoCamera
} from '@phosphor-icons/vue'
import imgCourse1 from '@/assets/images/course-1.jpg'
import imgCourse2 from '@/assets/images/course-2.jpg'
import imgCourse3 from '@/assets/images/course-3.jpg'
import imgCourse4 from '@/assets/images/course-4.jpg'
import imgCourse5 from '@/assets/images/course-5.jpg'
import imgCourse6 from '@/assets/images/course-6.jpg'

const catFilters = ['全部', 'AI 入门', '图形化编程', '机器学习', 'AI 与生活', '趣味实验']
const statusFilters = [
  { label: '全部', value: 'all' },
  { label: '已上架', value: 'online' },
  { label: '已下架', value: 'offline' }
]
const categoryOptions = catFilters.slice(1)
const difficultyOptions = ['入门', '进阶', '挑战']

// 分类名称 → 分类 id（用于服务端筛选与提交）
const CAT_ID = { 'AI 入门': 1, '图形化编程': 2, '机器学习': 3, 'AI 与生活': 4, '趣味实验': 5 }
const CAT_NAME = { 1: 'AI 入门', 2: '图形化编程', 3: '机器学习', 4: 'AI 与生活', 5: '趣味实验' }
const DIFF_ID = { '入门': 1, '进阶': 2, '挑战': 3 }
const catId = (name) => {
  if (name == null) return null
  if (CAT_ID[name] != null) return CAT_ID[name]
  if (typeof name === 'number') return name
  return null
}

const activeCat = ref('全部')
const activeStatus = ref('all')
const keyword = ref('')

// ---- 回退数据（后端不可用时保底）----
const FALLBACK_COURSES = [
  { id: 1, title: '什么是人工智能？', category: 'AI 入门', difficulty: '入门', duration: 18, online: true, views: 1284, summary: '和机器人小智一起认识 AI 世界的奇妙旅程！', videoUrl: 'https://v.douyin.com/xxxxx1', cover: imgCourse1 },
  { id: 2, title: '有趣的图像识别', category: 'AI 入门', difficulty: '进阶', duration: 25, online: true, views: 986, summary: '为什么手机能认出你的脸？图像识别原理大揭秘。', videoUrl: 'https://v.douyin.com/xxxxx2', cover: imgCourse2 },
  { id: 3, title: '方块编程第一课', category: '图形化编程', difficulty: '入门', duration: 30, online: true, views: 1523, summary: '拖一拖、拼一拼，用积木让角色动起来！', videoUrl: 'https://v.douyin.com/xxxxx3', cover: imgCourse3 },
  { id: 4, title: '掌握机器学习', category: '机器学习', difficulty: '进阶', duration: 45, online: false, views: 643, summary: '从数据中学习规律，认识机器学习的基本方法。', videoUrl: 'https://v.douyin.com/xxxxx4', cover: imgCourse4 },
  { id: 5, title: 'AI 陪伴生活', category: 'AI 与生活', difficulty: '入门', duration: 20, online: false, views: 872, summary: '发现身边的 AI，看它如何悄悄改变我们的日常。', videoUrl: 'https://v.douyin.com/xxxxx5', cover: imgCourse5 },
  { id: 6, title: '火山喷发小实验', category: '趣味实验', difficulty: '挑战', duration: 15, online: true, views: 1204, summary: '动手做一场火山喷发，理解背后的化学反应。', videoUrl: 'https://v.douyin.com/xxxxx6', cover: imgCourse6 }
]

const courses = ref([])
const COVER_MAP = { 1: imgCourse1, 2: imgCourse2, 3: imgCourse3, 4: imgCourse4, 5: imgCourse5, 6: imgCourse6 }

const normDifficulty = (d) => {
  const map = { 1: '入门', 2: '进阶', 3: '挑战' }
  return map[d] || d || '入门'
}

const statusToOnline = (s) => s === 'online' || s === true || s === 1 || s === 'published' || s === '上架'

function normalizeCourse(c) {
  return {
    id: c.id,
    title: c.title || '',
    category: c.categoryName || CAT_NAME[c.categoryId] || c.category || '',
    categoryId: c.categoryId,
    difficulty: normDifficulty(c.difficulty),
    duration: c.durationMinutes ?? c.duration ?? 0,
    online: statusToOnline(c.status),
    views: c.views ?? 0,
    summary: c.summary || '',
    videoUrl: c.videoUrl || '',
    cover: c.coverImage || '',
    coverImage: c.coverImage || '',
    coverEmoji: c.coverEmoji || '',
    teacher: c.teacher || '',
    status: c.status
  }
}

// 封面兜底：coverImage 无值时用课程 id 映射本地素材，存在则显示
function coverSrc(row) {
  if (row.coverImage) return row.coverImage
  if (COVER_MAP[row.id]) return COVER_MAP[row.id]
  return imgCourse1
}

function fallbackCourses() {
  const kw = keyword.value.trim().toLowerCase()
  return FALLBACK_COURSES.filter((c) => {
    const catOk = activeCat.value === '全部' || c.category === activeCat.value
    const statusOk = activeStatus.value === 'all' || (activeStatus.value === 'online' ? c.online : !c.online)
    const kwOk = !kw || c.title.toLowerCase().includes(kw)
    return catOk && statusOk && kwOk
  })
}

async function fetchCourses() {
  try {
    const params = {}
    if (activeCat.value !== '全部') {
      const id = catId(activeCat.value)
      if (id != null) params.categoryId = id
    }
    if (activeStatus.value !== 'all') params.status = activeStatus.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await request.get('/admin/courses', { params })
    courses.value = (Array.isArray(data) ? data : []).map(normalizeCourse)
  } catch (e) {
    courses.value = fallbackCourses()
  }
}

watch([activeCat, activeStatus, keyword], fetchCourses, { immediate: true })

const diffClass = (d) => (d === '入门' ? 'k-tag--teal' : d === '进阶' ? 'k-tag--orange' : 'k-tag--pink')

// 弹窗逻辑
const dialogVis = ref(false)
const dialogMode = ref('新增课程')
const form = ref({})

const resetForm = () => {
  form.value = {
    id: null,
    title: '',
    category: 'AI 入门',
    difficulty: '入门',
    duration: 20,
    summary: '',
    videoUrl: '',
    coverImage: '',
    coverEmoji: '',
    online: true
  }
}

const openCreate = () => {
  dialogMode.value = '新增课程'
  resetForm()
  dialogVis.value = true
}

const openEdit = (row) => {
  dialogMode.value = '编辑课程'
  form.value = {
    id: row.id,
    title: row.title,
    category: row.category,
    difficulty: row.difficulty,
    duration: row.duration,
    summary: row.summary,
    videoUrl: row.videoUrl,
    coverImage: row.coverImage || '',
    coverEmoji: row.coverEmoji || '',
    online: row.online
  }
  dialogVis.value = true
}

function toPayload(src) {
  return {
    title: src.title || '',
    categoryId: catId(src.category) ?? src.categoryId ?? null,
    difficulty: DIFF_ID[src.difficulty] ?? src.difficulty ?? 1,
    durationMinutes: src.duration ?? src.durationMinutes ?? 0,
    coverEmoji: src.coverEmoji || '',
    coverImage: src.coverImage || '',
    summary: src.summary || '',
    videoUrl: src.videoUrl || '',
    status: src.online ? 1 : 0
  }
}

const saveCourse = async () => {
  const f = form.value
  if (!f.title.trim()) {
    ElMessage.warning('请填写课程标题')
    return
  }
  try {
    if (dialogMode.value === '新增课程') {
      await request.post('/admin/courses', toPayload(f))
      ElMessage.success('课程创建成功')
    } else {
      await request.put(`/admin/courses/${f.id}`, toPayload(f))
      ElMessage.success('课程信息已更新')
    }
    dialogVis.value = false
    fetchCourses()
  } catch (e) { /* 拦截器已提示 */ }
}

const toggleOnline = async (row) => {
  const next = !row.online
  try {
    const payload = toPayload(row)
    payload.status = next ? 1 : 0
    await request.put(`/admin/courses/${row.id}`, payload)
    row.online = next
    ElMessage.success(next ? `「${row.title}」已上架` : `「${row.title}」已下架`)
  } catch (e) { /* 拦截器已提示 */ }
}

const removeCourse = (row) => {
  ElMessageBox.confirm(`确定删除课程「${row.title}」吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await request.delete(`/admin/courses/${row.id}`)
        ElMessage.success('课程已删除')
        fetchCourses()
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

.chips { display: flex; gap: 8px; flex-wrap: wrap; }

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

.search { width: 220px; }

.table-card { padding: 0; overflow: hidden; }

.cover {
  width: 64px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
}

.c-title { font-weight: 600; color: var(--ink); }
.muted { color: var(--ink-3); }

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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-row .el-select,
.w-full { width: 100%; }

.field-hint {
  font-size: 12px;
  color: var(--ink-3);
  margin: 6px 0 0;
}

.cover-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover-preview {
  width: 128px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--line);
}

.cover-note { font-size: 13px; color: var(--ink-3); }

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
