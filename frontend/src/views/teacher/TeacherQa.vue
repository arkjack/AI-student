<template>
  <div class="k-page t-qa">
    <div class="page-head">
      <h1 class="k-h1-icon"><ChatCircleDots :size="24" weight="bold" /> 答疑互动</h1>
      <p class="sub">接收并回复学生学习疑问，发布班级专属通知</p>
    </div>

    <!-- 答疑区 -->
    <section class="qa-grid">
      <!-- 学生提问列表 -->
      <div class="k-card qa-list">
        <div class="card-head">
          <h3><ChatCircleDots :size="16" weight="bold" /> 学生提问</h3>
          <span class="q-count">{{ pendingQuestionCount }} 条待回复</span>
        </div>
        <div class="q-list">
          <div
            v-for="q in questions"
            :key="q.id"
            class="q-item"
            :class="{ active: q.id === selectedId }"
            @click="select(q)"
          >
            <span class="q-avatar">{{ q.avatar }}</span>
            <div class="q-info">
              <b>{{ q.title }}</b>
              <span>{{ q.student }} · {{ q.time }}</span>
            </div>
            <span class="k-tag" :class="q.status === '已回复' ? 'k-tag--green' : 'k-tag--orange'">{{ q.status }}</span>
          </div>
        </div>
      </div>

      <!-- 答疑面板 -->
      <div class="k-card qa-panel">
        <template v-if="selected">
          <div class="card-head">
            <h3><ChatCircleDots :size="16" weight="bold" /> 答疑面板</h3>
          </div>
          <div class="qa-detail">
            <h4 class="qa-title">{{ selected.title }}</h4>
            <p class="qa-meta">{{ selected.student }}（{{ selected.className }}）· {{ selected.time }}</p>
            <div class="qa-body"><p>{{ selected.question }}</p></div>
          </div>
          <div class="reply-box">
            <el-input v-model="replyText" type="textarea" :rows="4" placeholder="输入你的解答..." />
            <button class="k-btn k-btn--orange" @click="sendReply">
              <PaperPlaneTilt :size="18" weight="bold" /> 发送回复
            </button>
          </div>
        </template>
        <div v-else class="qa-empty">从左侧选择一个问题开始答疑</div>

        <div class="history">
          <div class="history-head"><h4><ChatCircleDots :size="15" weight="bold" /> 历史回复</h4></div>
          <div v-if="repliedQuestions.length">
            <div v-for="q in repliedQuestions" :key="q.id" class="hist-item">
              <div class="hist-q">Q：{{ q.title }}<span>{{ q.student }} · {{ q.time }}</span></div>
              <p class="hist-a">{{ q.answer }}</p>
            </div>
          </div>
          <p v-else class="hist-empty">暂无已回复的问题</p>
        </div>
      </div>
    </section>

    <!-- 班级通知 -->
    <section class="k-card notice-card">
      <div class="card-head">
        <h3><Bell :size="16" weight="bold" /> 班级通知</h3>
      </div>

      <div class="notice-form">
        <div class="nf-row">
          <el-input v-model="noticeTitle" placeholder="通知标题" />
          <el-select v-model="noticeClass" placeholder="选择班级" class="nf-select">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <el-input v-model="noticeContent" type="textarea" :rows="3" placeholder="通知内容..." class="nf-content" />
        <button class="k-btn" @click="publishNotice">
          <PaperPlaneTilt :size="18" weight="bold" /> 发布通知
        </button>
      </div>

      <div class="notice-list">
        <div v-for="n in notices" :key="n.id" class="notice-item">
          <div class="notice-top">
            <b>{{ n.title }}</b>
            <span class="notice-time">{{ n.time }}</span>
          </div>
          <p class="notice-content">{{ n.content }}</p>
          <div class="notice-actions">
            <span class="k-tag">{{ n.className }}</span>
            <button class="notice-del" @click="deleteNotice(n.id)">
              <Trash :size="15" weight="bold" /> 删除
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhChatCircleDots as ChatCircleDots,
  PhPaperPlaneTilt as PaperPlaneTilt,
  PhBell as Bell,
  PhTrash as Trash
} from '@phosphor-icons/vue'

const FALLBACK_CLASSES = [
  { id: 'c1', name: '五年级(1)班' },
  { id: 'c2', name: '五年级(2)班' },
  { id: 'c3', name: '四年级(1)班' }
]

const classOptions = ref([...FALLBACK_CLASSES])

// ===== FALLBACK 数据：4 个学生问题（2 待回复 + 2 已回复） =====
const FALLBACK_QUESTIONS = [
  { id: 1, title: '为什么循环要用“重复 5 次”，一个个搬堆积木不行吗？', student: '小明', avatar: '🦊', className: '五年级(1)班', time: '今天 09:12', status: '待回复', question: '老师，我在拼小星星的时候，为什么要用“重复 5 次”这种积木呢？我想把同样的积木一个个搬过去行不行？', answer: '' },
  { id: 2, title: '人工智能会帮我们把作业都写完吗？', student: '朵朵', avatar: '🐰', className: '五年级(1)班', time: '今天 08:47', status: '待回复', question: '如果人工智能那么聪明，它会不会把我们的作业都写完啦？那我们还要不要自己做题呢？', answer: '' },
  { id: 3, title: '老师说的“云端”到底在哪里？', student: '安安', avatar: '🐨', className: '五年级(2)班', time: '昨天 21:05', status: '已回复', question: '老师经常说数据存在云端，云端到底是一个什么地方呀？', answer: '云端其实是一个放在很远地方的“超级机房”，里面有一排排的服务器。我们把数据和程序放进去，就能随时用网络取回来。因为它“在很远的地方，像在天上一样”，所以大家叫它云端啦。' },
  { id: 4, title: '手机是怎么认出我的脸的？', student: '豆豆', avatar: '🐤', className: '四年级(1)班', time: '昨天 17:52', status: '已回复', question: '我打开手机就能被认出来，人工智能是怎么做到的呢？', answer: '这是人脸识别。人工智能先学习了大量的人脸照片，记住眼睛、鼻子的特征，再把你现在的脸和记住的特征做对比，相似度够高就认出你了。记得每次解锁要在爸爸妈妈的同意下使用哦。' }
]

const questions = ref([...FALLBACK_QUESTIONS])
const selectedId = ref(1)
const replyText = ref('')

const selected = computed(() => questions.value.find(q => q.id === selectedId.value) || null)
const repliedQuestions = computed(() => questions.value.filter(q => q.status === '已回复' && q.answer))
const pendingQuestionCount = computed(() => questions.value.filter(q => q.status === '待回复').length)

const select = (q) => {
  selectedId.value = q.id
  replyText.value = q.answer || ''
}

function mapQuestion(q) {
  const question = q.question || ''
  const answered = !!q.answer
  return {
    id: q.id,
    title: question.length > 30 ? question.slice(0, 30) + '…' : (question || '学生提问'),
    student: q.studentName || '学生',
    avatar: '🎓',
    className: q.className || '—',
    time: q.createdAt || '',
    status: answered ? '已回复' : '待回复',
    question,
    answer: q.answer || ''
  }
}

async function loadQuestions() {
  try {
    const data = await request.get('/teacher/qa/list')
    const list = Array.isArray(data) ? data : []
    questions.value = list.map(mapQuestion)
    if (questions.value.length) selectedId.value = questions.value[0].id
  } catch (e) {
    // 回退 FALLBACK
  }
}

const sendReply = () => {
  if (!selected.value) return
  if (!replyText.value.trim()) {
    ElMessage.warning('请先输入回复内容')
    return
  }
  const id = selected.value.id
  const answer = replyText.value.trim()
  request.post(`/teacher/qa/${id}/reply`, { answer })
    .then(() => {
      selected.value.status = '已回复'
      selected.value.answer = answer
      replyText.value = ''
      ElMessage.success('回复已发送 ✨')
    })
    .catch(() => {})
}

async function loadClasses() {
  try {
    const data = await request.get('/teacher/classes')
    const list = Array.isArray(data) ? data : []
    classOptions.value = list.map(c => ({ id: c.id, name: c.name }))
  } catch (e) {}
}

// ===== mock 数据：班级通知（无后端接口，保留 mock + 演示标注） =====
const notices = ref([
  { id: 1, title: '下周一课程安排', content: '下周一我们将学习《什么是人工智能》，请同学们提前观看预告视频并想一想自己的疑问。', className: '五年级(1)班', time: '09-07 10:00' },
  { id: 2, title: '编程作品提交提醒', content: '积木作品《小星星循环舞》请在本周五前提交到实验区，记得给自己的作品取一个名字哦。', className: '五年级(2)班', time: '09-06 15:30' }
])

const noticeTitle = ref('')
const noticeContent = ref('')
const noticeClass = ref('')

const nowTime = () => {
  const d = new Date()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `今天 ${hh}:${mm}`
}

const publishNotice = () => {
  if (!noticeTitle.value.trim() || !noticeContent.value.trim() || !noticeClass.value) {
    ElMessage.warning('请完整填写通知标题、内容与班级')
    return
  }
  ElMessage.info('演示模式：班级通知接口暂未接入，当前为演示')
  const cls = classOptions.value.find(c => c.id === noticeClass.value)
  notices.value.unshift({
    id: Date.now(),
    title: noticeTitle.value.trim(),
    content: noticeContent.value.trim(),
    className: cls ? cls.name : '',
    time: nowTime()
  })
  noticeTitle.value = ''
  noticeContent.value = ''
  noticeClass.value = ''
}

const deleteNotice = (id) => {
  ElMessage.info('演示模式：班级通知接口暂未接入，当前为演示')
  notices.value = notices.value.filter(n => n.id !== id)
}

onMounted(() => {
  loadQuestions()
  loadClasses()
})
</script>

<style scoped>
.t-qa .sub { color: var(--ink-2); margin: 4px 0 0; }

/* 答疑区 */
.qa-grid {
  display: grid;
  grid-template-columns: 0.95fr 1.15fr;
  gap: var(--space-5);
  align-items: start;
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

.q-count { font-size: 12.5px; color: var(--ink-3); }

.q-list { display: flex; flex-direction: column; }

.q-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.18s;
}

.q-item:last-child { border-bottom: none; }

.q-item.active {
  background: var(--brand-soft);
  border-radius: var(--radius-md);
  padding-left: 10px;
  padding-right: 10px;
}

.q-avatar { font-size: 24px; flex-shrink: 0; }

.q-info { flex: 1; display: flex; flex-direction: column; line-height: 1.4; min-width: 0; }
.q-info b {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.q-info span { font-size: 12px; color: var(--ink-3); }

/* 答疑面板 */
.qa-detail {
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.qa-title { margin: 0 0 6px; font-size: 16px; }

.qa-meta { margin: 0 0 12px; font-size: 12.5px; color: var(--ink-3); }

.qa-body p {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--ink-2);
}

.reply-box {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reply-box .k-btn { align-self: flex-end; }

.qa-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--ink-3);
}

.history {
  margin-top: 22px;
  border-top: 1px solid var(--line);
  padding-top: 16px;
}
.history-head h4 {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 12px;
  font-size: 14.5px;
}
.history-head h4 svg { color: var(--brand); }

.hist-item {
  padding: 11px 0;
  border-bottom: 1px dashed var(--line);
}
.hist-item:last-child { border-bottom: none; }

.hist-q {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--ink);
}
.hist-q span {
  font-size: 12px;
  color: var(--ink-3);
  font-weight: 400;
  margin-left: 8px;
}

.hist-a {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink-2);
}

.hist-empty { color: var(--ink-3); font-size: 13px; }

/* 班级通知 */
.notice-card { margin-top: var(--space-5); }

.notice-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  margin-bottom: 20px;
}

.nf-row { display: flex; gap: 12px; }
.nf-row .el-input { flex: 1; }
.nf-select { width: 180px; }
.nf-content .el-textarea__inner { background: #fff; }
.notice-form .k-btn { align-self: flex-end; }

.notice-list { display: flex; flex-direction: column; }

.notice-item {
  padding: 13px 0;
  border-bottom: 1px solid var(--line);
}
.notice-item:last-child { border-bottom: none; }

.notice-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.notice-top b { font-size: 15px; }
.notice-time { font-size: 12px; color: var(--ink-3); }

.notice-content {
  margin: 6px 0 10px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-2);
}

.notice-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-del {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: none;
  color: var(--ink-3);
  font-size: 12.5px;
  cursor: pointer;
  transition: color 0.18s;
  font-family: inherit;
  padding: 0;
}
.notice-del:hover { color: var(--c-pink); }

@media (max-width: 1000px) {
  .qa-grid { grid-template-columns: 1fr; }
  .nf-row { flex-direction: column; }
  .nf-select { width: 100%; }
}
</style>
