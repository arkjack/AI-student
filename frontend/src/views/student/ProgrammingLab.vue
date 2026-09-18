<template>
  <div class="k-page lab-page">
    <!-- 顶部栏 -->
    <div class="lab-head">
      <div>
        <h1 class="k-h1-icon"><PuzzlePiece weight="bold" /> 编程实验室</h1>
        <p class="sub">拖拽积木块编写程序，运行后立刻看到结果。</p>
      </div>
      <div class="controls">
        <el-select v-model="templateId" placeholder="示例模板" class="tpl-select" @change="onTemplateChange">
          <el-option v-for="t in templates" :key="t.id" :label="`${t.level} · ${t.name}${isDisabled(t.id) ? '（已停用）' : ''}`" :value="t.id" />
        </el-select>
        <el-input v-model="projectName" placeholder="作品名称" class="name-input" />
        <button class="k-btn k-btn--green" @click="run">
          <Play weight="bold" /> 运行
        </button>
        <button class="k-btn" @click="save">
          <FolderOpen weight="bold" /> 保存
        </button>
        <button class="k-btn k-btn--navy" @click="submit">
          <PaperPlaneTilt weight="bold" /> 提交作品
        </button>
      </div>
    </div>

    <div class="lab-grid">
      <!-- 左侧积木工作区 -->
      <div class="k-card block-card">
        <div class="block-tip"><CursorClick weight="bold" /> 从左侧工具箱拖出积木，拼在一起</div>
        <div v-if="currentTemplate" class="tpl-requirement">
          <div class="req-head">
            <span class="req-diff">{{ currentTemplate.level }}</span>
            <b>{{ currentTemplate.name }}</b>
            <button class="mini-btn answer-btn" @click="showAnswer">
              <Eye :size="14" weight="bold" /> 查看标准答案
            </button>
          </div>
          <div class="req-row"><span class="req-label">🎯 任务要求</span><span>{{ currentTemplate.desc }}</span></div>
          <div class="req-row"><span class="req-label">✨ 目标效果</span><span>{{ currentTemplate.goal }}</span></div>
        </div>
        <div ref="blocklyDiv" class="blockly-div"></div>
      </div>

      <!-- 右侧：代码 + 运行 -->
      <div class="right-col">
        <div class="k-card code-card">
          <div class="code-head">
            <h3><FileText weight="bold" /> 生成的代码</h3>
            <span class="lang">JavaScript</span>
          </div>
          <pre class="code-pre">{{ generatedCode || '// 拖入积木后，这里会显示生成的代码' }}</pre>
        </div>

        <div class="k-card run-card">
          <div class="code-head">
            <h3><Monitor weight="bold" /> 运行结果</h3>
            <button class="mini-btn" @click="clearRun">清空</button>
          </div>
          <div class="run-output">
            <div v-if="runLogs.length === 0" class="run-empty">
              <TerminalWindow :size="38" weight="fill" color="#93a2b8" />
              <p>点击「运行」看看程序结果吧</p>
            </div>
            <div v-for="(l, i) in runLogs" :key="i" class="run-line">{{ l }}</div>
          </div>
          <iframe ref="runFrame" class="run-frame-hidden" title="运行沙箱"></iframe>
        </div>
      </div>
    </div>

    <!-- 参考答案弹窗 -->
    <el-dialog v-model="answerVisible" title="参考答案" width="540px">
      <p class="answer-desc">{{ answerDesc }}</p>
      <div class="answer-block">
        <div class="answer-code-label">🧩 标准积木拼法</div>
        <p class="answer-steps">{{ answerSteps }}</p>
      </div>
      <div v-if="answerResult" class="answer-block">
        <div class="answer-code-label">✨ 运行效果</div>
        <pre class="answer-result">{{ answerResult }}</pre>
      </div>
      <template #footer>
        <el-button @click="useAnswer" plain>载入标准积木到工作区</el-button>
        <el-button type="primary" @click="answerVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 我的作品 -->
    <div class="k-section-head">
      <h2>我的作品</h2>
    </div>
    <section class="works-list">
      <div v-for="p in projects" :key="p.id" class="work-item">
        <span class="work-icon"><PuzzlePiece weight="bold" /></span>
        <div class="work-info">
          <b>{{ p.name }}</b>
          <span>{{ p.time }}</span>
        </div>
        <div class="work-right">
          <el-tag v-if="p.status === '已提交'" type="success" round>{{ p.status }}</el-tag>
          <el-tag v-else type="info" round>{{ p.status }}</el-tag>
          <template v-if="p.submit">
            <span class="score">得分 {{ p.score }}</span>
            <span class="feedback" :title="p.feedback"><ChatCircleDots weight="bold" /> {{ p.feedback }}</span>
          </template>
          <button class="mini-btn" @click="loadProject(p)">打开</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import * as Blockly from 'blockly'
import * as zhHans from 'blockly/msg/zh-hans'
import { javascriptGenerator } from 'blockly/javascript'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { PhPuzzlePiece as PuzzlePiece, PhPlay as Play, PhFolderOpen as FolderOpen, PhPaperPlaneTilt as PaperPlaneTilt, PhCursorClick as CursorClick, PhFileText as FileText, PhMonitor as Monitor, PhTerminalWindow as TerminalWindow, PhChatCircleDots as ChatCircleDots, PhEye as Eye } from '@phosphor-icons/vue'

Blockly.setLocale(zhHans)

/* ============ 自定义积木：事件 / 动作 / 外观（丰富工具箱以完成课程任务） ============ */
Blockly.defineBlocksWithJsonArray([
  { type: 'cat_event_start', message0: '🏁 当点击开始时', nextStatement: null, colour: '#e67e22', tooltip: '程序从这里开始运行' },
  { type: 'cat_move_center', message0: '🐱 走到屏幕中央', previousStatement: null, nextStatement: null, colour: '#22c55e', tooltip: '让小猫走到屏幕中央' },
  {
    type: 'cat_move_forward', message0: '🐱 向前走 %1 步',
    args0: [{ type: 'input_value', name: 'STEPS', check: 'Number' }],
    inputsInline: true, previousStatement: null, nextStatement: null, colour: '#22c55e'
  },
  {
    type: 'cat_move_backward', message0: '🐱 向后退 %1 步',
    args0: [{ type: 'input_value', name: 'STEPS', check: 'Number' }],
    inputsInline: true, previousStatement: null, nextStatement: null, colour: '#22c55e'
  },
  { type: 'cat_turn_left', message0: '🐱 向左转', previousStatement: null, nextStatement: null, colour: '#22c55e' },
  { type: 'cat_turn_right', message0: '🐱 向右转', previousStatement: null, nextStatement: null, colour: '#22c55e' },
  {
    type: 'cat_say', message0: '🐱 说 “%1”',
    args0: [{ type: 'input_value', name: 'TEXT', check: 'String' }],
    previousStatement: null, nextStatement: null, colour: '#3b82f6', tooltip: '让小猫说一句话'
  }
])

const g = javascriptGenerator
g.forBlock['cat_event_start'] = () => ''
g.forBlock['cat_move_center'] = () => "console.log('🐱 走到了屏幕中央');\n"
g.forBlock['cat_move_forward'] = (block) => {
  const s = g.valueToCode(block, 'STEPS', 0) || '1'
  return `console.log('🐱 向前走 ' + ${s} + ' 步');\n`
}
g.forBlock['cat_move_backward'] = (block) => {
  const s = g.valueToCode(block, 'STEPS', 0) || '1'
  return `console.log('🐱 向后退 ' + ${s} + ' 步');\n`
}
g.forBlock['cat_turn_left'] = () => "console.log('🐱 向左转');\n"
g.forBlock['cat_turn_right'] = () => "console.log('🐱 向右转');\n"
g.forBlock['cat_say'] = (block) => {
  const text = g.valueToCode(block, 'TEXT', 0) || "'你好'"
  return `console.log('🐱 说：' + ${text});\n`
}
// 让"输出"积木输出到运行结果区域（而非浏览器弹窗 alert）
g.forBlock['text_print'] = (block) => {
  const msg = g.valueToCode(block, 'TEXT', 0) || "''"
  return `console.log(${msg});\n`
}

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_TEMPLATES = [
  { id: 1, name: '小猫咪动起来', desc: '让小猫走到屏幕中央', goal: '运行后小猫从左边走到屏幕中央，并说一句"你好"', level: '入门', code: '小猫说: 你好!',
    answerSteps: '① 🏁 当点击开始时　② 🐱 走到屏幕中央　③ 🐱 说「你好，AI 星球！」',
    answerResult: '🐱 走到了屏幕中央\n🐱 说：你好，AI 星球！' },
  { id: 2, name: '会算数的机器人', desc: '机器人帮你算加减法', goal: '运行后自动算出 1+2 的结果并打印出来', level: '入门', code: '机器人的数学题',
    answerSteps: '① 🔢 设置「结果」= 1 + 2　② 💬 打印「计算结果：」　③ 💬 打印「结果」',
    answerResult: '计算结果：3' },
  { id: 3, name: '小星星循环舞', desc: '用循环画出一片星空', goal: '运行后循环打印 5 颗"⭐星星⭐"和 3 道"✨闪光✨"，形成星光效果', level: '进阶', code: '循环画星星',
    answerSteps: '① 🔁 重复 5 次 → 💬 打印「⭐星星⭐」　② 🔁 重复 3 次 → 💬 打印「✨闪光✨」',
    answerResult: '⭐星星⭐（×5）\n✨闪光✨（×3）' },
  { id: 4, name: '自动巡逻车', desc: '条件判断：碰到墙就转弯', goal: '运行后根据条件判断输出"向前走"或"转向"', level: '进阶', code: '自动巡逻车',
    answerSteps: '① ❓ 如果 3 < 5 → 💬 打印「3比5小，向前走！」　否则 → 💬 打印「转向！」',
    answerResult: '3 比 5 小，向前走！' }
]
const FALLBACK_PROJECTS = [
  { id: 1, name: '小猫咪动起来', status: '已提交', score: 95, feedback: '写得很棒！可以尝试让小猫转个圈～', time: '2026-08-30', submit: true, blocksJson: null },
  { id: 2, name: '会算数的机器人', status: '已提交', score: 88, feedback: '思路清晰，注意积木的连接顺序哦。', time: '2026-08-26', submit: true, blocksJson: null },
  { id: 3, name: '我的第一个作品', status: '草稿', score: null, feedback: null, time: '2026-08-20', submit: false, blocksJson: null }
]

const formatDate = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

const blocklyDiv = ref(null)
const templates = FALLBACK_TEMPLATES
const projects = ref([])
const templateId = ref(null)
const currentTemplate = computed(() => templates.find(t => t.id === templateId.value) || null)

// 模板启停状态：由后端返回启用模板列表判断
const enabledIds = ref([])
const templateStatusLoaded = ref(false)
const isDisabled = (id) => {
  if (!templateStatusLoaded.value) return false
  return templates.some(t => t.id === id) && !enabledIds.value.includes(Number(id))
}
async function loadTemplateStatus() {
  try {
    const list = await request.get('/project/templates')
    enabledIds.value = (Array.isArray(list) ? list : []).map(t => Number(t.id))
  } catch (e) {
    // 接口失败时视为全部启用，避免误禁用
    enabledIds.value = templates.map(t => t.id)
  } finally {
    templateStatusLoaded.value = true
  }
}
// 从作业中心跳转而来时携带的任务 id（提交作品时据此关联作业提交）
const route = useRoute()
const taskAssignmentId = computed(() => (route.query.assignmentId ? Number(route.query.assignmentId) : null))
const projectName = ref('我的第一个作品')
const generatedCode = ref('')
const runLogs = ref([])
const runFrame = ref(null)

let ws = null

// 简化工具箱：控制 / 逻辑 / 数学 / 文本 / 变量 / 函数
const toolbox = {
  kind: 'categoryToolbox',
  contents: [
    {
      kind: 'category', name: '事件', colour: '#e67e22',
      contents: [
        { kind: 'block', type: 'cat_event_start' }
      ]
    },
    {
      kind: 'category', name: '控制', colour: '#3b82f6',
      contents: [
        { kind: 'block', type: 'controls_repeat_ext' },
        { kind: 'block', type: 'controls_if' },
        { kind: 'block', type: 'controls_whileUntil' }
      ]
    },
    {
      kind: 'category', name: '逻辑', colour: '#f7b731',
      contents: [
        { kind: 'block', type: 'logic_compare' },
        { kind: 'block', type: 'logic_operation' },
        { kind: 'block', type: 'logic_boolean' }
      ]
    },
    {
      kind: 'category', name: '数学', colour: '#2ecc71',
      contents: [
        { kind: 'block', type: 'math_number' },
        { kind: 'block', type: 'math_arithmetic' }
      ]
    },
    {
      kind: 'category', name: '文本', colour: '#2f5cd8',
      contents: [
        { kind: 'block', type: 'text' },
        { kind: 'block', type: 'text_print' }
      ]
    },
    {
      kind: 'category', name: '动作', colour: '#22c55e',
      contents: [
        { kind: 'block', type: 'cat_move_center' },
        { kind: 'block', type: 'cat_move_forward' },
        { kind: 'block', type: 'cat_move_backward' },
        { kind: 'block', type: 'cat_turn_left' },
        { kind: 'block', type: 'cat_turn_right' }
      ]
    },
    {
      kind: 'category', name: '外观', colour: '#3b82f6',
      contents: [
        { kind: 'block', type: 'cat_say' }
      ]
    },
    {
      kind: 'category', name: '变量', colour: '#ff6b9d', custom: 'VARIABLE'
    },
    {
      kind: 'category', name: '函数', colour: '#22c3aa', custom: 'PROCEDURE'
    }
  ]
}

// 示例模板的积木 XML
const TEMPLATE_XML = {
  1: `<xml xmlns="https://developers.google.com/blockly/xml">
        <block type="cat_event_start" x="20" y="20">
          <next>
            <block type="cat_move_center">
              <next>
                <block type="cat_say">
                  <value name="TEXT"><shadow type="text"><field name="TEXT">你好，AI 星球！</field></shadow></value>
                </block>
              </next>
            </block>
          </next>
        </block>
      </xml>`,
  2: `<xml xmlns="https://developers.google.com/blockly/xml">
        <block type="variables_set" x="20" y="20">
          <field name="VAR">结果</field>
          <value name="VALUE">
            <block type="math_arithmetic">
              <field name="OP">ADD</field>
              <value name="A"><shadow type="math_number"><field name="NUM">1</field></shadow></value>
              <value name="B"><shadow type="math_number"><field name="NUM">2</field></shadow></value>
            </block>
          </value>
        </block>
        <block type="text_print" x="20" y="110">
          <value name="TEXT"><shadow type="text"><field name="TEXT">计算结果：</field></shadow></value>
        </block>
        <block type="text_print" x="20" y="160">
          <value name="TEXT"><block type="variables_get"><field name="VAR">结果</field></block></value>
        </block>
      </xml>`,
  3: `<xml xmlns="https://developers.google.com/blockly/xml">
        <block type="controls_repeat_ext" x="20" y="20">
          <value name="TIMES"><shadow type="math_number"><field name="NUM">5</field></shadow></value>
          <statement name="DO">
            <block type="text_print"><value name="TEXT"><shadow type="text"><field name="TEXT">⭐ 星星 ⭐</field></shadow></value></block>
          </statement>
        </block>
        <block type="controls_repeat_ext" x="20" y="110">
          <value name="TIMES"><shadow type="math_number"><field name="NUM">3</field></shadow></value>
          <statement name="DO">
            <block type="text_print"><value name="TEXT"><shadow type="text"><field name="TEXT">✨ 闪光 ✨</field></shadow></value></block>
          </statement>
        </block>
      </xml>`,
  4: `<xml xmlns="https://developers.google.com/blockly/xml">
        <block type="controls_if" x="20" y="20">
          <value name="IF0">
            <block type="logic_compare">
              <field name="OP">LT</field>
              <value name="A"><shadow type="math_number"><field name="NUM">3</field></shadow></value>
              <value name="B"><shadow type="math_number"><field name="NUM">5</field></shadow></value>
            </block>
          </value>
          <statement name="DO0">
            <block type="text_print"><value name="TEXT"><shadow type="text"><field name="TEXT">3 比 5 小，向前走！</field></shadow></value></block>
          </statement>
          <statement name="ELSE">
            <block type="text_print"><value name="TEXT"><shadow type="text"><field name="TEXT">转向！</field></shadow></value></block>
          </statement>
        </block>
      </xml>`
}

onMounted(() => {
  ws = Blockly.inject(blocklyDiv.value, {
    toolbox,
    trashcan: true,
    zoom: { controls: false, wheel: true, startScale: 0.9 },
    grid: { spacing: 22, length: 3, colour: '#edf2fc', snap: true },
    move: { scrollbars: true, drag: true, wheel: true },
    theme: Blockly.Themes.Classic
  })

  // 监听变化实时生成代码
  ws.addChangeListener(() => {
    try {
      generatedCode.value = javascriptGenerator.workspaceToCode(ws)
    } catch (e) {
      generatedCode.value = '// 代码生成中…'
    }
  })

  loadTemplateStatus()
  loadProjects()
})

onBeforeUnmount(() => {
  if (ws) ws.dispose()
})

// 选择模板：清空工作区并显示要求，不自动加载积木（标准积木需主动点「载入标准积木」）
let prevTemplateId = null
const onTemplateChange = (id) => {
  if (!ws) return
  // 已停用模板：弹出提示并回退选择，禁止使用
  if (id && isDisabled(id)) {
    ElMessage.warning('当前资源已关闭请联系管理员')
    templateId.value = prevTemplateId
    return
  }
  prevTemplateId = id
  ws.clear()
  if (id) projectName.value = templates.find(t => t.id === id)?.name || projectName.value
  ElMessage.success(id ? `已选择模板「${templates.find(t => t.id === id)?.name}」，请从左侧工具箱拼积木` : '已清空工作区')
}

// 载入标准积木到工作区（仅用户点击「载入标准积木到工作区」时）
const loadStandardBlocks = (id) => {
  if (!id || !ws) return
  const xml = TEMPLATE_XML[id]
  ws.clear()
  if (xml) {
    // 用 Blockly 官方 textToDom 解析 XML（DOMParser 的 namespace 不被 Blockly 识别）
    const dom = Blockly.utils.xml.textToDom(xml)
    Blockly.Xml.domToWorkspace(dom, ws)
    // 清空后把积木整齐排列到工作区顶部，避免残留在角落
    ws.cleanUp()
    projectName.value = templates.find(t => t.id === id)?.name || projectName.value
    ElMessage.success('已载入标准积木，可对照查看')
  }
}

/* ---------- 查看标准答案 ---------- */
const answerVisible = ref(false)
const answerSteps = ref('')
const answerResult = ref('')
const answerDesc = ref('')
const showAnswer = () => {
  const id = templateId.value
  if (!id) {
    ElMessage.warning('请先选择一个模板')
    return
  }
  const t = templates.find(x => x.id === id)
  answerDesc.value = t ? `${t.name} · ${t.desc}。达成效果：${t.goal}` : ''
  answerSteps.value = t?.answerSteps || '（该模板暂无拼法说明）'
  answerResult.value = t?.answerResult || ''
  answerVisible.value = true
}

const useAnswer = () => {
  const id = templateId.value
  if (id) loadStandardBlocks(id)
  answerVisible.value = false
}

const loadProjects = async () => {
  try {
    const list = await request.get('/project/my')
    projects.value = (list || []).map(p => ({
      id: p.id,
      name: p.title,
      blocksJson: p.blocksJson,
      codeText: p.codeText,
      status: p.status === 1 ? '已提交' : '草稿',
      submit: p.status === 1,
      score: p.score,
      feedback: p.feedback,
      time: formatDate(p.submittedAt || p.createdAt)
    }))
  } catch (e) {
    projects.value = FALLBACK_PROJECTS
  }
}

const loadProject = (p) => {
  projectName.value = p.name
  if (ws && p.blocksJson) {
    try {
      const state = JSON.parse(p.blocksJson)
      Blockly.serialization.workspaces.load(state, ws)
      ElMessage.success(`已加载「${p.name}」`)
    } catch (e) {
      ElMessage.warning('该作品的积木数据无法解析，已保留当前工作区')
    }
  } else {
    ElMessage.info(`已选择「${p.name}」`)
  }
}

const run = () => {
  if (templateId.value && isDisabled(templateId.value)) {
    ElMessage.warning('当前资源已关闭请联系管理员')
    return
  }
  const code = javascriptGenerator.workspaceToCode(ws)
  generatedCode.value = code
  runLogs.value = []
  if (!code.trim()) {
    ElMessage.warning('先拖一些积木到工作区吧～')
    return
  }
  executeInSandbox(code)
}

// 在沙箱 iframe 中执行生成的代码，捕获 console.log
const executeInSandbox = (code) => {
  if (!runFrame.value) return
  const html = `<scr` + `ipt>
    const send = (t) => parent.postMessage({ type: 'log', text: String(t) }, '*');
    const origLog = console.log;
    console.log = (...a) => { a.forEach(x => send(x)); origLog(...a); };
    try {
      ${code.replace(/<\/script>/g, '<\\/script>')}
      send('[程序运行结束]');
    } catch (e) {
      send('❌ 出错了: ' + e.message);
    }
  </scr` + `ipt>`
  runFrame.value.srcdoc = html
}

// 接收 iframe 的日志
const onFrameMessage = (e) => {
  if (e.data && e.data.type === 'log') {
    runLogs.value.push(e.data.text)
  }
}
window.addEventListener('message', onFrameMessage)

const clearRun = () => {
  runLogs.value = []
}

const save = async () => {
  if (!ws) return
  const blocksJson = JSON.stringify(Blockly.serialization.workspaces.save(ws))
  const codeText = generatedCode.value || javascriptGenerator.workspaceToCode(ws)
  try {
    await request.post('/project/save', { title: projectName.value, blocksJson, codeText, status: 0 })
    ElMessage.success(`作品「${projectName.value}」已保存到我的作品 ✨`)
    loadProjects()
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}

const submit = async () => {
  if (!ws) return
  if (templateId.value && isDisabled(templateId.value)) {
    ElMessage.warning('当前资源已关闭请联系管理员')
    return
  }
  const blocks = ws.getAllBlocks(false)
  const codeText = generatedCode.value || javascriptGenerator.workspaceToCode(ws)
  // 非空校验：至少要有积木或代码内容
  if ((!blocks || blocks.length === 0) && (!codeText || !codeText.trim())) {
    ElMessage.warning('作品还是空的，请先搭一些积木再提交～')
    return
  }
  const blocksJson = JSON.stringify(Blockly.serialization.workspaces.save(ws))
  try {
    await request.post('/project/save', { title: projectName.value, blocksJson, codeText, status: 1 })
    // 关联作业提交：优先用 URL 携带的任务 id，否则尝试该学生可提交的编程任务
    const asgId = taskAssignmentId.value
    let linked = false
    if (asgId) {
      try {
        await request.post('/homework/submit', { assignmentId: asgId, content: codeText || '提交了积木作品' })
        linked = true
      } catch (e) {}
    } else {
      try {
        const assigns = await request.get('/homework/assignments')
        const progTask = (assigns || []).find(a => a.type === 2)
        if (progTask) {
          await request.post('/homework/submit', { assignmentId: progTask.id, content: codeText || '提交了积木作品' })
          linked = true
        }
      } catch (e) {}
    }
    ElMessage.success(linked ? '作品已提交！等待老师批改～ 🎉' : '作品已提交！')
    loadProjects()
  } catch (e) {
    // 失败仅静默（拦截器已提示）
  }
}
</script>

<style scoped>
.lab-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.lab-head h1 {
  margin: 4px 0 4px;
  font-size: 30px;
}

.sub {
  margin: 0;
  color: var(--ink-2);
}

.controls {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.tpl-select { width: 220px; }
.name-input { width: 180px; }

.lab-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 20px;
  align-items: start;
}

.block-card {
  padding: 14px;
}

.block-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-3);
  padding: 0 4px 10px;
}

.block-tip svg {
  color: var(--brand);
}

/* 模板要求面板 */
.tpl-requirement {
  background: linear-gradient(135deg, #eef4ff, #eef8ff);
  border: 1.5px solid #d8e6ff;
  border-radius: var(--radius-md);
  padding: 12px 16px;
  margin-bottom: 12px;
}

.req-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.req-head b {
  font-size: 15px;
  color: var(--ink);
}

.req-diff {
  background: var(--brand-grad);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 999px;
}

.req-row {
  display: flex;
  gap: 10px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--ink-2);
}

.req-row + .req-row {
  margin-top: 4px;
}

.req-label {
  flex-shrink: 0;
  font-weight: 700;
  color: var(--brand-deep);
}

/* 查看标准答案 */
.answer-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  background: var(--brand-grad);
  color: #fff;
}

.answer-btn:hover {
  background: var(--brand-deep);
}

.answer-desc {
  margin: 0 0 12px;
  color: var(--ink-2);
  font-size: 13.5px;
  line-height: 1.6;
}

.answer-block {
  background: #f8faff;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.answer-code-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--ink-3);
  margin-bottom: 6px;
}

.answer-steps {
  margin: 0;
  font-size: 14px;
  color: var(--ink);
  line-height: 1.7;
}

.answer-result {
  margin: 0;
  font-size: 13px;
  color: var(--brand-deep);
  line-height: 1.7;
  white-space: pre-line;
  font-family: 'Cascadia Code', Consolas, monospace;
}

.blockly-div {
  height: 560px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 2px solid var(--line);
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.code-card,
.run-card {
  padding: 16px 18px;
}

.code-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.code-head h3 {
  margin: 0;
  font-size: 15px;
}

.lang {
  font-size: 11px;
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 700;
}

.code-pre {
  background: #0f172a;
  color: #a5f3fc;
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 12.5px;
  line-height: 1.7;
  margin: 0;
  min-height: 130px;
  max-height: 210px;
  overflow: auto;
  font-family: 'Cascadia Code', Consolas, monospace;
}

.mini-btn {
  border: none;
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.mini-btn:hover {
  background: var(--brand-grad);
  color: #fff;
}

.run-output {
  min-height: 120px;
  max-height: 220px;
  overflow: auto;
  background: #f6f9ff;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
}

.run-empty {
  text-align: center;
  color: var(--ink-3);
  padding: 18px 0;
}

.run-line {
  padding: 3px 0;
  border-bottom: 1px dashed var(--line);
}

.run-line:last-child {
  border-bottom: none;
}

.run-frame-hidden {
  position: absolute;
  width: 0;
  height: 0;
  border: 0;
  visibility: hidden;
}

/* 作品（无卡列表） */
.works-list {
  display: flex;
  flex-direction: column;
}

.work-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--line);
  transition: background 0.2s;
}

.work-item:last-child {
  border-bottom: none;
}

.work-item:hover .work-info b {
  color: var(--brand);
}

.work-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--brand-grad-soft);
  color: var(--brand);
  flex-shrink: 0;
}

.work-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.work-info b {
  font-size: 14px;
}

.work-info span {
  font-size: 12px;
  color: var(--ink-3);
}

.work-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.score {
  font-size: 13px;
  font-weight: 700;
  color: #d97706;
}

.feedback {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-2);
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1080px) {
  .lab-grid { grid-template-columns: 1fr; }
}
</style>
