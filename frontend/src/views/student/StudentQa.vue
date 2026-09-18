<template>
  <div class="k-page qa-page">
    <!-- ========== 页头 ========== -->
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><ChatCircleDots :size="16" weight="bold" /> 答疑互动</h1>
        <p class="sub">遇到不懂的问题大胆问出来，老师在另一端等你哦 🌟</p>
      </div>
      <div class="head-stats">
        <div class="hs"><b class="orange">{{ counts.pending }}</b><span>待回复</span></div>
        <div class="hs"><b class="green">{{ counts.replied }}</b><span>已解答</span></div>
      </div>
    </div>

    <!-- ========== 提问卡片 ========== -->
    <section class="ask-card">
      <div class="ask-head">
        <div class="ask-ghost">💬</div>
        <div class="ask-head-txt">
          <b>有问题？现在就问</b>
          <span v-if="myTeacher">你的提问会发送给「{{ myTeacher }}」老师</span>
          <span v-else>你的提问会发送给你的负责老师</span>
        </div>
      </div>

      <div class="ask-body">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
          placeholder="把不懂的地方写下来，比如：为什么电脑能认出我的脸呀？"
        />
        <div class="ask-foot">
          <div class="quick-chips">
            <span v-for="c in quickQuestions" :key="c" class="q-chip" @click="question = c">{{ c }}</span>
          </div>
          <button class="k-btn k-btn--orange" :disabled="sending" @click="submit">
            <PaperPlaneTilt :size="18" weight="bold" /> {{ sending ? '发送中…' : '发送给老师' }}
          </button>
        </div>
      </div>
    </section>

    <!-- ========== 状态筛选 ========== -->
    <div class="filters">
      <span
        v-for="f in statusFilters"
        :key="f.value"
        class="chip"
        :class="{ active: activeStatus === f.value }"
        @click="activeStatus = f.value"
      >{{ f.label }}</span>
    </div>

    <!-- ========== 问答列表 ========== -->
    <div v-if="filtered.length" class="qa-list">
      <div v-for="q in filtered" :key="q.id" class="k-card qa-item">
        <!-- 我的提问 -->
        <div class="qa-row">
          <span class="bubble me">我</span>
          <div class="qa-content">
            <div class="qa-top">
              <span class="ask-label">提问</span>
              <span class="k-tag" :class="q.status === '已解答' ? 'k-tag--green' : 'k-tag--orange'">{{ q.status }}</span>
            </div>
            <p class="qa-text">{{ q.question }}</p>
            <span class="qa-time"><Clock :size="14" weight="bold" /> {{ q.time }}</span>
          </div>
        </div>

        <!-- 老师的回答 -->
        <div v-if="q.answer" class="qa-row">
          <span class="bubble teacher">{{ q.teacherInitial }}</span>
          <div class="qa-content answer">
            <div class="qa-top">
              <span class="ask-label">老师的解答</span>
              <span class="teacher-name">{{ q.teacherName || '老师' }}</span>
            </div>
            <p class="qa-text answer-text">{{ q.answer }}</p>
          </div>
        </div>
        <div v-else class="qa-waiting">
          <span class="waiting-dot"></span>
          <span>老师正在路上，请耐心等待～</span>
        </div>
      </div>
    </div>

    <el-empty v-else description="还没有提问，把第一个问题告诉老师吧！" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhChatCircleDots as ChatCircleDots,
  PhPaperPlaneTilt as PaperPlaneTilt,
  PhClock as Clock
} from '@phosphor-icons/vue'

const question = ref('')
const sending = ref(false)
const activeStatus = ref('all')

const statusFilters = [
  { label: '全部', value: 'all' },
  { label: '待回复', value: '待回复' },
  { label: '已解答', value: '已解答' }
]

const quickQuestions = [
  '为什么电脑能认出我的脸？',
  '人工智能会取代我们的工作吗？',
  '什么是编程？'
]

const myQuestions = ref([])

// 我的负责老师（取第一条有教师姓名的问题）
const myTeacher = computed(() => {
  const q = myQuestions.value.find(i => i.teacherName)
  return q ? q.teacherName : ''
})

const filtered = computed(() => {
  if (activeStatus.value === 'all') return myQuestions.value
  return myQuestions.value.filter(q => q.status === activeStatus.value)
})

const counts = computed(() => ({
  pending: myQuestions.value.filter(q => q.status === '待回复').length,
  replied: myQuestions.value.filter(q => q.status === '已解答').length
}))

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}年${mm}月${dd}日 ${hh}时${mi}分`
}

const mapQuestion = (q) => ({
  id: q.id,
  question: q.question || '',
  answer: q.answer || '',
  status: q.answer ? '已解答' : '待回复',
  teacherName: q.teacherName || '',
  teacherInitial: (q.teacherName || '师').charAt(0),
  time: formatDate(q.createdAt)
})

async function loadQuestions() {
  try {
    const data = await request.get('/qa/my')
    myQuestions.value = (Array.isArray(data) ? data : []).map(mapQuestion)
  } catch (e) {
    myQuestions.value = []
  }
}

const submit = async () => {
  const text = question.value.trim()
  if (!text) {
    ElMessage.warning('请先写下你的问题')
    return
  }
  sending.value = true
  try {
    await request.post('/qa/ask', { question: text })
    question.value = ''
    ElMessage.success('发送成功，老师会尽快解答 ✨')
    await loadQuestions()
  } catch (e) {
    // 错误提示已在拦截器统一处理
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.qa-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.page-head h1 { margin: 4px 0 6px; font-size: 30px; }
.sub { color: var(--ink-2); margin: 0; }

.head-stats { display: flex; gap: var(--space-5); align-items: stretch; }
.hs { text-align: center; padding: 0 var(--space-4); }
.hs + .hs { border-left: 1.5px solid var(--line); }
.hs b { display: block; font-size: 30px; line-height: 1.1; }
.hs span { font-size: 13px; color: var(--ink-3); }
.hs .orange { color: var(--accent-deep); }
.hs .green { color: #17934e; }

/* 提问卡片（沿用首页 hero 场景背景，左重右轻遮罩保证可读性） */
.ask-card {
  position: relative;
  margin-top: 22px;
  background:
    linear-gradient(115deg, rgba(21, 40, 92, 0.96) 0%, rgba(30, 79, 216, 0.88) 46%, rgba(30, 79, 216, 0.52) 73%, rgba(30, 79, 216, 0.28) 100%),
    url('@/assets/images/hero-bg-wide.jpg') center / cover no-repeat;
  border-radius: 20px;
  padding: 22px 26px 24px;
  box-shadow: 0 10px 26px rgba(30, 79, 216, 0.28);
  color: #fff;
  overflow: hidden;
}

.ask-head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.ask-ghost {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}
.ask-head-txt { display: flex; flex-direction: column; }
.ask-head-txt b { font-size: 19px; }
.ask-head-txt span { font-size: 13px; opacity: 0.85; margin-top: 2px; }

.ask-body :deep(.el-textarea__inner) {
  background: #fff;
  border: none;
  border-radius: 14px;
  font-size: 14.5px;
  padding: 12px 14px;
  box-shadow: none;
}

.ask-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.quick-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.q-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 12.5px;
  cursor: pointer;
  transition: background 0.2s;
}
.q-chip:hover { background: rgba(255, 255, 255, 0.3); }

/* 筛选 */
.filters { display: flex; gap: 8px; margin: 20px 0; }
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
.chip.active { background: var(--brand-grad); color: #fff; }

/* 问答列表 */
.qa-list { display: flex; flex-direction: column; gap: 16px; }
.qa-item { padding: 20px 22px; }

.qa-row { display: flex; gap: 14px; align-items: flex-start; }
.qa-row + .qa-row, .qa-row + .qa-waiting { margin-top: 18px; padding-top: 18px; border-top: 1px dashed var(--line); }

.bubble {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 15px;
  flex-shrink: 0;
}
.bubble.me { background: var(--brand-grad-soft); color: var(--brand); }
.bubble.teacher { background: #ffe9d6; color: var(--accent-deep); }

.qa-content { flex: 1; min-width: 0; }
.qa-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.ask-label { font-size: 12px; font-weight: 700; color: var(--ink-3); }

.qa-text { margin: 0; font-size: 14.5px; line-height: 1.7; color: var(--ink); }
.qa-time { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--ink-3); margin-top: 8px; }

.teacher-name { font-size: 12.5px; color: var(--accent-deep); font-weight: 700; }
.answer { background: #fff8f1; border-radius: 12px; padding: 12px 14px; }
.answer-text { color: #7a4a12; }

.qa-waiting {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  font-size: 13px;
}
.waiting-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--accent);
  animation: pulse 1.4s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 0.4; } 50% { opacity: 1; } }

@media (max-width: 720px) {
  .ask-foot { flex-direction: column; align-items: stretch; }
  .ask-foot .k-btn { align-self: flex-end; }
}
</style>
