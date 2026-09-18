<template>
  <div class="k-page t-review">
    <div class="page-head">
      <h1 class="k-h1-icon"><NotePencil :size="24" weight="bold" /> 作业批改</h1>
      <p class="sub">查看学生提交的编程作品、实验成果与作业内容，打分与点评</p>
    </div>

    <!-- 汇总数据条 -->
    <section class="ov-strip">
      <div class="ov-item">
        <span class="ov-icon orange"><NotePencil :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ pendingCount }}</b><span>待批改</span></div>
      </div>
      <div class="ov-item">
        <span class="ov-icon green"><CheckCircle :size="19" weight="bold" /></span>
        <div class="ov-num"><b>{{ gradedCount }}</b><span>已批改</span></div>
      </div>
    </section>

    <!-- 提交列表 -->
    <section class="k-card">
      <div class="rv-filters">
        <el-select v-model="filterAssignment" class="rv-select">
          <el-option label="全部任务" :value="null" />
          <el-option v-for="a in assignmentOptions" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
        <div class="rv-chips">
          <span
            v-for="st in statusOptions"
            :key="st"
            class="chip"
            :class="{ active: filterStatus === st }"
            @click="filterStatus = st"
          >{{ st }}</span>
        </div>
        <el-input v-model="filterSearch" placeholder="搜索学生 / 任务..." clearable class="rv-search">
          <template #prefix><MagnifyingGlass :size="16" /></template>
        </el-input>
      </div>

      <el-table :data="submissions" @row-click="openReview" class="rv-table">
        <el-table-column label="学生" width="170">
          <template #default="{ row }">
            <div class="stu-cell">
              <span class="stu-avatar">{{ row.avatar }}</span>
              <b>{{ row.name }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="task" label="任务" min-width="200" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <span class="k-tag" :class="typeTag(row.type)">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="提交时间" width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="k-tag" :class="row.status === '已批改' ? 'k-tag--green' : 'k-tag--orange'">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click.stop="openReview(row)">
              {{ row.status === '待批改' ? '去批改' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 批改抽屉 -->
    <el-drawer v-model="drawerOpen" size="480px" :with-header="false">
      <div v-if="current" class="rv-detail">
        <div class="rv-hero">
          <span class="rv-avatar">{{ current.avatar }}</span>
          <div class="rv-hero-info">
            <b>{{ current.name }}</b>
            <div class="rv-sub">{{ current.className }} · {{ current.task }}</div>
          </div>
          <span class="k-tag" :class="current.status === '已批改' ? 'k-tag--green' : 'k-tag--orange'">{{ current.status }}</span>
        </div>

        <!-- 提交内容 -->
        <div class="rv-block">
          <div class="rv-block-head"><h4>{{ typeLabel(current) }}</h4></div>
          <pre v-if="current.type === '编程作品'" class="code-block">{{ current.content }}</pre>
          <p v-else class="qa-text">{{ current.content }}</p>
        </div>

        <!-- 评分 -->
        <div class="rv-block">
          <div class="rv-block-head"><h4>评分</h4></div>
          <div class="rv-rate">
            <el-rate v-model="stars" allow-half />
            <el-input-number v-model="score" :min="0" :max="100" :step="1" />
          </div>
        </div>

        <!-- 点评 -->
        <div class="rv-block">
          <div class="rv-block-head"><h4>点评</h4></div>
          <el-input v-model="comment" type="textarea" :rows="4" placeholder="写下你的点评..." />
          <div v-if="current.status === '已批改'" class="rv-comment-show">
            <span class="rv-comment-label">已有点评：</span>{{ current.comment }}
          </div>
        </div>

        <button class="k-btn k-btn--orange rv-submit" @click="submitReview">
          <Check :size="18" weight="bold" /> 提交批改
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhNotePencil as NotePencil,
  PhCheckCircle as CheckCircle,
  PhMagnifyingGlass as MagnifyingGlass,
  PhCheck as Check
} from '@phosphor-icons/vue'

const FALLBACK_ASSIGNMENTS = [
  { id: 'a1', name: '积木作品：小星星循环舞' },
  { id: 'a2', name: 'AI 写作：我的未来学校' },
  { id: 'a3', name: '课程问答：什么是人工智能' }
]

// ===== FALLBACK 数据：8 条提交（4 待批改 + 4 已批改） =====
const FALLBACK_SUBMISSIONS = [
  { id: 1, classId: 'c1', className: '五年级(1)班', name: '小明', avatar: '🦊', task: '积木作品：小星星循环舞', type: '编程作品', time: '今天 09:12', status: '待批改', score: null, comment: '', content: `when clicked\n  repeat 5 {\n    turn right (72·2)°\n    move (100) steps\n  }\nplay sound (tada)°`, },
  { id: 2, classId: 'c1', className: '五年级(1)班', name: '朵朵', avatar: '🐰', task: 'AI 写作：我的未来学校', type: 'AI 写作', time: '今天 08:47', status: '待批改', score: null, comment: '', content: '我的未来学校会有一栋会飞的图书馆，我们用积木搭建出会发光的校园，教室的窗户可以看到星星。' },
  { id: 3, classId: 'c1', className: '五年级(1)班', name: '小航', avatar: '🐼', task: '课程问答：什么是人工智能', type: '课程问答', time: '昨天 19:03', status: '待批改', score: null, comment: '', content: '人工智能就是让计算机学会像人一样思考和做事，比如帮我们识别图片、听懂说话、做数学题。' },
  { id: 4, classId: 'c2', className: '五年级(2)班', name: '糖糖', avatar: '🐱', task: '编程作品：会跳舞的小精灵', type: '编程作品', time: '昨天 17:40', status: '待批改', score: null, comment: '', content: `when flag clicked\nforever {\n  turn right 30°\n  next costume\n  play sound (jump) until done\n}` },
  { id: 5, classId: 'c2', className: '五年级(2)班', name: '安安', avatar: '🐨', task: '编程作品：海底世界动画', type: '编程作品', time: '09-06 10:20', status: '已批改', score: 86, comment: '循环结构用得很熟练，把小鱼们的动作做得很有层次，继续加油！', content: `when clicked\nswitch costume to (fish 1)\nrepeat 12 {\n  glide 1 secs to (random position)\n}\nif <touching (shark)?> then\n  say 快跑！` },
  { id: 6, classId: 'c2', className: '五年级(2)班', name: '可可', avatar: '🐹', task: 'AI 写作：我的未来学校', type: 'AI 写作', time: '09-05 12:30', status: '已批改', score: 78, comment: '想象很丰富，建议把“校园里的机器人老师”这一段写得再具体一些。', content: '未来的学校里会有机器人老师，它们能陪我做实验、陪我下围棋，还会在放学时提醒我收拾书包。' },
  { id: 7, classId: 'c3', className: '四年级(1)班', name: '豆豆', avatar: '🐤', task: '课程问答：什么是人工智能', type: '课程问答', time: '09-04 11:08', status: '已批改', score: 82, comment: '能说出人工智能的用途，很完整。可以再想一想它能不能代替人类。', content: '人工智能是机器模仿人类的智能，它帮我们扫地、开车、翻译，但它只能按程序做事。' },
  { id: 8, classId: 'c3', className: '四年级(1)班', name: '跳跳', avatar: '🦄', task: '编程作品：小星星循环舞', type: '编程作品', time: '09-03 08:36', status: '已批改', score: 92, comment: '积木拼得又干净又清晰，角色切换很流畅，是一份很棒的作品！', content: `when flag clicked\nset rotation style (all around)\nrepeat 6 {\n  turn right 60°\n  move 80 steps\n  change size by 10\n}\nplay sound (pop) until done` }
]

const assignmentOptions = ref([...FALLBACK_ASSIGNMENTS])
let assignmentMap = {} // assignmentId -> assignment

const statusOptions = ['全部', '待批改', '已批改']

const allSubmissions = ref([...FALLBACK_SUBMISSIONS])
const submissions = computed(() => {
  let list = allSubmissions.value
  if (filterAssignment.value != null) list = list.filter(s => s.assignmentId === filterAssignment.value)
  if (filterStatus.value !== '全部') list = list.filter(s => s.status === filterStatus.value)
  const kw = filterSearch.value.trim()
  if (kw) list = list.filter(s => s.name.includes(kw) || s.task.includes(kw))
  return list
})

const filterAssignment = ref(null)
const filterStatus = ref('全部')
const filterSearch = ref('')

const drawerOpen = ref(false)
const current = ref(null)
const score = ref(85)
const comment = ref('')

const stars = computed({
  get: () => Math.round(score.value / 10) / 2,
  set: (v) => { score.value = Math.round(v * 20) }
})

// ===== 汇总 =====
const pendingCount = ref(0)
const gradedCount = computed(() => allSubmissions.value.filter(s => s.status === '已批改').length)

const typeTag = (type) => {
  if (type === '编程作品') return ''
  if (type === 'AI 写作') return 'k-tag--teal'
  return 'k-tag--yellow'
}

const typeLabel = (row) => {
  if (row.type === '编程作品') return '编程作品 · 作品代码预览'
  if (row.type === 'AI 写作') return 'AI 写作 · 写作内容'
  return '课程问答 · 回答内容'
}

const typeNumToLabel = (n) => ({ 1: '课程问答', 2: '编程作品', 3: 'AI 写作', 4: '课程问答' })[n] || '课程问答'

function mapReview(r) {
  const assignment = assignmentMap[r.assignmentId]
  const type = assignment ? typeNumToLabel(Number(assignment.type)) : '课程问答'
  return {
    id: r.submissionId,
    assignmentId: r.assignmentId,
    name: r.studentName || '学生',
    avatar: '🎓',
    className: (assignment && assignment.className) || '—',
    task: assignment ? assignment.title : (r.assignmentName || `任务#${r.assignmentId}`),
    type,
    time: r.submittedAt || '',
    status: r.status === 2 ? '已批改' : '待批改',
    score: r.score != null ? r.score : null,
    comment: r.feedback || '',
    content: r.content || ''
  }
}

async function loadPending() {
  try {
    const data = await request.get('/teacher/review/pending-count')
    if (typeof data === 'number') pendingCount.value = data
    else if (data && data.count != null) pendingCount.value = data.count
  } catch (e) {
    pendingCount.value = allSubmissions.value.filter(s => s.status === '待批改').length
  }
}

async function loadAssignments() {
  try {
    const data = await request.get('/teacher/assignments')
    const list = Array.isArray(data) ? data : []
    assignmentMap = {}
    list.forEach(a => { assignmentMap[a.id] = a })
    assignmentOptions.value = list.map(a => ({ id: a.id, name: a.title }))
    return true
  } catch (e) {
    return false
  }
}

async function loadSubmissions() {
  try {
    const data = await request.get('/teacher/review/list')
    const list = Array.isArray(data) ? data : []
    allSubmissions.value = list.map(mapReview)
  } catch (e) {
    // 回退 FALLBACK
  }
}

// ===== 交互 =====
const openReview = (row) => {
  current.value = row
  score.value = row.score ?? 85
  comment.value = row.comment || ''
  drawerOpen.value = true
}

const submitReview = () => {
  if (!current.value) return
  const id = current.value.id
  request.post(`/teacher/review/${id}`, { score: score.value, feedback: comment.value.trim() })
    .then(() => {
      ElMessage.success(`已为「${current.value.name}」批改完成 ✨`)
      drawerOpen.value = false
      loadSubmissions()
      loadPending()
    })
    .catch(() => {})
}

onMounted(async () => {
  loadPending()
  await loadAssignments()
  loadSubmissions()
})
</script>

<style scoped>
.t-review .sub { color: var(--ink-2); margin: 4px 0 0; }

/* 汇总数据条 */
.ov-strip {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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
  flex-shrink: 0;
}
.ov-icon.orange { background: var(--accent-soft); color: var(--accent-deep); }
.ov-icon.green { background: #e4f4ec; color: #1f7a52; }

.ov-num b {
  display: block;
  font-size: 24px;
  line-height: 1.1;
}
.ov-num span { font-size: 12.5px; color: var(--ink-3); }

/* 列表 */
.rv-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.rv-select { width: 160px; }
.rv-search { width: 240px; }

.rv-chips { display: flex; gap: 6px; }

.chip {
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-2);
  background: var(--surface-soft);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: all 0.18s;
}

.chip:hover { border-color: var(--brand); color: var(--brand); }

.chip.active {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

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

/* 抽屉详情 */
.rv-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.rv-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--surface-soft);
  font-size: 24px;
  flex-shrink: 0;
}

.rv-hero-info { flex: 1; }
.rv-hero-info b { font-size: 16px; }
.rv-sub { font-size: 13px; color: var(--ink-3); }

.rv-block { margin-top: 20px; }

.rv-block-head h4 {
  margin: 0 0 10px;
  font-size: 14.5px;
  font-family: var(--font-body);
  font-weight: 700;
  color: var(--ink-2);
}

.code-block {
  margin: 0;
  padding: 16px;
  background: #0f172a;
  color: #a5f3fc;
  border-radius: var(--radius-md);
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.qa-text {
  margin: 0;
  padding: 16px;
  background: var(--surface-soft);
  border-radius: var(--radius-md);
  color: var(--ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.rv-rate {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.rv-rate .el-input-number { width: 140px; }

.rv-comment-show {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--surface-soft);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
}
.rv-comment-label { color: var(--ink-3); }

.rv-submit {
  margin-top: 24px;
  width: 100%;
}

@media (max-width: 800px) {
  .ov-strip { grid-template-columns: 1fr; }
  .rv-select, .rv-search { width: 100%; }
}
</style>
