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

    <!-- 2 AI 实验参数 -->
    <section class="k-section-head">
      <h2><SlidersHorizontal :size="22" weight="bold" /> AI 实验参数</h2>
    </section>
    <p class="field-hint">当前为演示数据，暂无独立管理接口。</p>
    <div class="lab-grid">
      <div v-for="exp in aiExperiments" :key="exp.id" class="k-card exp-card">
        <div class="exp-top">
          <span class="exp-name">{{ exp.name }}</span>
          <button class="mini-btn" @click="openExp(exp)">编辑参数</button>
        </div>
        <div class="exp-params">
          <div class="p"><span>默认风格</span><b>{{ exp.style }}</b></div>
          <div class="p"><span>题目数量</span><b>{{ exp.questions === 0 ? '不限' : exp.questions + ' 题' }}</b></div>
          <div class="p"><span>答题限时</span><b>{{ exp.limit }} 秒</b></div>
        </div>
        <div class="exp-switch">
          <span>敏感词过滤</span>
          <el-switch v-model="exp.filter" />
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
            <el-input v-model="dsConfig.model" placeholder="deepseek-chat" />
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
        <el-form-item label="内容安全">
          <div class="safe-row">
            <el-switch v-model="dsConfig.contentSafe" />
            <span class="safe-label">启用未成年内容过滤</span>
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
    <el-dialog v-model="expVis" :title="`${expForm.name} · 编辑参数`" width="460px">
      <el-form :model="expForm" label-position="top">
        <el-form-item label="默认风格">
          <el-select v-model="expForm.style" placeholder="选择默认风格" class="w-full">
            <el-option v-for="s in styleOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="题目数量">
            <el-input-number v-model="expForm.questions" :min="0" :max="20" controls-position="right" />
          </el-form-item>
          <el-form-item label="答题限时(秒)">
            <el-input-number v-model="expForm.limit" :min="5" :max="600" :step="5" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="敏感词过滤">
          <el-switch v-model="expForm.filter" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="expVis = false">取消</button>
        <button class="k-btn" @click="saveExp">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  PhFloppyDisk as FloppyDisk,
  PhPuzzlePiece as PuzzlePiece,
  PhSlidersHorizontal as SlidersHorizontal,
  PhRobot as Robot,
  PhPlus as Plus,
  PhKey as Key
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

// 2 AI 实验参数（无独立后端接口，保留静态演示数据）
const aiExperiments = ref([
  { id: 1, name: '创意写作实验室', style: '童话风', questions: 3, limit: 60, filter: true },
  { id: 2, name: '知识闯关实验室', style: '竞赛风', questions: 10, limit: 30, filter: true },
  { id: 3, name: '智能答疑实验室', style: '友好助教', questions: 0, limit: 30, filter: false }
])

const expVis = ref(false)
const expForm = ref({})

const openExp = (exp) => {
  expForm.value = { ...exp }
  expVis.value = true
}

const saveExp = () => {
  const target = aiExperiments.value.find((e) => e.id === expForm.value.id)
  if (target) {
    target.style = expForm.value.style
    target.questions = expForm.value.questions
    target.limit = expForm.value.limit
    target.filter = expForm.value.filter
  }
  expVis.value = false
  ElMessage.success('实验参数已更新')
}

// 3 DeepSeek 接口配置
const dsConfig = ref({
  apiKey: '',        // 回显时保持留空，留空表示不修改密钥
  baseUrl: '',       // 后端保存的 baseUrl（表单无输入项，仅回显保存）
  model: 'deepseek-chat',
  timeout: 30,
  concurrency: 8,
  rpm: 120,
  contentSafe: true
})
const maskedKey = ref('')

async function fetchAiConfig() {
  try {
    const data = await request.get('/admin/ai-config')
    if (data && typeof data === 'object') {
      dsConfig.value = {
        apiKey: '',
        baseUrl: data.baseUrl || '',
        model: data.model || 'deepseek-chat',
        timeout: data.timeoutSec ?? 30,
        concurrency: data.maxConcurrency ?? 8,
        rpm: data.rateLimitPerMin ?? 120,
        contentSafe: data.contentFilter !== false
      }
      maskedKey.value = data.apiKey ? `****${String(data.apiKey).slice(-4)}` : ''
    }
  } catch (e) { /* 回退默认值 */ }
}

const saveConfig = async () => {
  try {
    const d = dsConfig.value
    await request.put('/admin/ai-config', {
      apiKey: d.apiKey.trim(),   // 空串时后端保留原密钥
      baseUrl: d.baseUrl,
      model: d.model,
      timeoutSec: d.timeout,
      maxConcurrency: d.concurrency,
      rateLimitPerMin: d.rpm,
      contentFilter: d.contentSafe
    })
    ElMessage.success('DeepSeek 接口配置已保存')
    await fetchAiConfig() // 保存后刷新回显
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(() => {
  fetchTemplates()
  fetchAiConfig()
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

.exp-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
  font-size: 13px;
  color: var(--ink-2);
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
