<template>
  <div class="k-page a-safety">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><ShieldCheck :size="24" weight="bold" /> 内容安全</h1>
        <p class="sub">维护敏感词库，查看每一次 AI 交互的处置结果，并对疑似内容做兜底复核</p>
      </div>
      <button class="k-btn k-btn--ghost" @click="reloadAll">
        <ArrowClockwise :size="16" weight="bold" /> 刷新
      </button>
    </div>

    <!-- 运行模式不是「正常」时必须在这里也告警：本页是管理员看内容安全的第一入口 -->
    <div v-if="contentMode !== 1" class="mode-banner" :class="contentMode === 0 ? 'is-off' : 'is-observe'">
      <WarningDiamond :size="16" weight="bold" />
      <span v-if="contentMode === 0">
        内容安全运行模式为<b>完全关闭</b>——当前既不检测也不拦截。日志仍会记录每次调用，
        但不会有任何命中信息。请尽快到「实验资源」页改回「正常」。
      </span>
      <span v-else>
        内容安全运行模式为<b>观察模式</b>——检测、打分、日志全部照常，但<b>不会拦截</b>任何内容。
        这条链路目前的命中记录正适合用来评估误杀情况。
      </span>
    </div>

    <!-- 概览 -->
    <div class="stat-grid">
      <div v-for="s in statCards" :key="s.key" class="k-card stat">
        <span class="stat-label">{{ s.label }}</span>
        <b class="stat-value" :class="s.cls">{{ summary[s.key] ?? 0 }}</b>
        <span class="stat-hint">{{ s.hint }}</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="safety-tabs">
      <!-- ==================== 敏感词库 ==================== -->
      <el-tab-pane name="words">
        <template #label>
          <span class="tab-label"><WarningDiamond :size="16" weight="bold" /> 敏感词库</span>
        </template>

        <div class="toolbar">
          <el-input
            v-model="wordQuery.keyword"
            placeholder="搜索敏感词"
            clearable
            class="w-200"
            @keyup.enter="searchWords"
            @clear="searchWords"
          />
          <el-select v-model="wordQuery.category" placeholder="全部分类" clearable class="w-140" @change="searchWords">
            <el-option v-for="c in CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
          <el-select v-model="wordQuery.enabled" placeholder="全部状态" clearable class="w-120" @change="searchWords">
            <el-option label="已启用" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
          <button class="k-btn k-btn--ghost" @click="searchWords">
            <MagnifyingGlass :size="16" weight="bold" /> 查询
          </button>
          <div class="toolbar-gap" />
          <button class="k-btn k-btn--ghost" @click="openImport">
            <UploadSimple :size="16" weight="bold" /> 批量导入
          </button>
          <button class="k-btn" @click="openWord()">
            <Plus :size="16" weight="bold" /> 新增敏感词
          </button>
        </div>

        <div class="k-card table-card">
          <el-table :data="words" style="width: 100%" v-loading="wordLoading">
            <el-table-column prop="word" label="敏感词" min-width="140">
              <template #default="{ row }"><span class="w-name">{{ row.word }}</span></template>
            </el-table-column>
            <el-table-column label="分类" width="120" align="center">
              <template #default="{ row }">
                <span class="k-tag" :class="categoryClass(row.category)">{{ categoryLabel(row.category) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="级别" width="150" align="center">
              <template #default="{ row }">
                <span class="lv" :class="'lv-' + row.level">{{ levelLabel(row.level) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
            <el-table-column label="启用" width="90" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled === 1"
                  @change="(v) => toggleWord(row, v)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <div class="ops">
                  <button class="mini-btn" @click="openWord(row)">编辑</button>
                  <button class="mini-btn is-danger" @click="removeWord(row)">删除</button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <p class="empty">词库为空，可先「批量导入」一份词表</p>
            </template>
          </el-table>

          <div class="table-foot">
            <span class="total">共 {{ wordTotal }} 条 · 生效中的词才会参与匹配</span>
            <el-pagination
              background
              layout="prev, pager, next"
              :total="wordTotal"
              :page-size="wordQuery.size"
              v-model:current-page="wordQuery.page"
              @current-change="fetchWords"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ==================== 交互日志 ==================== -->
      <el-tab-pane name="logs">
        <template #label>
          <span class="tab-label"><ListMagnifyingGlass :size="16" weight="bold" /> 交互日志</span>
        </template>

        <div class="toolbar">
          <el-select v-model="logQuery.scene" placeholder="全部场景" clearable class="w-160" @change="searchLogs">
            <el-option v-for="s in SCENE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="logQuery.hitStage" placeholder="全部层级" clearable class="w-150" @change="searchLogs">
            <el-option v-for="s in STAGE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-select v-model="logQuery.reviewStatus" placeholder="全部复核状态" clearable class="w-150" @change="searchLogs">
            <el-option v-for="s in REVIEW_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-input
            v-model="logQuery.keyword"
            placeholder="搜索输入 / 输出 / 命中词"
            clearable
            class="w-220"
            @keyup.enter="searchLogs"
            @clear="searchLogs"
          />
          <button class="k-btn k-btn--ghost" @click="searchLogs">
            <MagnifyingGlass :size="16" weight="bold" /> 查询
          </button>
        </div>

        <div class="k-card table-card">
          <el-table :data="logs" style="width: 100%" v-loading="logLoading">
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ row.createdAt }}</template>
            </el-table-column>
            <el-table-column label="学生" width="120">
              <template #default="{ row }">
                <span class="stu"><span v-if="row.avatar">{{ row.avatar }}</span>{{ row.studentName || ('#' + row.userId) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="场景" width="130">
              <template #default="{ row }">{{ row.sceneName }}</template>
            </el-table-column>
            <el-table-column label="处理层级" width="120" align="center">
              <template #default="{ row }">
                <span class="k-tag" :class="stageClass(row.hitStage)">{{ row.hitStageText }}</span>
              </template>
            </el-table-column>
            <el-table-column label="处置" width="120" align="center">
              <template #default="{ row }">{{ row.actionText }}</template>
            </el-table-column>
            <el-table-column label="风险分" width="90" align="center">
              <template #default="{ row }">
                <span class="risk" :class="riskClass(row.riskScore)">{{ row.riskScore }}</span>
              </template>
            </el-table-column>
            <el-table-column label="输入" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.inputText || '—' }}</template>
            </el-table-column>
            <el-table-column label="复核" width="110" align="center">
              <template #default="{ row }">
                <span class="k-tag" :class="reviewClass(row.reviewStatus)">{{ row.reviewStatusText }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <button class="mini-btn" @click="openLog(row)">查看</button>
              </template>
            </el-table-column>
            <template #empty>
              <p class="empty">暂无交互记录，学生使用一次 AI 实验后这里就会有数据</p>
            </template>
          </el-table>

          <div class="table-foot">
            <span class="total">共 {{ logTotal }} 条</span>
            <el-pagination
              background
              layout="prev, pager, next"
              :total="logTotal"
              :page-size="logQuery.size"
              v-model:current-page="logQuery.page"
              @current-change="fetchLogs"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 编辑敏感词 -->
    <el-dialog v-model="wordVis" :title="wordForm.id ? '编辑敏感词' : '新增敏感词'" width="460px">
      <el-form :model="wordForm" label-position="top">
        <el-form-item label="敏感词">
          <el-input v-model="wordForm.word" placeholder="如：赌博" maxlength="50" show-word-limit />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="wordForm.category" class="w-full">
              <el-option v-for="c in CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="级别">
            <el-select v-model="wordForm.level" class="w-full">
              <el-option v-for="l in LEVEL_OPTIONS" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
          </el-form-item>
        </div>
        <p class="field-hint">{{ levelHint }}</p>
        <el-form-item label="备注">
          <el-input v-model="wordForm.remark" placeholder="说明收录原因，便于后续维护" maxlength="120" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="wordForm.enabledBool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="wordVis = false">取消</button>
        <button class="k-btn" @click="saveWord">保存</button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVis" title="批量导入敏感词" width="520px">
      <el-form :model="importForm" label-position="top">
        <el-form-item label="词表（一行一个词）">
          <el-input
            v-model="importForm.text"
            type="textarea"
            :rows="8"
            placeholder="赌博&#10;诈骗&#10;加微信"
          />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="统一分类">
            <el-select v-model="importForm.category" class="w-full">
              <el-option v-for="c in CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="统一级别">
            <el-select v-model="importForm.level" class="w-full">
              <el-option v-for="l in LEVEL_OPTIONS" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
          </el-form-item>
        </div>
        <p class="field-hint">已存在的词会自动跳过，单次最多导入 500 条。</p>
      </el-form>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="importVis = false">取消</button>
        <button class="k-btn" @click="doImport">导入</button>
      </template>
    </el-dialog>

    <!-- 日志详情 + 复核 -->
    <el-dialog v-model="logVis" title="交互详情" width="640px">
      <div v-if="currentLog" class="log-detail">
        <div class="ld-head">
          <span class="stu"><span v-if="currentLog.avatar">{{ currentLog.avatar }}</span>{{ currentLog.studentName || ('#' + currentLog.userId) }}</span>
          <span class="k-tag" :class="stageClass(currentLog.hitStage)">{{ currentLog.hitStageText }}</span>
          <span class="k-tag" :class="reviewClass(currentLog.reviewStatus)">{{ currentLog.reviewStatusText }}</span>
        </div>
        <div class="ld-meta">
          <span>{{ currentLog.sceneName }}</span>
          <span>{{ currentLog.createdAt }}</span>
          <span v-if="currentLog.model">模型 {{ currentLog.model }}</span>
          <span v-if="currentLog.elapsedMs != null">耗时 {{ currentLog.elapsedMs }}ms</span>
          <span>风险分 {{ currentLog.riskScore }}</span>
        </div>
        <div class="ld-block">
          <h4>学生输入</h4>
          <p class="ld-text">{{ currentLog.inputText || '（无输入，如出题场景）' }}</p>
        </div>
        <div class="ld-block">
          <h4>AI 原始返回</h4>
          <p class="ld-text">{{ currentLog.outputText || '（未调用大模型，输入阶段已被拦截）' }}</p>
        </div>
        <div class="ld-block">
          <h4>安全判定</h4>
          <p class="ld-text">
            处置动作：{{ currentLog.actionText }}<br />
            <template v-if="currentLog.hitWords">命中词：{{ currentLog.hitWords }}<br /></template>
            <template v-if="currentLog.reason">原因：{{ currentLog.reason }}</template>
          </p>
        </div>
        <div v-if="currentLog.reviewStatus !== 1" class="ld-block">
          <h4>复核结果</h4>
          <p class="ld-text">
            {{ currentLog.reviewStatusText }}
            <template v-if="currentLog.reviewerName">· {{ currentLog.reviewerName }}</template>
            <template v-if="currentLog.reviewRemark"><br />意见：{{ currentLog.reviewRemark }}</template>
          </p>
        </div>
        <div v-if="currentLog.reviewStatus === 1" class="ld-block">
          <h4>管理员兜底复核</h4>
          <el-input v-model="reviewRemark" type="textarea" :rows="3" placeholder="复核意见（可选）" />
        </div>
      </div>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="logVis = false">关闭</button>
        <template v-if="currentLog && currentLog.reviewStatus === 1">
          <button class="k-btn k-btn--ghost" @click="doReview(3)">驳回</button>
          <button class="k-btn" @click="doReview(2)">复核通过</button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import {
  PhShieldCheck as ShieldCheck,
  PhWarningDiamond as WarningDiamond,
  PhListMagnifyingGlass as ListMagnifyingGlass,
  PhMagnifyingGlass as MagnifyingGlass,
  PhPlus as Plus,
  PhUploadSimple as UploadSimple,
  PhArrowClockwise as ArrowClockwise
} from '@phosphor-icons/vue'

import {
  CATEGORY_OPTIONS,
  LEVEL_OPTIONS,
  SCENE_OPTIONS,
  STAGE_OPTIONS,
  REVIEW_OPTIONS,
  categoryLabel,
  categoryClass,
  levelLabel,
  stageClass,
  reviewClass,
  riskClass,
  cleanParams as clean
} from '@/utils/contentSafety'

const activeTab = ref('words')
const summary = ref({})
/** 内容安全运行模式：0 完全关闭 / 1 正常 / 2 观察模式 */
const contentMode = ref(1)
const statCards = [
  { key: 'total', label: '近 7 天 AI 调用', hint: '含通过、拦截与降级', cls: '' },
  { key: 'blocked', label: '其中被拦截', hint: '输入或输出命中处置', cls: 'is-danger' },
  { key: 'replaced', label: '被替换', hint: '内容被换成安全文案', cls: 'is-warn' },
  { key: 'pendingReview', label: '待复核', hint: '需要人工确认', cls: 'is-warn' }
]

async function fetchSummary() {
  try {
    summary.value = (await request.get('/admin/content/summary', { params: { days: 7 } })) || {}
  } catch (e) { /* 拦截器已提示 */ }
}

/** 读取运行模式，用来在本页顶部决定要不要挂告警横幅 */
async function fetchContentMode() {
  try {
    const cfg = await request.get('/admin/ai-config')
    contentMode.value = cfg?.contentFilter ?? 1
  } catch (e) {
    contentMode.value = 1
  }
}

/* ---------------- 敏感词库 ---------------- */
const words = ref([])
const wordTotal = ref(0)
const wordLoading = ref(false)
const wordQuery = ref({ keyword: '', category: null, enabled: null, page: 1, size: 10 })

async function fetchWords() {
  wordLoading.value = true
  try {
    const data = await request.get('/admin/content/words', { params: clean(wordQuery.value) })
    words.value = data?.records || []
    wordTotal.value = data?.total || 0
  } catch (e) {
    words.value = []
    wordTotal.value = 0
  } finally {
    wordLoading.value = false
  }
}

const searchWords = () => {
  wordQuery.value.page = 1
  fetchWords()
}

const wordVis = ref(false)
const wordForm = ref({})

const levelHint = computed(() => {
  const v = wordForm.value.level
  if (v === 1) return '级别 1：只参与适宜性风险打分，不单独触发拦截。'
  if (v === 3) return '级别 3：命中即按「敏感词过滤」直接拦截（输入侧则不会调用大模型）。'
  return '级别 2：命中后由适宜性评估加权，达到阈值才替换或拦截。'
})

function openWord(row) {
  if (row) {
    wordForm.value = {
      id: row.id,
      word: row.word,
      category: row.category,
      level: row.level,
      remark: row.remark || '',
      enabledBool: row.enabled === 1
    }
  } else {
    wordForm.value = { id: null, word: '', category: 7, level: 2, remark: '', enabledBool: true }
  }
  wordVis.value = true
}

async function saveWord() {
  const f = wordForm.value
  if (!f.word || !f.word.trim()) {
    ElMessage.warning('请填写敏感词')
    return
  }
  const body = {
    word: f.word.trim(),
    category: f.category,
    level: f.level,
    remark: f.remark,
    enabled: f.enabledBool ? 1 : 0
  }
  try {
    if (f.id) {
      await request.put(`/admin/content/words/${f.id}`, body)
      ElMessage.success('已更新')
    } else {
      await request.post('/admin/content/words', body)
      ElMessage.success('已新增')
    }
    wordVis.value = false
    fetchWords()
    fetchSummary()
  } catch (e) { /* 拦截器已提示 */ }
}

async function toggleWord(row, val) {
  try {
    await request.put(`/admin/content/words/${row.id}/enabled`, { enabled: val })
    row.enabled = val ? 1 : 0
    ElMessage.success(val ? `已启用「${row.word}」` : `已停用「${row.word}」`)
  } catch (e) { /* 失败时保持原状态 */ }
}

async function removeWord(row) {
  try {
    await ElMessageBox.confirm(`确定删除敏感词「${row.word}」吗？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await request.delete(`/admin/content/words/${row.id}`)
    ElMessage.success('已删除')
    fetchWords()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ---------------- 批量导入 ---------------- */
const importVis = ref(false)
const importForm = ref({ text: '', category: 7, level: 2 })

function openImport() {
  importForm.value = { text: '', category: 7, level: 2 }
  importVis.value = true
}

async function doImport() {
  if (!importForm.value.text.trim()) {
    ElMessage.warning('请粘贴词表，一行一个词')
    return
  }
  try {
    const data = await request.post('/admin/content/words/import', importForm.value)
    ElMessage.success(`导入完成，新增 ${data?.added ?? 0} 条`)
    importVis.value = false
    searchWords()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ---------------- 交互日志 ---------------- */
const logs = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logQuery = ref({ scene: null, hitStage: null, reviewStatus: null, keyword: '', page: 1, size: 10 })

async function fetchLogs() {
  logLoading.value = true
  try {
    const data = await request.get('/admin/content/logs', { params: clean(logQuery.value) })
    logs.value = data?.records || []
    logTotal.value = data?.total || 0
  } catch (e) {
    logs.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

const searchLogs = () => {
  logQuery.value.page = 1
  fetchLogs()
}

const logVis = ref(false)
const currentLog = ref(null)
const reviewRemark = ref('')

function openLog(row) {
  currentLog.value = row
  reviewRemark.value = ''
  logVis.value = true
}

async function doReview(result) {
  if (!currentLog.value) return
  try {
    await request.post(`/admin/content/logs/${currentLog.value.id}/review`, {
      result,
      remark: reviewRemark.value
    })
    ElMessage.success(result === 2 ? '已复核通过' : '已驳回')
    logVis.value = false
    fetchLogs()
    fetchSummary()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ---------------- 工具 ---------------- */
function reloadAll() {
  fetchContentMode()
  fetchSummary()
  fetchWords()
  fetchLogs()
}

onMounted(reloadAll)
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.page-head h1 { margin: 4px 0 6px; font-size: 30px; }
.sub { color: var(--ink-2); margin: 0; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin: var(--space-5) 0 var(--space-4);
}

/* 运行模式告警横幅：正常模式下不显示 */
.mode-banner {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: var(--space-5);
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

.stat { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 13px; color: var(--ink-2); }
.stat-value { font-family: var(--font-title); font-size: 28px; color: var(--ink); line-height: 1.2; }
.stat-value.is-danger { color: var(--c-pink); }
.stat-value.is-warn { color: var(--accent-deep); }
.stat-hint { font-size: 12px; color: var(--ink-3); }

.safety-tabs { margin-top: var(--space-3); }
.tab-label { display: inline-flex; align-items: center; gap: 6px; }

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--space-4);
}

.toolbar-gap { flex: 1; }
.w-120 { width: 120px; }
.w-140 { width: 140px; }
.w-150 { width: 150px; }
.w-160 { width: 160px; }
.w-200 { width: 200px; }
.w-220 { width: 220px; }
.w-full { width: 100%; }

.table-card { padding: 0; overflow: hidden; }
.w-name { font-weight: 600; color: var(--ink); }
.stu { display: inline-flex; align-items: center; gap: 5px; }

.lv { font-size: 13px; font-weight: 600; }
.lv-1 { color: var(--ink-3); }
.lv-2 { color: var(--accent-deep); }
.lv-3 { color: var(--c-pink); }

.risk { font-weight: 700; }
.risk.is-low { color: var(--c-green); }
.risk.is-mid { color: var(--accent-deep); }
.risk.is-high { color: var(--c-pink); }

.ops { display: flex; gap: 6px; justify-content: flex-end; }

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
.mini-btn.is-danger { background: #fdeaef; color: #b8325d; }
.mini-btn.is-danger:hover { background: var(--c-pink); color: #fff; }

.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-top: 1px solid var(--line);
}

.total { font-size: 13px; color: var(--ink-3); }
.empty { color: var(--ink-3); font-size: 13px; padding: 18px 0; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

.field-hint { font-size: 12px; color: var(--ink-3); margin: -8px 0 14px; }

.log-detail { display: flex; flex-direction: column; gap: 14px; }
.ld-head { display: flex; align-items: center; gap: 10px; }
.ld-meta { display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; color: var(--ink-3); }
.ld-block h4 { margin: 0 0 6px; font-size: 13px; color: var(--ink-2); font-weight: 600; }
.ld-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink);
  background: var(--surface-soft);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

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
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
