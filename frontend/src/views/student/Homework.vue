<template>
  <div class="k-page">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><NotebookPen :size="16" weight="bold" /> 作业中心</h1>
        <p class="sub">查看老师布置的任务，按时提交就能拿到点评。</p>
      </div>
      <div class="head-stats">
        <div class="hs"><b class="orange">{{ counts.todo }}</b><span>待提交</span></div>
        <div class="hs"><b class="teal">{{ counts.submitted }}</b><span>已提交</span></div>
        <div class="hs"><b class="green">{{ counts.done }}</b><span>已批改</span></div>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="filters">
      <span
        v-for="f in statusFilters"
        :key="f.value"
        class="chip"
        :class="{ active: activeStatus === f.value }"
        @click="activeStatus = f.value"
      >{{ f.label }}</span>
    </div>

    <!-- 作业列表 -->
    <div class="hw-list">
      <div v-for="a in filtered" :key="a.id" class="k-card hw-card">
        <div class="hw-icon"><component :is="typeIcon(a)" :size="26" weight="bold" /></div>
        <div class="hw-body">
          <div class="hw-title-row">
            <h3>{{ a.title }}</h3>
            <el-tag
              :type="tagType(a.status)"
              round
              :effect="a.status === '待提交' ? 'dark' : 'light'"
            >{{ a.status }}</el-tag>
          </div>
          <p class="hw-content">{{ a.content }}</p>
          <div class="hw-meta">
            <span class="k-tag">{{ a.type }}任务</span>
            <span class="deadline"><Timer :size="16" weight="bold" /> 截止 {{ a.deadline }}</span>
            <span v-if="a.submitAt" class="submitted-at">提交于 {{ a.submitAt }}</span>
          </div>
          <div v-if="a.status === '已批改'" class="feedback-box">
            <span class="fb-head"><ChatCircleText :size="16" weight="bold" /> 老师点评（{{ a.score }} 分）</span>
            <span class="fb-text">{{ a.feedback }}</span>
          </div>
        </div>
        <div class="hw-action">
          <button
            v-if="a.status === '待提交'"
            class="k-btn k-btn--orange"
            @click="goDo(a)"
          >去完成</button>
          <button v-else class="k-btn k-btn--ghost" @click="goDo(a)">查看详情</button>
        </div>
      </div>

      <el-empty v-if="filtered.length === 0" description="这里空空如也～" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { PhNotePencil as NotebookPen, PhBookOpen as BookOpen, PhPuzzlePiece as PuzzlePiece, PhSparkle as Sparkle, PhTrophy as Trophy, PhFileText as FileText, PhTimer as Timer, PhChatCircleText as ChatCircleText } from '@phosphor-icons/vue'

const router = useRouter()

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_ASSIGNMENTS = [
  {
    id: 1, title: '观看《什么是人工智能？》并回答问题', type: '课程', typeEmoji: '📖',
    content: '观看本节课程后，请回答：人工智能能帮我们做哪些事情？请列举 3 个例子。',
    deadline: '2026-09-08 20:00', status: '待提交', score: null, feedback: null,
    submitAt: null
  },
  {
    id: 2, title: '完成积木作品：小星星循环舞', type: '编程', typeEmoji: '🧩',
    content: '使用循环积木完成星空绘制作品并提交，注意循环次数设置。',
    deadline: '2026-09-12 20:00', status: '待提交', score: null, feedback: null,
    submitAt: null
  },
  {
    id: 3, title: 'AI 创意写作：我的未来学校', type: 'AI 实验', typeEmoji: '✨',
    content: '用 AI 创意写作实验室，写一篇 200 字左右的《我的未来学校》，主题风格自定。',
    deadline: '2026-09-05 20:00', status: '已批改', score: 92,
    feedback: '想象力丰富！如果用上比喻句会更好哦～', submitAt: '2026-09-03 18:22'
  },
  {
    id: 4, title: '知识闯关：AI 基础 10 题', type: 'AI 实验', typeEmoji: '🏆',
    content: '完成 AI 知识闯关实验室的「AI 基础」关卡，并截图保存成绩。',
    deadline: '2026-08-30 20:00', status: '已批改', score: 100,
    feedback: '满分！太棒啦！', submitAt: '2026-08-29 19:05'
  }
]

const formatDateTime = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd} ${hh}:${mi}`
}

const statusFilters = [
  { label: '全部', value: 'all' },
  { label: '待提交', value: '待提交' },
  { label: '已提交', value: '已提交' },
  { label: '已完成', value: '已完成' },
  { label: '已批改', value: '已批改' }
]
const activeStatus = ref('all')

// 作业列表：合并 assignments + submissions（按 assignmentId 关联）后的展示数据
const assignmentList = ref([])
const submissionMap = ref({})
const progressMap = ref({})

const TYPE_MAP = { 1: '课程', 2: '编程', 3: 'AI 实验', 4: '闯关' }
const TYPE_EMOJI = { '课程': '📖', '编程': '🧩', 'AI 实验': '✨', '闯关': '🏆' }

const statusOf = (a) => {
  const sub = submissionMap.value[a.id]
  if (sub && sub.status === 2) return '已批改'
  if (sub && sub.status === 1) return '已提交'
  // 课程学习任务：按课程观看进度判断完成
  if (a.type === 1 && a.resourceId != null && (progressMap.value[a.resourceId] || 0) >= 100) return '已完成'
  return '待提交'
}

const mapAssignment = (a) => {
  const sub = submissionMap.value[a.id]
  const typeLabel = TYPE_MAP[a.type] || '课程'
  return {
    id: a.id,
    title: a.title,
    content: a.content,
    type: typeLabel,
    typeEmoji: TYPE_EMOJI[typeLabel] || '📖',
    deadline: a.deadline || '待定',
    rawStatus: a.status,
    status: statusOf(a),
    score: sub?.score,
    feedback: sub?.feedback,
    submitAt: sub?.submittedAt ? formatDateTime(sub.submittedAt) : null
  }
}

const filtered = computed(() => {
  if (activeStatus.value === 'all') return assignmentList.value
  return assignmentList.value.filter(a => a.status === activeStatus.value)
})

const counts = computed(() => ({
  todo: assignmentList.value.filter(a => a.status === '待提交').length,
  submitted: assignmentList.value.filter(a => a.status === '已提交').length,
  done: assignmentList.value.filter(a => a.status === '已批改' || a.status === '已完成').length
}))

const tagType = (s) => (s === '待提交' ? 'warning' : s === '已提交' ? 'primary' : 'success')

const typeIconMap = { '📖': BookOpen, '🧩': PuzzlePiece, '✨': Sparkle, '🏆': Trophy }
const typeIcon = (a) => typeIconMap[a.typeEmoji] || FileText

const loadHomework = async () => {
  try {
    const [assigns, subs, progress] = await Promise.all([
      request.get('/homework/assignments'),
      request.get('/homework/submissions'),
      request.get('/progress/my').catch(() => [])
    ])
    submissionMap.value = {}
    ;(subs || []).forEach(s => { submissionMap.value[s.assignmentId] = s })
    progressMap.value = {}
    ;(progress || []).forEach(p => { progressMap.value[p.courseId] = p.progress || 0 })
    assignmentList.value = (assigns || []).map(mapAssignment)
  } catch (e) {
    // 接口失败回退静态 mock；点评数据已内嵌在 mock 中
    assignmentList.value = FALLBACK_ASSIGNMENTS
  }
}

const goDo = (a) => {
  if (a.type === '课程') {
    router.push(`/student/course/${a.resourceId || 1}`)
  } else if (a.type === '编程') {
    // 带上任务 id，编程实验室提交时据此关联作业提交
    router.push('/student/lab?assignmentId=' + a.id)
  } else {
    // AI 实验：带任务 id，AI 实验室提交时据此关联作业提交
    router.push('/student/ai?tab=' + (a.typeEmoji === '🏆' ? 'quiz' : 'writing') + '&assignmentId=' + a.id)
  }
}

onMounted(() => {
  loadHomework()
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

.head-stats {
  display: flex;
  gap: var(--space-5);
  align-items: stretch;
}

.hs {
  text-align: center;
  padding: 0 var(--space-4);
}

.hs + .hs {
  border-left: 1.5px solid var(--line);
}

.hs b { display: block; font-size: 30px; line-height: 1.1; }
.hs span { font-size: 13px; color: var(--ink-3); }
.hs .orange { color: var(--c-orange-deep); }
.hs .teal { color: #0c9d86; }
.hs .green { color: #17934e; }

.filters {
  display: flex;
  gap: 8px;
  margin: 20px 0;
}

.chip {
  padding: 7px 20px;
  border-radius: 999px;
  font-size: 14px;
  background: #fff;
  color: var(--ink-2);
  cursor: pointer;
  font-weight: 700;
  box-shadow: var(--shadow-card);
  transition: all 0.2s;
}

.chip.active {
  background: var(--brand-grad);
  color: #fff;
}

.hw-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hw-card {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding: 20px 22px;
}

.hw-icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: var(--brand-grad-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.hw-body { flex: 1; }

.hw-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hw-title-row h3 {
  margin: 0;
  font-size: 17px;
}

.hw-content {
  margin: 8px 0;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.6;
}

.hw-meta {
  display: flex;
  gap: 14px;
  align-items: center;
  font-size: 12.5px;
  color: var(--ink-3);
  flex-wrap: wrap;
}

.feedback-box {
  margin-top: 12px;
  background: linear-gradient(135deg, #fff7e6, #fff1d6);
  border: 1.5px solid #ffe3b8;
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fb-head { font-size: 13px; font-weight: 700; color: #b7791f; }
.fb-text { font-size: 13.5px; color: #8a6d3b; }

.hw-action {
  flex-shrink: 0;
  padding-top: 8px;
}
</style>
