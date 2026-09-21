<template>
  <div class="k-page a-lab">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><FloppyDisk :size="24" weight="bold" /> 实验资源</h1>
        <p class="sub">维护图形化编程案例模板、AI 交互实验参数与第三方 AI 接口</p>
      </div>
    </div>

    <!-- 1 编程模板 -->
    <section class="k-section-head">
      <h2><PuzzlePiece :size="22" weight="bold" /> 编程模板</h2>
      <button class="k-btn k-btn--ghost" @click="openTemplate">
        <Plus :size="16" weight="bold" /> 新增模板
      </button>
    </section>
    <div class="k-card table-card">
      <el-table :data="templates" style="width: 100%">
        <el-table-column label="模板名" min-width="170">
          <template #default="{ row }">
            <span class="t-name">
              <span v-if="row.emoji" class="t-emoji">{{ row.emoji }}</span>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="110" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="diffClass(row.difficulty)">{{ row.difficulty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="desc" label="接入说明" min-width="260" />
        <el-table-column label="启停" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="onToggleTemplate(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 2 AI 实验参数与安全策略 -->
    <section class="k-section-head">
      <h2><SlidersHorizontal :size="22" weight="bold" /> AI 实验参数与安全策略</h2>
    </section>
    <p class="field-hint">
      每个实验可单独控制内容安全的五道开关。参数改动会立即生效，学生端下次调用即按新策略执行。
    </p>
    <!-- 运行模式不是「正常」时，必须在这里说清楚：下面这些开关点着也不生效 -->
    <div v-if="contentMode !== 1" class="mode-banner" :class="contentMode === 0 ? 'is-off' : 'is-observe'">
      <WarningDiamond :size="16" weight="bold" />
      <span v-if="contentMode === 0">
        内容安全运行模式为<b>完全关闭</b>——下方所有实验的安全策略<b>均不生效</b>，
        违规内容不会被拦截，请在排障结束后立即改回「正常」。
      </span>
      <span v-else>
        内容安全运行模式为<b>观察模式</b>——下方策略仍然在检测、打分并写入日志，
        但<b>不会拦截任何内容</b>。适合用来评估误杀情况。
      </span>
    </div>
    <div class="lab-grid">
      <div v-for="exp in scenePolicies" :key="exp.scene" class="k-card exp-card">
        <div class="exp-top">
          <span class="exp-name">{{ exp.sceneName }}</span>
          <button class="mini-btn" @click="openExp(exp)">编辑参数</button>
        </div>
        <div class="exp-params">
          <div class="p" v-if="exp.scene === 'writing'">
            <span>默认风格</span><b>{{ exp.defaultStyle || '—' }}</b>
          </div>
          <div class="p" v-if="exp.scene === 'quiz'">
            <span>每轮题量</span><b>{{ exp.genCount > 0 ? exp.genCount + ' 题' : '不限' }}</b>
          </div>
          <div class="p" v-if="exp.scene === 'quiz'">
            <span>答题限时</span><b>{{ exp.answerLimitSec > 0 ? exp.answerLimitSec + ' 秒/题' : '不限时' }}</b>
          </div>
          <div class="p" v-if="exp.scene === 'chat'">
            <span>实验参数</span><b>该实验无额外参数</b>
          </div>
        </div>
        <div class="layers">
          <span class="layers-title">安全策略</span>
          <div class="layer-list">
            <button
              v-for="l in LAYER_ITEMS"
              :key="l.key"
              class="layer"
              :class="{ on: exp[l.key] === 1 }"
              :title="l.hint"
              @click="toggleLayer(exp, l)"
            >{{ l.label }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3 DeepSeek 接口配置 -->
    <section class="k-section-head">
      <h2><Robot :size="22" weight="bold" /> DeepSeek 接口配置</h2>
    </section>
    <div class="k-card config-card">
      <el-form :model="dsConfig" label-position="top">
        <el-form-item label="API Key">
          <el-input
            v-model="dsConfig.apiKey"
            type="password"
            show-password
            placeholder="sk-..."
          >
            <template #prefix><Key :size="16" weight="bold" /></template>
          </el-input>
          <p class="field-hint">当前密钥尾号：{{ maskedKey || '未设置' }}。密钥仅保存于后端，留空表示保持不变。</p>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="基础模型名">
            <el-input v-model="dsConfig.model" placeholder="deepseek-flash" />
          </el-form-item>
          <el-form-item label="超时时间(秒)">
            <el-input-number v-model="dsConfig.timeout" :min="5" :max="300" controls-position="right" />
          </el-form-item>
          <el-form-item label="并发限制">
            <el-input-number v-model="dsConfig.concurrency" :min="1" :max="64" controls-position="right" />
          </el-form-item>
          <el-form-item label="每分钟请求数">
            <el-input-number v-model="dsConfig.rpm" :min="10" :max="2000" :step="10" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="内容安全运行模式">
          <el-radio-group v-model="dsConfig.contentMode">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="2">观察模式</el-radio>
            <el-radio :value="0">完全关闭</el-radio>
          </el-radio-group>
          <div class="mode-explain" :class="modeExplain.cls">
            <span>{{ modeExplain.text }}</span>
          </div>
        </el-form-item>
        <button class="k-btn" @click="saveConfig">
          <FloppyDisk :size="17" weight="bold" /> 保存配置
        </button>
      </el-form>
    </div>

    <!-- 新增模板弹窗 -->
    <el-dialog v-model="templateVis" title="新增模板" width="460px">
      <el-form :model="templateForm" label-position="top">
        <el-form-item label="名称">
          <el-input v-model="templateForm.name" placeholder="如：小猫咪动起来" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="templateForm.difficulty" placeholder="选择难度" class="w-full">
            <el-option v-for="d in difficultyOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="接入说明">
          <el-input v-model="templateForm.desc" type="textarea" :rows="3" placeholder="说明该模板如何接入到课堂" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="templateVis = false">取消</button>
        <button class="k-btn" @click="saveTemplate">保存</button>
      </template>
    </el-dialog>

    <!-- 编辑实验参数弹窗 -->
    <el-dialog v-model="expVis" :title="`${expForm.sceneName} · 编辑参数`" width="480px">
      <el-form :model="expForm" label-position="top">
        <el-form-item v-if="expForm.scene === 'writing'" label="默认风格（学生端写作的初始风格）">
          <el-select v-model="expForm.defaultStyle" placeholder="选择默认风格" class="w-full">
            <el-option v-for="s in styleOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <div class="form-row" v-if="expForm.scene === 'quiz'">
          <el-form-item label="每轮题量">
            <el-input-number v-model="expForm.genCount" :min="0" :max="20" controls-position="right" />
          </el-form-item>
          <el-form-item label="答题限时(秒/题)">
            <el-input-number v-model="expForm.answerLimitSec" :min="0" :max="600" :step="5" controls-position="right" />
          </el-form-item>
        </div>
        <p class="field-hint" v-if="expForm.scene === 'quiz'">
          每轮题量为 0 表示使用该关卡的全部题目；答题限时为 0 表示不限时。
        </p>
        <p class="field-hint" v-else-if="expForm.scene === 'chat'">
          智能答疑实验室没有额外实验参数，只需要配置上方的安全策略。
        </p>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="expVis = false">取消</button>
        <button class="k-btn" @click="saveExp">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import {
  PhFloppyDisk as FloppyDisk,
  PhPuzzlePiece as PuzzlePiece,
  PhSlidersHorizontal as SlidersHorizontal,
  PhRobot as Robot,
  PhPlus as Plus,
  PhKey as Key,
  PhWarningDiamond as WarningDiamond
} from '@phosphor-icons/vue'

const difficultyOptions = ['入门', '进阶', '挑战']
const styleOptions = ['童话风', '竞赛风', '科普风', '友好助教', '简洁风']

const diffClass = (d) => (d === '入门' ? 'k-tag--teal' : d === '进阶' ? 'k-tag--orange' : 'k-tag--pink')

// 1 编程模板
const DIFF_LABEL = { 1: '入门', 2: '进阶', 3: '挑战' }
const templates = ref([])

function normalizeTemplate(t) {
  const diff = t.difficulty != null ? t.difficulty : t.level
  const diffLabel = typeof diff === 'string' ? diff : (DIFF_LABEL[diff] || '入门')
  return {
    id: t.id,
    name: t.title || t.name || '',
    emoji: t.emoji || '',
    difficulty: diffLabel,
    desc: t.description || t.desc || '',
    enabled: t.enabled ?? true
  }
}

async function fetchTemplates() {
  try {
    const data = await request.get('/admin/templates')
    templates.value = (Array.isArray(data) ? data : []).map(normalizeTemplate)
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

const templateVis = ref(false)
const templateForm = ref({})

const openTemplate = () => {
  templateForm.value = { name: '', difficulty: '入门', desc: '' }
  templateVis.value = true
}

const saveTemplate = () => {
  const f = templateForm.value
  if (!f.name.trim()) {
    ElMessage.warning('请填写模板名称')
    return
  }
  templates.value.unshift({
    id: Date.now(),
    name: f.name,
    difficulty: f.difficulty,
    desc: f.desc,
    enabled: true
  })
  templateVis.value = false
  ElMessage.success('模板「' + f.name + '」已新增')
}

const onToggleTemplate = async (row) => {
  try {
    await request.put(`/admin/templates/${row.id}/enabled`, { enabled: row.enabled })
    ElMessage.success(row.enabled ? `已启用模板「${row.name}」` : `已停用模板「${row.name}」`)
  } catch (e) {
    // 保存失败回滚开关状态
    row.enabled = !row.enabled
  }
}

/* ------------------------------------------------------------------
   2 AI 实验参数与安全策略
   这里的开关必须落到后端才有意义：数据源是 /admin/ai-scene-policy，
   点击即保存，刷新后状态保持。
   ------------------------------------------------------------------ */
const LAYER_ITEMS = [
  { key: 'inputFilter', label: '输入检测', hint: '学生输入先做合规检测，命中则不调用大模型' },
  { key: 'outputSensitive', label: '敏感词', hint: '对 AI 返回内容做禁用词过滤（level=3 的词直接拦）' },
  { key: 'minorSuitability', label: '适宜性', hint: '按分类权重对全部命中词加权打分，达到阈值才处置' },
  { key: 'accuracyCheck', label: '准确性', hint: '结构完整性 + 模型自我描述泄漏 + 数值型常识比对' },
  { key: 'autoBlock', label: '自动拦截', hint: '关闭后命中只记录、不拦截（排障用）' }
]

const scenePolicies = ref([])

async function fetchScenePolicies() {
  try {
    const data = await request.get('/admin/ai-scene-policy')
    scenePolicies.value = Array.isArray(data) ? data : []
  } catch (e) {
    scenePolicies.value = []
  }
}

async function toggleLayer(exp, layer) {
  const next = exp[layer.key] === 1 ? 0 : 1
  const previous = exp[layer.key]
  exp[layer.key] = next
  try {
    await request.put('/admin/ai-scene-policy', [{ scene: exp.scene, [layer.key]: next }])
    ElMessage.success(`${exp.sceneName} · ${layer.label}已${next === 1 ? '开启' : '关闭'}`)
  } catch (e) {
    exp[layer.key] = previous
  }
}

const expVis = ref(false)
const expForm = ref({})

const openExp = (exp) => {
  expForm.value = {
    scene: exp.scene,
    sceneName: exp.sceneName,
    defaultStyle: exp.defaultStyle,
    genCount: exp.genCount ?? 0,
    answerLimitSec: exp.answerLimitSec ?? 0
  }
  expVis.value = true
}

async function saveExp() {
  const f = expForm.value
  try {
    await request.put('/admin/ai-scene-policy', [{
      scene: f.scene,
      defaultStyle: f.defaultStyle,
      genCount: f.genCount,
      answerLimitSec: f.answerLimitSec
    }])
    ElMessage.success('实验参数已更新')
    expVis.value = false
    fetchScenePolicies()
  } catch (e) { /* 拦截器已提示 */ }
}

// 3 DeepSeek 接口配置
const dsConfig = ref({
  apiKey: '',        // 回显时保持留空，留空表示不修改密钥
  baseUrl: '',       // 后端保存的 baseUrl（表单无输入项，仅回显保存）
  model: 'deepseek-flash',
  timeout: 30,
  concurrency: 8,
  rpm: 120,
  contentMode: 1     // 内容安全运行模式：0 完全关闭 / 1 正常 / 2 观察模式
})
const maskedKey = ref('')

const contentMode = computed(() => dsConfig.value.contentMode)

const modeExplain = computed(() => {
  if (dsConfig.value.contentMode === 0) {
    return {
      cls: 'is-off',
      text: '完全关闭：不走任何内容检测，系统提示词里的安全约束也会去掉。仅限应急排障，界面会全局告警。'
    }
  }
  if (dsConfig.value.contentMode === 2) {
    return {
      cls: 'is-observe',
      text: '观察模式：检测、打分、日志全部照常，只是不再拦截。用来评估误杀率，安全网还在。'
    }
  }
  return {
    cls: '',
    text: '正常：按各实验自己的策略执行检测与拦截。'
  }
})

async function fetchAiConfig() {
  try {
    const data = await request.get('/admin/ai-config')
    if (data && typeof data === 'object') {
      dsConfig.value = {
        apiKey: '',
        baseUrl: data.baseUrl || '',
        model: data.model || 'deepseek-flash',
        timeout: data.timeoutSec ?? 30,
        concurrency: data.maxConcurrency ?? 8,
        rpm: data.rateLimitPerMin ?? 120,
        contentMode: data.contentFilter ?? 1
      }
      maskedKey.value = data.apiKey ? `****${String(data.apiKey).slice(-4)}` : ''
    }
  } catch (e) { /* 回退默认值 */ }
}

const saveConfig = async () => {
  try {
    const d = dsConfig.value
    // 从「正常」切到「完全关闭」需要二次确认：这是唯一会让保护整体失效的操作
    if (d.contentMode === 0) {
      try {
        await ElMessageBox.confirm(
          '完全关闭后，所有实验的内容检测与拦截都会停止，违规内容将直接展示给学生。确定继续吗？',
          '确认关闭内容安全',
          { type: 'warning', confirmButtonText: '确定关闭', cancelButtonText: '取消' }
        )
      } catch (e) {
        return
      }
    }
    await request.put('/admin/ai-config', {
      apiKey: d.apiKey.trim(),   // 空串时后端保留原密钥
      baseUrl: d.baseUrl,
      model: d.model,
      timeoutSec: d.timeout,
      maxConcurrency: d.concurrency,
      rateLimitPerMin: d.rpm,
      // 后端 contentFilter 是 Integer（0 关闭 / 1 正常 / 2 观察），
      // 直接传布尔值会被 Jackson 判为类型不匹配而整个请求 400
      contentFilter: d.contentMode
    })
    ElMessage.success('DeepSeek 接口配置已保存')
    await fetchAiConfig() // 保存后刷新回显
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  fetchTemplates()
  fetchAiConfig()
  fetchScenePolicies()
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

.table-card { padding: 0; overflow: hidden; }

.t-name { font-weight: 600; color: var(--ink); }
.t-emoji { margin-right: 7px; font-size: 17px; vertical-align: middle; }

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

.lab-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}

.exp-card { display: flex; flex-direction: column; gap: 14px; }

.exp-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.exp-name {
  font-family: var(--font-title);
  font-size: 16.5px;
  color: var(--ink);
}

.exp-params {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.exp-params .p {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.exp-params .p span { color: var(--ink-3); }
.exp-params .p b { color: var(--ink); font-weight: 600; }

.layers {
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}

.layers-title {
  display: block;
  font-size: 12px;
  color: var(--ink-3);
  margin-bottom: 8px;
}

.layer-list { display: flex; flex-wrap: wrap; gap: 6px; }

.layer {
  border: 1px solid var(--line);
  background: var(--surface-soft);
  color: var(--ink-3);
  border-radius: 999px;
  padding: 4px 11px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}

.layer:hover { border-color: var(--brand-soft); color: var(--brand-deep); }

.layer.on {
  background: var(--brand-soft);
  border-color: var(--brand-soft);
  color: var(--brand-deep);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-row .el-input-number,
.form-row .el-select,
.w-full { width: 100%; }

.field-hint {
  font-size: 12px;
  color: var(--ink-3);
  margin: 6px 0 0;
}

.safe-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.safe-label { font-size: 14px; color: var(--ink); }

/* 运行模式的即时说明：正常=中性，观察=暖色，完全关闭=红色 */
.mode-explain {
  margin-top: 8px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--ink-3);
}

.mode-explain.is-observe { color: var(--accent-deep); }
.mode-explain.is-off { color: #b8325d; font-weight: 600; }

/* 运行模式不是「正常」时，在实验卡片上方挂横幅，避免开关看着像生效了 */
.mode-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0 0 var(--space-4);
  padding: 11px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.65;
}

.mode-banner svg { flex-shrink: 0; margin-top: 2px; }
.mode-banner b { font-weight: 700; }

.mode-banner.is-observe {
  background: var(--accent-soft);
  border: 1px solid #ffd9b8;
  color: #97530f;
}

.mode-banner.is-off {
  background: #fdeaef;
  border: 1px solid #f8d3de;
  color: #a82f57;
}

.config-card .el-form-item { margin-bottom: 18px; }

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

@media (max-width: 1100px) {
  .lab-grid { grid-template-columns: 1fr; }
}
</style>
