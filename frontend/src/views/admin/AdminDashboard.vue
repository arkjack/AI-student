<template>
  <div class="k-page a-dashboard">
    <div class="page-head">
      <h1 class="k-h1-icon"><ChartLineUp :size="24" weight="bold" /> 数据看板</h1>
      <span class="k-tag">数据更新于 {{ today }} · {{ adminName }}</span>
    </div>

    <!-- 指标 -->
    <section class="kpis">
      <div class="kpi">
        <b class="num" :data-count="summary.totalUsers">0</b>
        <span>累计注册用户</span>
      </div>
      <div class="kpi">
        <b class="num" :data-count="summary.weekActive">0</b>
        <span>本周活跃用户</span>
      </div>
      <div class="kpi">
        <b class="num" :data-count="summary.courseCompleteRate" data-decimals="1">0</b><b class="unit">%</b>
        <span>课程完成率</span>
      </div>
      <div class="kpi">
        <b class="num" :data-count="summary.experimentCount">0</b>
        <span>实验参与人次</span>
      </div>
    </section>

    <!-- 图表 -->
    <section class="a-grid">
      <div class="k-card chart-card">
        <h3><TrendUp :size="16" weight="bold" /> 近 12 周注册趋势</h3>
        <div ref="lineRef" class="chart"></div>
      </div>
      <div class="k-card chart-card">
        <h3><Users :size="16" weight="bold" /> 用户角色分布</h3>
        <div ref="pieRef" class="chart"></div>
      </div>
    </section>

    <!-- 列表 -->
    <section class="a-grid">
      <div class="k-card">
        <h3 class="list-h"><Camera :size="16" weight="bold" /> 最近公告</h3>
        <div class="row-item" v-for="a in announces" :key="a.id">
          <span class="row-tag">{{ a.tag }}</span>
          <span class="row-title">{{ a.title }}</span>
          <span class="row-time">{{ a.time }}</span>
        </div>
        <p v-if="!announces.length" class="empty-list">暂无公告</p>
      </div>
      <div class="k-card">
        <h3 class="list-h"><ListBullets :size="16" weight="bold" /> 近期操作日志</h3>
        <div class="row-item" v-for="l in logs" :key="l.id">
          <span class="row-tag">{{ l.op }}</span>
          <span class="row-title">{{ l.detail }}</span>
          <span class="row-time">{{ l.time }}</span>
        </div>
        <p v-if="!logs.length" class="empty-list">暂无日志</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'
import { PhChartLineUp as ChartLineUp, PhTrendUp as TrendUp, PhUsers as Users, PhCamera as Camera, PhListBullets as ListBullets } from '@phosphor-icons/vue'
import { countUpGroup, prefersReducedMotion } from '@/utils/motion'

const now = new Date()
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

// 当前管理员姓名（来自 localStorage userInfo）
const adminName = ref('管理员')
try {
  const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
  adminName.value = info.nickname || info.realName || info.username || '管理员'
} catch (e) { /* 保底默认值 */ }

// ---- 状态（全部由真实接口驱动，无前端模拟数据） ----
const summary = ref({ totalUsers: 0, weekActive: 0, courseCompleteRate: 0, experimentCount: 0 })
const trendWeeks = ref([])
const trendCounts = ref([])
const roleDist = ref([])
const announces = ref([])
const logs = ref([])

const lineRef = ref(null)
const pieRef = ref(null)
let charts = []
let lineChart = null
let pieChart = null

// ---- 工具 ----
const fmtDate = (v) => (v ? String(v).slice(0, 10) : '')
const fmtDateTime = (v) => (v ? (String(v).length >= 16 ? String(v).slice(0, 16) : String(v)) : '')
const roleNameMap = { student: '学生', teacher: '教师', admin: '管理员' }

function normalizeTrend(list) {
  if (!Array.isArray(list) || !list.length) return null
  return {
    weeks: list.map((d) => {
      const label = d.label || d.week || ''
      // 后端返回 yyyy-MM-dd（周一），横轴显示 MM-dd
      return label.length >= 10 ? label.slice(5, 10) : label
    }),
    counts: list.map((d) => Number(d.count) || 0)
  }
}

function normalizeRoles(list) {
  if (!Array.isArray(list) || !list.length) return null
  return list.map((r, i) => ({
    name: roleNameMap[r.name] || roleNameMap[r.role] || r.name || r.role || `其他${i + 1}`,
    value: Number(r.value) || 0
  }))
}

function normalizeAnnounces(list) {
  if (!Array.isArray(list)) return null
  return list.slice(0, 3).map((a, i) => ({
    id: a.id ?? i,
    tag: a.tag || a.type || '公告',
    title: a.title || '',
    time: fmtDate(a.createdAt)
  }))
}

function normalizeLogs(list) {
  if (!Array.isArray(list)) return null
  return list.slice(0, 3).map((l, i) => ({
    id: l.id ?? i,
    op: l.opType || l.op || '操作',
    detail: l.detail || '',
    time: fmtDateTime(l.createdAt)
  }))
}

async function loadSummary() {
  try {
    const data = await request.get('/admin/dashboard/summary')
    if (data && typeof data === 'object') {
      summary.value = {
        totalUsers: data.totalUsers != null ? Number(data.totalUsers) : summary.value.totalUsers,
        weekActive: data.weekActive != null ? Number(data.weekActive) : summary.value.weekActive,
        courseCompleteRate: data.courseCompleteRate != null ? Number(data.courseCompleteRate) : summary.value.courseCompleteRate,
        experimentCount: data.experimentCount != null ? Number(data.experimentCount) : summary.value.experimentCount
      }
    }
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

async function loadTrend() {
  try {
    const norm = normalizeTrend(await request.get('/admin/dashboard/register-trend'))
    if (norm) { trendWeeks.value = norm.weeks; trendCounts.value = norm.counts }
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

async function loadRoles() {
  try {
    const norm = normalizeRoles(await request.get('/admin/dashboard/role-dist'))
    if (norm) roleDist.value = norm
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

async function loadAnnounces() {
  try {
    const norm = normalizeAnnounces(await request.get('/admin/announcements'))
    if (norm) announces.value = norm
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

async function loadLogs() {
  try {
    const data = await request.get('/admin/logs', { params: { page: 1, size: 3 } })
    const records = data && Array.isArray(data.records) ? data.records : (Array.isArray(data) ? data : [])
    const norm = normalizeLogs(records)
    if (norm) logs.value = norm
  } catch (e) { /* 接口失败保持空数据，不注入模拟值 */ }
}

function renderLine() {
  lineChart.setOption({
    grid: { left: 42, right: 20, top: 26, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trendWeeks.value, axisLine: { lineStyle: { color: '#d9e1f2' } }, axisLabel: { color: '#4c5a78' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2fa' } }, axisLabel: { color: '#8896b4' } },
    series: [{
      type: 'line', smooth: true, data: trendCounts.value,
      symbolSize: 7, lineStyle: { width: 3, color: '#1e4fd8' },
      itemStyle: { color: '#1e4fd8' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(30,79,216,0.22)' }, { offset: 1, color: 'rgba(30,79,216,0.02)' }
      ]) }
    }]
  })
}

function renderPie() {
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#4c5a78' } },
    color: ['#1e4fd8', '#ff8a3d', '#163a9e'],
    series: [{
      type: 'pie', radius: ['44%', '70%'], center: ['50%', '44%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      data: roleDist.value
    }]
  })
}

async function loadAll() {
  await Promise.all([loadSummary(), loadTrend(), loadRoles(), loadAnnounces(), loadLogs()])
  renderLine()
  renderPie()
  if (!prefersReducedMotion()) {
    countUpGroup(document.querySelector('.kpis'), { duration: 1.3, stagger: 0.1 })
  }
}

onMounted(() => {
  lineChart = echarts.init(lineRef.value)
  pieChart = echarts.init(pieRef.value)
  charts = [lineChart, pieChart]
  window.addEventListener('resize', onResize)
  loadAll()
})

const onResize = () => charts.forEach((c) => c.resize())

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.kpi {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kpi b {
  font-size: 30px;
  line-height: 1.15;
  color: var(--ink);
}

.kpi .unit {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink-2);
  margin-left: 2px;
}

.kpi span {
  font-size: 12.5px;
  color: var(--ink-3);
}

.a-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--space-5);
  margin-top: var(--space-5);
}

.chart-card h3,
.list-h {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 15.5px;
}

.chart-card h3 svg,
.list-h svg { color: var(--brand); }

.chart {
  height: 260px;
}

.row-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13.5px;
}

.row-item:last-child { border-bottom: none; }

.row-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-deep);
  background: var(--brand-soft);
  border-radius: 999px;
  padding: 2px 10px;
  flex-shrink: 0;
}

.row-title { flex: 1; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-time { font-size: 12px; color: var(--ink-3); flex-shrink: 0; }

.empty-list {
  margin: 18px 0 8px;
  text-align: center;
  font-size: 13px;
  color: var(--ink-3);
}

@media (max-width: 1100px) {
  .kpis { grid-template-columns: repeat(2, 1fr); }
  .a-grid { grid-template-columns: 1fr; }
}
</style>
