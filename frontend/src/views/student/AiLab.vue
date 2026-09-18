<template>
  <div class="k-page ai-page">
    <div class="ai-hero">
      <div class="ai-hero-text">
        <h1><Sparkle :size="16" weight="bold" /> AI 魔法实验室</h1>
        <p>三个动手实验室：写故事、闯关卡、问问题。</p>
      </div>
      <img v-if="heroOk" src="@/assets/images/ai-quiz.jpg" class="ai-hero-img" @error="heroOk = false" />
    </div>

    <!-- Tab 切换 -->
    <div class="ai-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ai-tab"
        :class="{ active: activeTab === t.key }"
        @click="activeTab = t.key"
      >
        <span class="tab-emoji"><component :is="t.icon" :size="22" weight="bold" /></span>
        <span class="tab-title">{{ t.title }}</span>
        <span class="tab-desc">{{ t.desc }}</span>
      </button>
    </div>

    <!-- ================= 创意写作 ================= -->
    <section v-if="activeTab === 'writing'" class="k-card panel">
      <div class="panel-grid writing-grid">
        <div class="writing-form">
          <h3><NotebookPen :size="16" weight="bold" /> 写作设置</h3>
          <label class="form-label">写作主题</label>
          <el-input v-model="writing.topic" placeholder="比如：风 / 会飞的学校 / 我的机器人伙伴" size="large" />
          <label class="form-label">写作风格</label>
          <div class="style-chips">
            <span v-for="s in styles" :key="s" class="chip" :class="{ active: writing.style === s }" @click="writing.style = s">{{ s }}</span>
          </div>
          <label class="form-label">内容长度</label>
          <el-slider v-model="writing.length" :min="50" :max="300" :step="10" style="padding: 0 8px" />
          <div class="length-hint">约 {{ writing.length }} 字</div>
          <button class="k-btn k-btn--orange gen-btn" :disabled="generating" @click="generateStory">
            <Sparkle v-if="!generating" :size="16" weight="bold" />
            {{ generating ? 'AI 正在创作中…' : '让 AI 写一篇' }}
          </button>
        </div>

        <div class="writing-result" ref="editorRef" :class="{ 'is-flash': editorFlash }">
          <h3><FileText :size="16" weight="bold" /> 作品预览 <span class="k-tag">来自 DeepSeek</span></h3>

          <!-- 从「我的写作记录」载入某篇作品后的说明条：讲清这次保存会发生什么 -->
          <div v-if="openedRecord" class="opened-bar" :class="{ 'is-graded': openedRecord.graded }">
            <Info :size="15" weight="bold" />
            <span class="opened-text">
              正在编辑《{{ openedRecord.title }}》
              <template v-if="openedRecord.graded">（老师已批改 {{ openedRecord.score }} 分）—— 继续保存不会改动这篇已批改的作品，你的修改会另存为新草稿</template>
              <template v-else>—— 保存会另存为新草稿，原记录保持不变</template>
            </span>
            <button class="opened-close" @click="closeOpenedRecord">关闭</button>
          </div>

          <div v-if="generating" class="gen-anim">
            <span class="gen-orb">🤖</span>
            <p>AI 正在构思一个有趣的故事…</p>
          </div>
          <el-input
            v-else
            v-model="writing.content"
            type="textarea"
            :rows="13"
            placeholder="点击「让 AI 写一篇」，故事就会出现在这里～"
          />
          <div class="result-btns" v-if="writing.content">
            <button class="k-btn k-btn--ghost" @click="saveWriting">
              <FolderOpen :size="16" weight="bold" /> {{ openedRecord ? '另存为新草稿' : '保存到作品集' }}
            </button>
            <button class="k-btn k-btn--orange" @click="submitWriting">
              <PaperPlaneTilt :size="16" weight="bold" /> 提交作业
            </button>
          </div>
        </div>
      </div>

      <div class="history">
        <h3><Clock :size="16" weight="bold" /> 我的写作记录</h3>
        <!-- 记录可点击：把主题/风格/正文重新载回上方编辑器继续查看或修改 -->
        <p v-if="writingRecords.length" class="history-tip">点一下任意一条，就能重新打开继续查看或编辑～</p>
        <div
          v-for="r in writingRecords"
          :key="r.id"
          class="history-item"
          :class="{ 'is-open': openedRecord && openedRecord.id === r.id }"
          role="button"
          tabindex="0"
          @click="openWritingRecord(r)"
          @keydown.enter="openWritingRecord(r)"
          @keydown.space.prevent="openWritingRecord(r)"
        >
          <div class="h-left">
            <b>{{ r.title }}</b>
            <span>{{ r.topic }} · {{ r.style }} · {{ r.time }}</span>
          </div>
          <div class="h-right">
            <span v-if="openedRecord && openedRecord.id === r.id" class="h-open">
              <PencilSimple :size="13" weight="bold" /> 编辑中
            </span>
            <el-tag v-if="r.status === '已批改'" type="success" round>已批改 {{ r.score }}分</el-tag>
            <el-tag v-else-if="r.status === '未批改'" type="warning" round>未批改</el-tag>
            <el-tag v-else type="info" round>草稿</el-tag>
            <CaretRight :size="14" weight="bold" class="h-caret" />
          </div>
        </div>
        <p v-if="!writingRecords.length" class="history-empty">还没有写作记录，写一篇再点「保存到作品集」吧～</p>
      </div>
    </section>

    <!-- ================= 知识闯关 ================= -->
    <section v-if="activeTab === 'quiz'" class="k-card panel">
      <template v-if="!quiz.finished">
        <div class="quiz-top">
          <div class="quiz-level">
            <span class="k-tag k-tag--orange"><Trophy :size="16" weight="bold" /> {{ quiz.level }}</span>
            <span class="quiz-count">第 {{ quiz.index + 1 }} / {{ quizBank.length }} 题</span>
          </div>
          <div class="quiz-bar">
            <div class="quiz-bar-fill" :style="{ transform: 'scaleX(' + (quiz.index / quizBank.length) + ')' }"></div>
          </div>
          <div class="quiz-score">当前得分 <b>{{ quiz.score }}</b></div>
        </div>

        <div class="quiz-q">
          <h3><Question :size="16" weight="bold" /> {{ currentQ.q }}</h3>
          <div class="quiz-options">
            <button
              v-for="(opt, i) in currentQ.options"
              :key="i"
              class="quiz-opt"
              :class="optionClass(i)"
              :disabled="quiz.answered !== null"
              @click="answer(i)"
            >
              <span class="opt-letter">{{ 'ABCD'[i] }}</span>
              <span>{{ opt }}</span>
              <span v-if="quiz.answered !== null && i === currentQ.answer" class="opt-mark"><CheckCircle :size="16" weight="fill" color="#2ecc71" /></span>
              <span v-else-if="quiz.answered === i" class="opt-mark"><XCircle :size="16" weight="fill" color="#f56c6c" /></span>
            </button>
          </div>
          <div v-if="quiz.answered !== null" class="quiz-feedback">
            <span v-if="quiz.answered === currentQ.answer" class="good">答对啦！+10 分</span>
            <span v-else class="bad">差点答对，正确答案是 {{ currentQ.options[currentQ.answer] }}</span>
            <button class="k-btn next-btn" @click="nextQuestion">
              {{ quiz.index === quizBank.length - 1 ? '查看成绩' : '下一题' }}
              <ArrowRight :size="16" weight="bold" />
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="quiz-done">
          <div class="done-emoji">{{ quiz.score >= 40 ? '🏆' : quiz.score >= 20 ? '🎖️' : '🌱' }}</div>
          <h2>{{ quiz.score >= 40 ? '太厉害了，闯关王者！' : quiz.score >= 20 ? '不错哦，继续加油！' : '别灰心，再试一次！' }}</h2>
          <p>答对 {{ quiz.correct }} / {{ quizBank.length }} 题，得分 {{ quiz.score }}</p>
          <div class="done-btns">
            <button class="k-btn" @click="restartQuiz">
              <ArrowsClockwise :size="16" weight="bold" /> 重新挑战
            </button>
            <button class="k-btn k-btn--ghost" @click="$router.push('/student/homework')">
              <PaperPlaneTilt :size="16" weight="bold" /> 记录成绩并去提交
            </button>
          </div>

          <!-- 历次答题成绩 -->
          <div v-if="quizHistory.length" class="quiz-history">
            <h3><Trophy :size="16" weight="bold" /> 历次答题成绩</h3>
            <div class="quiz-history-list">
              <div v-for="h in quizHistory" :key="h.id" class="qh-item">
                <span class="qh-level">{{ h.level }}</span>
                <span class="qh-score">得分 <b>{{ h.score }}</b></span>
                <span class="qh-count">答对 {{ h.correct }}/{{ h.total }}</span>
                <span class="qh-time">{{ h.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- ================= 智能答疑 ================= -->
    <section v-if="activeTab === 'chat'" class="k-card panel chat-panel">
      <div class="chat-grid">
        <div class="chat-history">
          <h3><Clock :size="16" weight="bold" /> 历史问答</h3>
          <div v-for="(h, i) in qaHistory" :key="i" class="his-item" @click="ask(h.q)">
            <b>Q：{{ h.q }}</b>
            <span>{{ h.time }}</span>
          </div>
          <div class="chat-tip">
            <LightBulb :size="16" weight="bold" /> 试试问我：<br />「什么是 AI？」「怎么学编程？」「图像识别是什么？」
          </div>
        </div>

        <div class="chat-main">
          <div class="chat-window">
            <div v-if="msgs.length === 0" class="chat-empty">
              <ChatCircleDots :size="56" weight="bold" color="#93a2b8" />
              <p>有问题尽管问，AI 老师随时在线！</p>
            </div>
            <div v-for="(m, i) in msgs" :key="i" class="msg" :class="m.role">
              <span class="msg-avatar">{{ m.role === 'user' ? '🙋' : '🤖' }}</span>
              <div class="bubble">
                <span v-if="m.typing">思考中…</span>
                <span v-else>{{ m.text }}</span>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input v-model="chatInput" placeholder="输入你的问题，回车提问" size="large" @keyup.enter="ask()">
              <template #prefix><ChatCircleText :size="16" weight="bold" /></template>
            </el-input>
            <button class="k-btn k-btn--green" @click="ask()">
              <PaperPlaneTilt :size="16" weight="bold" /> 发送
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { gsap, prefersReducedMotion } from '@/utils/motion'
import { PhSparkle as Sparkle, PhNotePencil as NotebookPen, PhTrophy as Trophy, PhChatCircleDots as ChatCircleDots, PhFileText as FileText, PhFolderOpen as FolderOpen, PhPaperPlaneTilt as PaperPlaneTilt, PhClock as Clock, PhQuestion as Question, PhCheckCircle as CheckCircle, PhXCircle as XCircle, PhArrowRight as ArrowRight, PhArrowsClockwise as ArrowsClockwise, PhChatCircleText as ChatCircleText, PhLightbulb as LightBulb, PhPencilSimple as PencilSimple, PhCaretRight as CaretRight, PhInfo as Info } from '@phosphor-icons/vue'

/* ============ 组件内 mock 数据：接口失败/AI 未配置时的 fallback ============ */
const FALLBACK_QUIZ_BANK = [
  { q: '人工智能的英文缩写是？', options: ['AI', 'AR', 'VR', 'IT'], answer: 0 },
  { q: '下面哪个是人工智能的应用？', options: ['人脸识别', '电风扇', '自行车', '微波炉'], answer: 0 },
  { q: '机器人认识世界需要靠什么？', options: ['传感器', '颜料', '橡皮', '胶水'], answer: 0 },
  { q: '机器学习中，机器学习的“原料”是什么？', options: ['数据', '西瓜', '面粉', '颜料'], answer: 0 },
  { q: '语音助手能听懂我们说话，靠的是？', options: ['语音识别', '光能', '风力', '重力'], answer: 0 }
]
const FALLBACK_QA_HISTORY = [
  { q: 'AI 会自己思考吗？', a: '现在的 AI 更多是在“模仿”人类的思考方式，它们通过大量数据学习规律，还不能像人一样真正地思考和感受哦！', time: '昨天' },
  { q: '我以后能学会编程吗？', a: '当然可以！编程就像搭积木，从最简单的积木块开始，一步一步来，你一定能学会！', time: '3 天前' }
]
/* 接口失败时的本地示例：结构与 loadWritingRecords() 的映射结果保持一致，保证点击载入行为相同 */
const FALLBACK_WRITING_RECORDS = [
  { id: 1, title: '如果我是风', topic: '风', style: '童话风', content: '如果我是风，我要去森林里和树叶捉迷藏……', status: '已批改', graded: true, submitted: true, score: 92, time: '2026-08-28' },
  { id: 2, title: '会飞的书包', topic: '书包', style: '科幻风', content: '我的书包上有两个小翅膀，每天放学它带着我飞过操场……', status: '草稿', graded: false, submitted: false, score: null, time: '2026-08-31' }
]

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日 ${pad(d.getHours())}时${pad(d.getMinutes())}分`
}

const heroOk = ref(true)

const tabs = [
  { key: 'writing', icon: NotebookPen, title: '创意写作实验室', desc: '主题 · 风格 · AI 生成' },
  { key: 'quiz', icon: Trophy, title: '知识闯关实验室', desc: 'AI 出题 · 游戏化闯关' },
  { key: 'chat', icon: ChatCircleDots, title: '智能答疑实验室', desc: '自由问答 · 随时解惑' }
]
const activeTab = ref('writing')
// 从作业中心跳转而来时携带的任务 id（提交作品时据此关联作业提交）
const route = useRoute()
const assignmentId = computed(() => (route.query.assignmentId ? Number(route.query.assignmentId) : null))

/* ---------------- 创意写作 ---------------- */
const styles = ['童话风', '科幻风', '搞笑风', '诗意风', '冒险风']
const writing = ref({
  topic: '会飞的学校',
  style: '童话风',
  length: 150,
  content: ''
})
const generating = ref(false)
/* 当前在编辑器里打开的那条历史记录（null = 正在写一篇新的） */
const openedRecord = ref(null)
/* 编辑器容器：载入记录后滚动到它，并用一次高亮闪烁告诉学生内容去了哪里 */
const editorRef = ref(null)
const editorFlash = ref(false)
let flashTimer = null

// 本地示例故事（AI 未配置/失败时的回退）
const buildStory = (topic, style) => {
  const t = topic.trim()
  const s = style
  const stories = {
    '童话风': `在一片会眨眼的云朵上，有一座${t}。每天清晨，它都会轻轻地哼着歌飞到彩虹桥边，等小朋友们坐上长长的滑梯，再“嗖”地一下飞向天空。风婆婆住在${t}的阁楼里，她用星星做成的羽毛笔，把每个孩子的梦都记进日记里。有一天，一位小朋友问：“${t}会飞累吗？”风婆婆笑了：“有你们的笑声，永远不会累。”`,
    '科幻风': `公元 2088 年，当我按下书包上的蓝色按钮，${t}缓缓升空了。透明的舱壁外，城市像一张发光的电路板。导航机器人“小九”告诉我，今天的课程在天上：云朵里藏着数据农场，我们要帮它收集会唱歌的彩虹能量块。落地时，AI 老师递来一张卡片：“任务完成率 100%，欢迎升级为星际学员。”`,
    '搞笑风': `你知道吗？我们的${t}昨天起飞失败，因为窗帘被当成了翅膀——校长急得把帽子都甩飞了！同学们哈哈大笑，机器人老师却认真地记录：“建议下次用轻一点的窗帘。”经过 33 次实验，${t}终于学会了“先加油、再起飞”的秘诀。现在它每天绕操场七圈半，还学会了和鸽子争抢飞行路线。`,
    '诗意风': `清晨的白雾里，${t}展开云做的翅膀。风是它的老朋友，总会在转弯处轻轻地托它一把。教室里，黑板会写诗，粉笔是星星；课桌会做梦，抽屉里装着四季。每当晚霞染红屋顶，它就降落在一片梧桐叶上，等着明天，第一缕阳光叫醒它。`,
    '冒险风': `警报！北斗星信号消失了！作为王牌飞行员，我登上了${t}，启动反重力引擎。穿过紫色星云，我们找到了被小行星困住的信号塔。我伸出机械爪，小心地接好最后一根光缆——屏幕上出现了“信号恢复”的绿色字符。返航时，队长拍拍我的头盔：“干得漂亮，今天你救了整个星座！”`
  }
  return stories[s] || stories['童话风']
}

const generateStory = async () => {
  if (!writing.value.topic.trim()) {
    ElMessage.warning('先输入一个写作主题吧～')
    return
  }
  generating.value = true
  writing.value.content = ''
  // 新生成的故事不再属于之前打开的那条记录，「编辑中」标记要摘掉
  openedRecord.value = null
  try {
    const res = await request.post('/ai/writing', {
      topic: writing.value.topic.trim(),
      style: writing.value.style,
      length: writing.value.length
    })
    writing.value.content = res?.content || buildStory(writing.value.topic, writing.value.style)
    ElMessage.success('AI 创作完成 ✨')
  } catch (e) {
    writing.value.content = buildStory(writing.value.topic, writing.value.style)
    ElMessage.warning('AI 尚未配置，已使用本地示例')
  } finally {
    generating.value = false
    // 生成结束后的正文一定是 AI 新写的（生成途中若点过历史记录也会被覆盖），
    // 所以这里再摘一次「编辑中」标记，保证标记与编辑器内容始终一致
    openedRecord.value = null
  }
}

const writingRecords = ref([])

const loadWritingRecords = async () => {
  try {
    const list = await request.get('/writing/my')
    writingRecords.value = (list || []).map(r => ({
      id: r.id,
      title: r.topic || '未命名',
      topic: r.topic,
      style: r.style,
      // 正文：后端 /writing/my 返回完整实体（含 TEXT 类型 content），
      // 这里必须保留，否则点击记录时无法把文章重新载入编辑器
      content: r.content || '',
      time: formatDate(r.createdAt),
      status: r.status === 1 ? (r.score != null ? '已批改' : '未批改') : '草稿',
      // 老师已批改：保存时要在界面上明确提示「只另存新草稿，不动这一篇」
      graded: r.status === 1 && r.score != null,
      // 是否已提交过（status=1），用于区分提示语气
      submitted: r.status === 1,
      score: r.score
    }))
  } catch (e) {
    writingRecords.value = FALLBACK_WRITING_RECORDS
  }
}

/* 点击一条写作记录 → 把主题、风格、正文重新载回上方编辑器 */
const openWritingRecord = (record) => {
  if (!record) return
  writing.value.topic = record.topic || ''
  writing.value.style = record.style || writing.value.style
  writing.value.content = record.content || ''
  openedRecord.value = record

  if (record.content) {
    ElMessage.success(`已载入《${record.title}》，可以继续编辑`)
  } else {
    ElMessage.warning(`《${record.title}》没有保存正文，只能看到主题和风格～`)
  }

  // 平滑滚动到编辑器，并让编辑器闪一下高亮，避免学生「找不到内容去哪了」
  nextTick(() => {
    const el = editorRef.value
    if (!el) return
    try {
      el.scrollIntoView({ behavior: prefersReducedMotion() ? 'auto' : 'smooth', block: 'center' })
    } catch (e) {
      el.scrollIntoView()
    }
    editorFlash.value = true
    clearTimeout(flashTimer)
    flashTimer = setTimeout(() => { editorFlash.value = false }, 1400)
  })
}

/* 退出「编辑已有作品」状态：只摘掉标记，不清空正文，避免误删学生已写内容 */
const closeOpenedRecord = () => {
  openedRecord.value = null
}

const saveWriting = async () => {
  if (!writing.value.content.trim()) {
    ElMessage.warning('还没有内容可保存～')
    return
  }
  /* ------------------------------------------------------------------
     关于 status 的取值（重点，勿轻改）：
     1) 后端 POST /writing/save 是「新增」语义：WritingServiceImpl.saveWriting()
        只做 aiWritingMapper.insert()，从不 update 已有行。
        所以这里固定传 status:0 只会「多出一条草稿」，绝不会把已批改记录
        （status=1 且 score 有值）降级成草稿，老师的分数不会丢。
     2) 反过来，如果为了「保持原有 status」而传 status=1，危害更大：
        TeacherAssignmentServiceImpl.review() 批改的是学生「id 最大的
        status=1」那条（orderByDesc(id).last("limit 1")），新插入的 status=1
        会把老师之后的批改引到这份新副本上，并重复发放提交经验。
     3) 因此这里保持 status:0，并在界面上把「另存为新草稿」讲清楚
        （见编辑器的说明条与下面的成功提示），不做静默处理。
     ------------------------------------------------------------------ */
  const from = openedRecord.value
  try {
    const saved = await request.post('/writing/save', { topic: writing.value.topic, style: writing.value.style, content: writing.value.content, status: 0 })
    if (from) {
      if (from.graded) {
        ElMessage.success({ message: `已另存为新草稿；原《${from.title}》的 ${from.score} 分批改结果保持不变`, duration: 4000 })
      } else {
        ElMessage.success('已另存为新草稿，原记录保持不变 💾')
      }
    } else {
      ElMessage.success('故事已保存到作品集 💾')
    }
    await loadWritingRecords()
    // 保存后编辑器里的内容对应的是这条新草稿，标记跟着挪过去，「编辑中」高亮才准确
    if (saved && saved.id != null) {
      const fresh = writingRecords.value.find(r => r.id === saved.id)
      if (fresh) openedRecord.value = fresh
    }
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}

const submitWriting = async () => {
  if (!writing.value.content.trim()) {
    ElMessage.warning('还没有内容可提交～')
    return
  }
  try {
    await request.post('/writing/save', { topic: writing.value.topic, style: writing.value.style, content: writing.value.content, status: 1 })
    // 若从作业中心的任务进入，关联创建该任务的作业提交
    const asgId = assignmentId.value
    if (asgId) {
      try {
        await request.post('/homework/submit', { assignmentId: asgId, content: writing.value.content })
      } catch (e) {}
    }
    ElMessage.success('作品已提交！等待老师批改～ 📤')
    loadWritingRecords()
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}

/* ---------------- 知识闯关 ---------------- */
const quiz = ref({
  index: 0,
  score: 0,
  correct: 0,
  answered: null,
  finished: false,
  level: 'AI 基础'
})

const quizBank = ref(FALLBACK_QUIZ_BANK)
const currentQ = computed(() => quizBank.value[quiz.value.index])

const loadQuiz = async () => {
  try {
    const list = await request.get('/quiz/questions', { params: { level: quiz.value.level } })
    quizBank.value = (list || []).map(q => ({
      q: q.question,
      options: [q.optionA, q.optionB, q.optionC, q.optionD],
      answer: ['A', 'B', 'C', 'D'].indexOf(q.answer)
    }))
    if (quizBank.value.length === 0) quizBank.value = FALLBACK_QUIZ_BANK
  } catch (e) {
    quizBank.value = FALLBACK_QUIZ_BANK
  }
}

const optionClass = (i) => {
  if (quiz.value.answered === null) return ''
  if (i === currentQ.value.answer) return 'correct'
  if (i === quiz.value.answered) return 'wrong'
  return 'dim'
}

const answer = (i) => {
  if (quiz.value.answered !== null) return
  quiz.value.answered = i
  const correct = i === currentQ.value.answer
  if (correct) {
    quiz.value.score += 10
    quiz.value.correct += 1
  }
  // 反馈动效：选项标记弹性弹入 + 分数跳动（事件驱动的一次性 tween）
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    nextTick(() => {
      gsap.fromTo('.opt-mark', { scale: 0.3, autoAlpha: 0 }, { scale: 1, autoAlpha: 1, duration: 0.45, ease: 'back.out(2.6)' })
      gsap.fromTo('.quiz-score b', { scale: 1.3 }, { scale: 1, duration: 0.4, ease: 'power2.out' })
      gsap.fromTo('.quiz-feedback', { y: 12, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.35, ease: 'power2.out' })
    })
  }
}

const quizHistory = ref([])
const loadQuizHistory = async () => {
  try {
    const list = await request.get('/quiz/my')
    quizHistory.value = (list || []).map(r => ({
      id: r.id,
      level: r.level,
      score: r.score,
      correct: r.correctCount,
      total: r.totalCount,
      time: formatDate(r.createdAt)
    }))
  } catch (e) {
    quizHistory.value = []
  }
}

const recordQuiz = async () => {
  try {
    await request.post('/quiz/record', {
      level: quiz.value.level,
      score: quiz.value.score,
      correctCount: quiz.value.correct,
      totalCount: quizBank.value.length,
      assignmentId: assignmentId.value
    })
    // 记录后刷新历次成绩
    loadQuizHistory()
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}

const nextQuestion = () => {
  if (quiz.value.index === quizBank.value.length - 1) {
    quiz.value.finished = true
    recordQuiz()
    return
  }
  quiz.value.index += 1
  quiz.value.answered = null
}

const restartQuiz = () => {
  quiz.value = { index: 0, score: 0, correct: 0, answered: null, finished: false, level: 'AI 基础' }
}

/* ---------------- 智能答疑 ---------------- */
const chatInput = ref('')
const msgs = ref([])

const AI_ANSWERS = [
  { match: ['ai', '人工智能'], reply: '人工智能（AI）就是让机器模仿人类的能力，比如看图片、听声音、思考问题！就像我们的吉祥物小机器人一样，它通过“学习”很多数据变得越来越聪明。🤖' },
  { match: ['编程', '代码', '积木'], reply: '学编程就像搭积木！你可以从拖拽积木块开始，把“当开始”和“说你好”拼在一起，就写出了第一个程序。试试打开「编程实验室」吧！🧩' },
  { match: ['图像', '识别', '拍照'], reply: '图像识别就是让电脑“看懂”图片。比如你手机里的人脸解锁，就是先记住你的脸，再把每次拍到的脸和记忆对比，对了就解锁啦！👀' },
  { match: ['学习', '进步', '提高'], reply: '每天 15 分钟就够啦！先看一节有趣的课，再做一个小实验，坚持 7 天，你就会发现每次打开平台都有新收获。加油！💪' },
  { match: ['是什么', '为什么'], reply: '好问题！这是 AI 启蒙星球的经典问题～ 推荐你先去「课程中心」看看对应的课程，或者再看看「知识闯关」里的题，它们会用一个一个的故事告诉你答案！📚' }
]

// 本地兜底回答（AI 未配置/失败时）
const fallbackReply = (q) => {
  const hit = AI_ANSWERS.find(a => a.match.some(m => q.includes(m)))
  return hit ? hit.reply : `关于「${q}」：这是个很棒的问题！正式版中，平台会调用 DeepSeek 大模型为你生成专属解答，还会通过内容安全过滤后再展示给你哦。现在不妨先去课程中心找找线索～ 🔍`
}

const qaHistory = ref([])
const loadQaHistory = async () => {
  try {
    const list = await request.get('/qa/my')
    qaHistory.value = (list || []).map(r => ({ q: r.question, time: formatDate(r.createdAt) }))
  } catch (e) {
    qaHistory.value = FALLBACK_QA_HISTORY
  }
}

const ask = async (quickQuestion) => {
  const q = (quickQuestion || chatInput.value).trim()
  if (!q) return
  chatInput.value = ''
  msgs.value.push({ role: 'user', text: q })
  const typingMsg = { role: 'ai', text: '', typing: true }
  msgs.value.push(typingMsg)
  try {
    const res = await request.post('/ai/chat', { question: q })
    typingMsg.typing = false
    typingMsg.text = res?.answer || fallbackReply(q)
  } catch (e) {
    // AI 未配置/失败 → 回退本地示例回答
    typingMsg.typing = false
    typingMsg.text = fallbackReply(q)
  }
}

onMounted(() => {
  loadWritingRecords()
  loadQuiz()
  loadQaHistory()
  loadQuizHistory()
})
</script>

<style scoped>
.ai-hero {
  position: relative;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 88% 20%, rgba(255, 255, 255, 0.12) 0%, transparent 42%),
    var(--navy);
  color: #fff;
  padding: 26px 34px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(21, 40, 92, 0.26);
}

.ai-hero-text h1 {
  margin: 0 0 6px;
  font-size: 30px;
  color: #fff;
}

.ai-hero-text h1 svg {
  color: #fff;
  opacity: 0.9;
}

.ai-hero-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
}

.ai-hero-img {
  width: 260px;
  height: 130px;
  object-fit: cover;
  border-radius: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.18);
}

.ai-tabs {
  display: flex;
  gap: 14px;
  margin: 20px 0;
  flex-wrap: wrap;
}

.ai-tab {
  flex: 1;
  min-width: 200px;
  border: none;
  background: #fff;
  border-radius: 18px;
  box-shadow: var(--shadow-card);
  padding: 16px 18px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  transition: all 0.22s ease;
  text-align: left;
  border: 2.5px solid transparent;
}

.ai-tab:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.ai-tab.active {
  border-color: var(--brand);
  background: var(--brand-grad-soft);
}

.tab-emoji {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--brand-grad-soft);
  color: var(--brand);
  margin-bottom: 4px;
  transition: transform 0.22s ease;
}

.ai-tab.active .tab-emoji {
  background: var(--brand-grad);
  color: #fff;
}

.ai-tab:hover .tab-emoji {
  transform: scale(1.06);
}

.tab-title { font-size: 16px; font-weight: 700; color: var(--ink); }
.tab-desc { font-size: 12px; color: var(--ink-3); }

.panel {
  padding: 24px 26px;
}

/* ----- 创意写作 ----- */
.panel-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 26px;
}

.writing-form h3,
.writing-result h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 17px;
}

.writing-form h3 svg,
.writing-result h3 svg {
  color: var(--brand);
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-2);
  margin: 14px 0 8px;
}

.style-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  background: #f3f7fe;
  color: var(--ink-2);
  cursor: pointer;
  border: 1.5px solid transparent;
  font-weight: 600;
  transition: all 0.2s;
}

.chip.active {
  background: var(--brand-grad);
  color: #fff;
}

.length-hint {
  font-size: 12px;
  color: var(--ink-3);
  margin-top: 6px;
}

.gen-btn {
  width: 100%;
  margin-top: 16px;
  font-size: 15px;
}

.gen-anim {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--ink-3);
}

.gen-orb {
  font-size: 54px;
  animation: float-y 1.6s ease-in-out infinite;
}

.result-btns {
  display: flex;
  gap: 12px;
  margin-top: 14px;
  justify-content: flex-end;
}

.history {
  margin-top: 24px;
  border-top: 1.5px dashed var(--line);
  padding-top: 16px;
}

.history h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  margin: 0 0 10px;
}

.history h3 svg {
  color: var(--brand);
}

.history-tip {
  margin: 0 0 10px;
  font-size: 12.5px;
  color: var(--ink-3);
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8faff;
  border-radius: 12px;
  margin-bottom: 8px;
  /* 整行可点击：重新载入到上方编辑器 */
  cursor: pointer;
  border: 1.5px solid transparent;
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
}

.history-item:hover {
  background: var(--brand-grad-soft);
  border-color: var(--brand);
  transform: translateX(2px);
}

.history-item:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

/* 当前正在编辑器里打开的那一条 */
.history-item.is-open {
  background: var(--brand-grad-soft);
  border-color: var(--brand);
}

.h-left { display: flex; flex-direction: column; min-width: 0; }
.h-left b { font-size: 14px; }
.h-left span { font-size: 12px; color: var(--ink-3); }

.h-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.h-open {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #fff;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}

.h-caret {
  color: var(--ink-3);
  opacity: 0.35;
  transition: opacity 0.2s, transform 0.2s, color 0.2s;
}

.history-item:hover .h-caret,
.history-item.is-open .h-caret {
  opacity: 1;
  color: var(--brand);
  transform: translateX(2px);
}

.history-empty {
  margin: 0;
  padding: 14px;
  border-radius: 12px;
  background: #f8faff;
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

/* 已载入历史作品时的说明条：把「这次保存会做什么」讲在前面 */
.opened-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  margin-bottom: 10px;
  border-radius: var(--radius-md);
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  font-size: 12.5px;
  line-height: 1.55;
}

.opened-bar svg { flex-shrink: 0; }

/* 老师已批改的作品：换成暖色提醒，区别于普通草稿 */
.opened-bar.is-graded {
  background: var(--accent-soft);
  color: #97530f;
}

.opened-text { flex: 1; }

.opened-close {
  flex-shrink: 0;
  padding: 3px 12px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: inherit;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.opened-close:hover { background: #fff; }

/* 载入记录后编辑器闪一下，提示内容落到了这里 */
.writing-result.is-flash {
  animation: editor-flash 1.3s ease;
}

@keyframes editor-flash {
  0% { background-color: var(--brand-grad-soft); box-shadow: 0 0 0 8px var(--brand-grad-soft); border-radius: 14px; }
  100% { background-color: transparent; box-shadow: 0 0 0 8px transparent; border-radius: 14px; }
}

/* ----- 闯关 ----- */
.quiz-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.quiz-count {
  font-size: 13px;
  color: var(--ink-2);
}

.quiz-bar {
  flex: 1;
  height: 10px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}

.quiz-bar-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
  transform-origin: left;
  transition: transform 0.35s ease;
}

.quiz-score {
  font-size: 14px;
  color: var(--ink-2);
}

.quiz-score b {
  color: #d97706;
  font-size: 18px;
}

.quiz-q h3 {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  margin: 0 0 18px;
}

.quiz-q h3 svg {
  color: var(--c-orange);
}

.quiz-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.quiz-opt {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: #f8faff;
  border: 2px solid transparent;
  border-radius: 14px;
  font-size: 15px;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s;
  font-family: inherit;
  color: var(--ink);
}

.quiz-opt:hover:not(:disabled) {
  border-color: var(--brand);
  background: var(--brand-grad-soft);
}

.opt-letter {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quiz-opt.correct { border-color: var(--c-green); background: #e5f9ee; }
.quiz-opt.wrong { border-color: var(--c-pink); background: #ffe9f1; }
.quiz-opt.dim { opacity: 0.55; }

.opt-mark { margin-left: auto; font-size: 18px; }

.quiz-feedback {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.good { color: #17934e; font-weight: 700; }
.bad { color: #e5487e; font-weight: 700; }

.quiz-done {
  text-align: center;
  padding: 40px 0 30px;
}

.done-emoji { font-size: 80px; }
.quiz-done h2 { margin: 10px 0 4px; font-size: 26px; }
.quiz-done p { color: var(--ink-2); }

.done-btns {
  display: flex;
  gap: 14px;
  justify-content: center;
  margin-top: 20px;
  flex-wrap: wrap;
}

.quiz-history {
  margin-top: 26px;
  text-align: left;
  border-top: 1px dashed var(--line);
  padding-top: 18px;
}

.quiz-history h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  margin: 0 0 12px;
  color: var(--ink);
}

.quiz-history h3 svg { color: var(--brand); }

.quiz-history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qh-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #f8faff;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 13.5px;
  color: var(--ink-2);
}

.qh-level {
  font-weight: 700;
  color: var(--ink);
}

.qh-score b { color: var(--brand); font-size: 16px; }

.qh-time {
  margin-left: auto;
  color: var(--ink-3);
  font-size: 12.5px;
}

/* ----- 答疑 ----- */
.chat-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 22px;
}

.chat-history h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  margin: 0 0 12px;
}

.chat-history h3 svg {
  color: var(--brand);
}

.his-item {
  padding: 12px 14px;
  background: #f8faff;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.his-item:hover {
  background: var(--brand-grad-soft);
}

.his-item b {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.4;
}

.his-item span {
  font-size: 11px;
  color: var(--ink-3);
}

.chat-tip {
  margin-top: 14px;
  font-size: 12px;
  color: var(--ink-3);
  background: var(--brand-grad-soft);
  padding: 12px 14px;
  border-radius: 12px;
  line-height: 1.7;
}

.chat-window {
  height: 360px;
  overflow-y: auto;
  background: #f8faff;
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--ink-3);
}

.msg {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  font-size: 26px;
  flex-shrink: 0;
}

.bubble {
  max-width: 72%;
  padding: 10px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  background: #fff;
  box-shadow: 0 2px 10px rgba(37, 51, 77, 0.06);
}

.msg.user .bubble {
  background: var(--brand-grad);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg.ai .bubble {
  border-bottom-left-radius: 4px;
}

.chat-input {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

@media (max-width: 900px) {
  .panel-grid,
  .chat-grid { grid-template-columns: 1fr; }
  .quiz-options { grid-template-columns: 1fr; }
  /* 窄屏时右侧的状态标签允许换行，避免挤压标题 */
  .history-item { flex-wrap: wrap; gap: 6px 8px; }
}
</style>
