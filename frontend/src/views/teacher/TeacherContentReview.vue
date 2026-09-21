<template>
  <div class="k-page t-content-review">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><ShieldCheck :size="24" weight="bold" /> 内容复核</h1>
        <p class="sub">查看本班学生 AI 交互中被安全机制处置或标记的内容，做人工复核兜底</p>
      </div>
      <div class="head-right">
        <span class="pending-chip" v-if="pendingCount > 0">待复核 {{ pendingCount }} 条</span>
        <button class="k-btn k-btn--ghost" @click="refresh">
          <ArrowClockwise :size="16" weight="bold" /> 刷新
        </button>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="query.reviewStatus" placeholder="复核状态" class="w-150" @change="search">
        <el-option v-for="s in REVIEW_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.scene" placeholder="全部场景" clearable class="w-160" @change="search">
        <el-option v-for="s in SCENE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索输入 / 命中词"
        clearable
        class="w-220"
        @keyup.enter="search"
        @clear="search"
      />
      <button class="k-btn k-btn--ghost" @click="search">
        <MagnifyingGlass :size="16" weight="bold" /> 查询
      </button>
    </div>

    <div class="k-card table-card">
      <el-table :data="list" style="width: 100%" v-loading="loading">
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
        <el-table-column prop="inputText" label="学生输入" min-width="200" show-overflow-tooltip />
        <el-table-column label="复核" width="110" align="center">
          <template #default="{ row }">
            <span class="k-tag" :class="reviewClass(row.reviewStatus)">{{ row.reviewStatusText }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="{ row }">
            <button class="mini-btn" @click="open(row)">{{ row.reviewStatus === 1 ? '去复核' : '查看' }}</button>
          </template>
        </el-table-column>
        <template #empty>
          <p class="empty">
            暂无需要复核的内容。学生使用 AI 实验后，被拦截、被替换或被标记的记录会自动出现在这里。
          </p>
        </template>
      </el-table>

      <div class="table-foot">
        <span class="total">共 {{ total }} 条</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="query.size"
          v-model:current-page="query.page"
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 复核详情 -->
    <el-dialog v-model="vis" title="内容复核" width="640px">
      <div v-if="current" class="detail">
        <div class="d-head">
          <span class="stu"><span v-if="current.avatar">{{ current.avatar }}</span>{{ current.studentName || ('#' + current.userId) }}</span>
          <span class="k-tag" :class="stageClass(current.hitStage)">{{ current.hitStageText }}</span>
          <span class="k-tag" :class="reviewClass(current.reviewStatus)">{{ current.reviewStatusText }}</span>
        </div>
        <div class="d-meta">
          <span>{{ current.sceneName }}</span>
          <span>{{ current.createdAt }}</span>
          <span v-if="current.elapsedMs != null">耗时 {{ current.elapsedMs }}ms</span>
          <span>风险分 {{ current.riskScore }}</span>
        </div>

        <div class="d-block">
          <h4>学生输入</h4>
          <p class="d-text">{{ current.inputText || '（本场景无学生输入）' }}</p>
        </div>
        <div class="d-block">
          <h4>AI 原始返回</h4>
          <p class="d-text">{{ current.outputText || '（未调用大模型：输入阶段已被拦截，这是最需要关注的一类）' }}</p>
        </div>
        <div class="d-block">
          <h4>系统判定</h4>
          <p class="d-text">
            处置动作：{{ current.actionText }}<br />
            <template v-if="current.hitWords">命中词：{{ current.hitWords }}<br /></template>
            <template v-if="current.reason">原因：{{ current.reason }}</template>
          </p>
        </div>

        <template v-if="current.reviewStatus === 1">
          <div class="d-block">
            <h4>复核意见</h4>
            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              placeholder="例如：属于正常科学讨论，已确认无误；或：内容确有不妥，已提醒学生"
            />
          </div>
        </template>
        <template v-else>
          <div class="d-block">
            <h4>复核结果</h4>
            <p class="d-text">
              {{ current.reviewStatusText }}
              <template v-if="current.reviewerName"> · {{ current.reviewerName }}</template>
              <template v-if="current.reviewedAt"><br />时间：{{ current.reviewedAt }}</template>
              <template v-if="current.reviewRemark"><br />意见：{{ current.reviewRemark }}</template>
            </p>
          </div>
        </template>
      </div>
      <template #footer>
        <button class="k-btn k-btn--ghost" @click="vis = false">关闭</button>
        <template v-if="current && current.reviewStatus === 1">
          <button class="k-btn k-btn--ghost" @click="submit(3)">驳回（内容下架）</button>
          <button class="k-btn" @click="submit(2)">复核通过</button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  SCENE_OPTIONS,
  REVIEW_OPTIONS,
  stageClass,
  reviewClass,
  riskClass,
  cleanParams
} from '@/utils/contentSafety'
import {
  PhShieldCheck as ShieldCheck,
  PhMagnifyingGlass as MagnifyingGlass,
  PhArrowClockwise as ArrowClockwise
} from '@phosphor-icons/vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const pendingCount = ref(0)
const query = ref({ reviewStatus: 1, scene: null, keyword: '', page: 1, size: 10 })

async function fetchList() {
  loading.value = true
  try {
    const data = await request.get('/teacher/content-review/list', {
      params: cleanParams(query.value)
    })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchPending() {
  try {
    const n = await request.get('/teacher/content-review/pending-count')
    pendingCount.value = typeof n === 'number' ? n : 0
  } catch (e) {
    pendingCount.value = 0
  }
}

const search = () => {
  query.value.page = 1
  fetchList()
}

const vis = ref(false)
const current = ref(null)
const remark = ref('')

function open(row) {
  current.value = row
  remark.value = ''
  vis.value = true
}

async function submit(result) {
  if (!current.value) return
  try {
    await request.post(`/teacher/content-review/${current.value.id}`, {
      result,
      remark: remark.value
    })
    ElMessage.success(result === 2 ? '已复核通过' : '已驳回')
    vis.value = false
    fetchList()
    fetchPending()
  } catch (e) { /* 拦截器已提示 */ }
}

function refresh() {
  fetchPending()
  fetchList()
}

onMounted(refresh)
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

.head-right { display: flex; align-items: center; gap: 12px; }

.pending-chip {
  background: var(--accent-soft);
  color: var(--accent-deep);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin: var(--space-5) 0 var(--space-4);
}

.w-150 { width: 150px; }
.w-160 { width: 160px; }
.w-220 { width: 220px; }

.table-card { padding: 0; overflow: hidden; }
.stu { display: inline-flex; align-items: center; gap: 5px; }

.risk { font-weight: 700; }
.risk.is-low { color: var(--c-green); }
.risk.is-mid { color: var(--accent-deep); }
.risk.is-high { color: var(--c-pink); }

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

.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-top: 1px solid var(--line);
}

.total { font-size: 13px; color: var(--ink-3); }
.empty { color: var(--ink-3); font-size: 13px; padding: 24px 0; max-width: 520px; margin: 0 auto; line-height: 1.7; }

.detail { display: flex; flex-direction: column; gap: 14px; }
.d-head { display: flex; align-items: center; gap: 10px; }
.d-meta { display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; color: var(--ink-3); }
.d-block h4 { margin: 0 0 6px; font-size: 13px; color: var(--ink-2); font-weight: 600; }
.d-text {
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
</style>
