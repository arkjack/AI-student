<template>
  <div class="k-page">
    <div class="page-head">
      <h1 class="k-h1-icon"><ChartLineUp :size="16" weight="bold" /> 学习数据</h1>
      <span class="k-tag">成长记录自动更新</span>
    </div>

    <!-- 概览（无卡数据条） -->
    <section class="stat-strip">
      <div class="stat-tile">
        <span class="tile-icon blue"><Timer :size="20" weight="bold" /></span>
        <div class="tile-num"><b><span class="num" :data-count="studyCount" data-decimals="0">0</span><span class="unit">分</span></b><span>课程学习</span></div>
      </div>
      <div class="stat-tile">
        <span class="tile-icon blue"><BookOpen :size="20" weight="bold" /></span>
        <div class="tile-num"><b><span class="num" :data-count="overview.completedCourses" data-decimals="0">0</span><span class="unit">门</span></b><span>已完成课程</span></div>
      </div>
      <div class="stat-tile">
        <span class="tile-icon green"><FolderOpen :size="20" weight="bold" /></span>
        <div class="tile-num"><b><span class="num" :data-count="overview.worksCount" data-decimals="0">0</span><span class="unit">个</span></b><span>作品数量</span></div>
      </div>
      <div class="stat-tile">
        <span class="tile-icon orange"><Medal :size="20" weight="bold" /></span>
        <div class="tile-num"><b><span class="num" :data-count="overview.avgScore" data-decimals="0">0</span><span class="unit">分</span></b><span>作业平均分</span></div>
      </div>
    </section>

    <!-- 图表区 -->
    <section class="chart-grid">
      <div class="k-card chart-card">
        <h3><ChartLineUp :size="16" weight="bold" /> 最近 7 天学习动态（次）</h3>
        <div ref="lineRef" class="chart"></div>
      </div>
      <div class="k-card chart-card">
        <h3><ChartPieSlice :size="16" weight="bold" /> 学习成果分布</h3>
        <div ref="pieRef" class="chart"></div>
      </div>
    </section>

    <section class="chart-grid single">
      <div class="k-card chart-card">
        <h3><ChartBar :size="16" weight="bold" /> 作业得分趋势</h3>
        <div ref="barRef" class="chart chart-tall"></div>
      </div>
      <div class="k-card grow-card">
        <h3><TrendUp :size="16" weight="bold" /> 成长等级</h3>
        <div class="lv-badge">Lv.{{ level.level }}</div>
        <div class="grow-bar"><div class="grow-fill" :style="{ width: pct + '%' }"></div></div>
        <p class="grow-tip">距离 Lv.{{ level.level + 1 }} 还差 {{ level.expNext - level.exp }} 经验，继续加油！</p>
        <div class="grow-btns">
          <div class="grow-btn" @click="$router.push('/student/courses')"><BookOpen :size="16" weight="bold" /> 看课程</div>
          <div class="grow-btn" @click="$router.push('/student/ai?tab=quiz')"><Trophy :size="16" weight="bold" /> 去闯关</div>
          <div class="grow-btn" @click="$router.push('/student/lab')"><PuzzlePiece :size="16" weight="bold" /> 编程</div>
        </div>
      </div>
    </section>

    <!-- 月度成就 -->
    <div class="k-section-head">
      <h2>本月成就</h2>
    </div>
    <section class="k-card month-card">
      <div v-for="m in monthGoals" :key="m.name" class="month-item">
        <span class="m-emoji"><component :is="monthIcon(m)" :size="22" weight="bold" /></span>
        <div class="m-info">
          <b>{{ m.name }}</b>
          <div class="m-bar"><div class="m-fill" :style="{ width: pctOf(m) + '%' }"></div></div>
          <span>{{ m.cur }} / {{ m.goal }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { gsap, countUpGroup } from '@/utils/motion'
import request from '@/api/request'
import { PhChartLineUp as ChartLineUp, PhTimer as Timer, PhBookOpen as BookOpen, PhFolderOpen as FolderOpen, PhMedal as Medal, PhChartPieSlice as ChartPieSlice, PhChartBar as ChartBar, PhTrendUp as TrendUp, PhTrophy as Trophy, PhPuzzlePiece as PuzzlePiece } from '@phosphor-icons/vue'

const overview = ref({ studyMinutes: 0, completedCourses: 0, worksCount: 0, avgScore: 0, quizCount: 0 })
const level = ref({ level: 1, exp: 0, expNext: 200 })
const monthGoals = ref([])

const studyCount = computed(() => Math.round(overview.value.studyMinutes))
const pct = computed(() => {
  if (!level.value.expNext) return 0
  return Math.min(100, Math.round(level.value.exp / level.value.expNext * 100))
})
const pctOf = (m) => (m.goal ? Math.min(100, Math.round(m.cur / m.goal * 100)) : 0)

const MONTH_ICONS = { '作品': PuzzlePiece, '闯关': Trophy, '课程': BookOpen, '作业': Medal }
const monthIcon = (m) => MONTH_ICONS[m.name.includes('作品') ? '作品' : m.name.includes('闯关') ? '闯关' : m.name.includes('课程') ? '课程' : '作业']

const lineRef = ref(null)
const pieRef = ref(null)
const barRef = ref(null)
let charts = []

onMounted(async () => {
  const data = await loadStats()

  // 折线图：最近 7 天学习动态
  const line = echarts.init(lineRef.value)
  line.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.weekLabels, axisLine: { lineStyle: { color: '#dbe6f8' } }, axisLabel: { color: '#5b6b8c' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef2fb' } }, axisLabel: { color: '#93a2c0' } },
    series: [{
      type: 'line',
      smooth: true,
      data: data.weekCounts,
      symbolSize: 8,
      itemStyle: { color: '#3b82f6' },
      lineStyle: { width: 3.5, color: '#3b82f6' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59,130,246,0.28)' },
          { offset: 1, color: 'rgba(59,130,246,0.02)' }
        ])
      }
    }]
  })

  // 饼图：学习成果分布
  const pie = echarts.init(pieRef.value)
  pie.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#5b6b8c' } },
    color: ['#1e4fd8', '#2f5cd8', '#ff8a3d'],
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      data: data.distValues.map((v, i) => ({ name: data.distLabels[i], value: v }))
    }]
  })

  // 柱状图：作业得分趋势
  const bar = echarts.init(barRef.value)
  bar.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.scoreLabels, axisLine: { lineStyle: { color: '#dbe6f8' } }, axisLabel: { color: '#5b6b8c' } },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#eef2fb' } }, axisLabel: { color: '#93a2c0' } },
    series: [{
      type: 'bar',
      data: data.scoreValues,
      barWidth: 26,
      itemStyle: {
        borderRadius: [10, 10, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#5b8def' },
          { offset: 1, color: '#3b82f6' }
        ])
      }
    }]
  })

  charts = [line, pie, bar]

  // 概览数字滚动 + 图标入场
  await nextTick()
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    countUpGroup(document.querySelector('.stat-strip'), { duration: 1.2, stagger: 0.1 })
    gsap.from('.stat-tile .tile-icon', {
      scale: 0.7,
      autoAlpha: 0,
      duration: 0.5,
      stagger: 0.08,
      ease: 'back.out(1.8)',
      delay: 0.2
    })
  }

  window.addEventListener('resize', onResize)
})

async function loadStats() {
  let data = {
    weekLabels: [], weekCounts: [],
    distLabels: [], distValues: [],
    scoreLabels: [], scoreValues: [],
    monthGoals: []
  }
  try {
    data = await request.get('/stats/student/my') || data
  } catch (e) {
    // 拦截器已提示，回退空数据
  }
  overview.value = {
    studyMinutes: data.studyMinutes || 0,
    completedCourses: data.completedCourses || 0,
    worksCount: data.worksCount || 0,
    avgScore: data.avgScore || 0,
    quizCount: data.quizCount || 0
  }
  level.value = { level: data.level || 1, exp: data.exp || 0, expNext: data.expNext || 200 }
  monthGoals.value = data.monthGoals || []
  return data
}

const onResize = () => charts.forEach(c => c.resize())

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-head h1 {
  margin: 4px 0;
  font-size: 30px;
}

/* 概览数据条（无卡） */
.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  padding: var(--space-3) 0;
}

.stat-tile {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4);
}

.stat-tile + .stat-tile {
  border-left: 1.5px solid var(--line);
}

.tile-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.tile-icon.blue { background: var(--brand-soft); color: var(--brand); }
.tile-icon.green { background: #e5f9ee; color: #17934e; }
.tile-icon.orange { background: #fff1e0; color: var(--accent-deep); }

.tile-num b {
  display: block;
  font-size: 28px;
  line-height: 1.15;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.tile-num .unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-2);
  margin-left: 3px;
}

.tile-num span:last-child {
  font-size: 12.5px;
  color: var(--ink-3);
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.chart-grid.single {
  grid-template-columns: 1.5fr 1fr;
}

.chart-card h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 16px;
}

.chart-card h3 svg {
  color: var(--brand);
}

.chart {
  height: 280px;
}

.chart-tall {
  height: 320px;
}

.grow-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
}

.grow-card h3 {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
}

.grow-card h3 svg {
  color: var(--brand);
}

.lv-badge {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  box-shadow: 0 10px 26px rgba(59, 130, 246, 0.35);
  margin-bottom: 14px;
  font-family: var(--font-title);
}

.grow-bar {
  width: 100%;
  height: 12px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}

.grow-fill {
  height: 100%;
  background: var(--brand-grad);
  border-radius: 999px;
}

.grow-tip {
  font-size: 12.5px;
  color: var(--ink-3);
  margin: 8px 0 14px;
}

.grow-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.grow-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--brand-grad-soft);
  color: var(--brand-deep);
  font-size: 12.5px;
  font-weight: 700;
  border-radius: 999px;
  padding: 7px 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.grow-btn:hover {
  background: var(--brand-grad);
  color: #fff;
}

/* 月度 */
.month-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.month-item {
  display: flex;
  gap: 14px;
  align-items: center;
  background: #f8faff;
  border-radius: 14px;
  padding: 14px 18px;
}

.m-emoji {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--brand-grad-soft);
  color: var(--brand);
  flex-shrink: 0;
}

.m-info { flex: 1; }

.m-info b {
  font-size: 14px;
}

.m-bar {
  height: 8px;
  background: #e7eefb;
  border-radius: 999px;
  overflow: hidden;
  margin: 7px 0 4px;
}

.m-fill {
  height: 100%;
  background: linear-gradient(90deg, #34d399, #22c3aa);
  border-radius: 999px;
}

.m-info span {
  font-size: 12px;
  color: var(--ink-3);
}

@media (max-width: 1000px) {
  .stat-strip { grid-template-columns: repeat(2, 1fr); }
  .stat-tile:nth-child(3) { border-left: none; }
  .chart-grid,
  .chart-grid.single,
  .month-card { grid-template-columns: 1fr; }
}
</style>
