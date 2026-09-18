<template>
  <div class="k-page msg-page">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><Bell :size="16" weight="bold" /> 消息中心</h1>
        <p class="sub">你的任务、批改、公告与入班动态都在这里。</p>
      </div>
      <div class="head-right">
        <span class="k-tag">{{ unread }} 条未读</span>
        <button class="k-btn k-btn--ghost" @click="markAll">
          <CheckCircle :size="16" weight="bold" /> 全部已读
        </button>
      </div>
    </div>

    <!-- 类型筛选 -->
    <div class="filters">
      <span
        v-for="f in filters"
        :key="f.value"
        class="chip"
        :class="{ active: activeType === f.value }"
        @click="activeType = f.value"
      >{{ f.label }}</span>
    </div>

    <!-- 消息列表 -->
    <div v-if="filtered.length" class="msg-list">
      <div
        v-for="n in filtered"
        :key="n.id"
        class="k-card msg-item"
        :class="{ unread: !n.read }"
        @click="openItem(n)"
      >
        <span class="msg-ico" :class="n.type">{{ typeIcon(n.type) }}</span>
        <div class="msg-body">
          <div class="msg-top">
            <b>{{ n.title }}</b>
            <span class="msg-type" :class="n.type">{{ typeLabel(n.type) }}</span>
            <span v-if="!n.read" class="msg-dot"></span>
          </div>
          <p class="msg-content">{{ n.content }}</p>
          <div class="msg-meta">
            <span><Clock :size="13" weight="bold" /> {{ n.time }}</span>
            <button v-if="!n.read" class="msg-read" @click.stop="markOne(n)">标记已读</button>
            <span v-else class="msg-read-done">已读</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无消息" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { PhBell as Bell, PhCheckCircle as CheckCircle, PhClock as Clock } from '@phosphor-icons/vue'

const router = useRouter()
const unread = ref(0)
const list = ref([])
const activeType = ref('all')

const filters = [
  { label: '全部', value: 'all' },
  { label: '新任务', value: 'task' },
  { label: '作业批改', value: 'grade' },
  { label: '平台公告', value: 'announce' },
  { label: '入班申请', value: 'apply' }
]

const TYPE_ICON = { task: '📘', grade: '✅', announce: '📣', apply: '👥' }
const TYPE_LABEL = { task: '新任务', grade: '作业批改', announce: '平台公告', apply: '入班申请' }
const typeIcon = (t) => TYPE_ICON[t] || '🔔'
const typeLabel = (t) => TYPE_LABEL[t] || '消息'

const fmtTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  if (isNaN(d.getTime())) return String(v)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${d.getFullYear()}年${mm}月${dd}日 ${hh}时${mi}分`
}

const filtered = computed(() => {
  if (activeType.value === 'all') return list.value
  return list.value.filter(n => n.type === activeType.value)
})

async function load() {
  try {
    const [cnt, items] = await Promise.all([
      request.get('/notify/unread-count'),
      request.get('/notify/my')
    ])
    unread.value = Number(cnt) || 0
    list.value = (Array.isArray(items) ? items : []).map(n => ({
      id: n.id,
      type: n.type,
      title: n.title || '消息',
      content: n.content || '',
      link: n.link || '',
      read: !!n.read,
      time: fmtTime(n.createdAt)
    }))
  } catch (e) {}
}

async function markOne(n) {
  try { await request.post(`/notify/${n.id}/read`) } catch (e) {}
  n.read = true
  unread.value = Math.max(0, unread.value - 1)
}

async function markAll() {
  try {
    await request.post('/notify/read-all')
    list.value.forEach(n => { n.read = true })
    unread.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) {}
}

function openItem(n) {
  if (!n.read) { markOne(n) }
  if (n.link) router.push(n.link)
}

onMounted(load)
</script>

<style scoped>
.msg-page { max-width: 860px; margin: 0 auto; }

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

.filters { display: flex; gap: 8px; margin: 20px 0; flex-wrap: wrap; }
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

.msg-list { display: flex; flex-direction: column; gap: 14px; }
.msg-item {
  display: flex;
  gap: 15px;
  align-items: flex-start;
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s;
}
.msg-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-hover); }
.msg-item.unread { border-left: 3px solid var(--brand); }

.msg-ico {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  flex-shrink: 0;
  background: var(--brand-grad-soft);
}
.msg-ico.grade { background: #e5f9ee; }
.msg-ico.announce { background: var(--accent-soft); }
.msg-ico.apply { background: #e0f3f0; }

.msg-body { flex: 1; min-width: 0; }
.msg-top { display: flex; align-items: center; gap: 10px; }
.msg-top b { font-size: 15.5px; }
.msg-type { font-size: 11.5px; color: var(--ink-3); margin-left: auto; }
.msg-type.task { color: var(--brand); }
.msg-type.grade { color: #17934e; }
.msg-type.announce { color: var(--accent-deep); }
.msg-type.apply { color: #0f6b61; }
.msg-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent); }

.msg-content {
  margin: 8px 0 10px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-2);
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--ink-3);
}
.msg-meta span { display: inline-flex; align-items: center; gap: 5px; }

.msg-read {
  border: none;
  background: var(--brand-soft);
  color: var(--brand-deep);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}
.msg-read:hover { background: var(--brand); color: #fff; }
.msg-read-done { color: var(--ink-3); }

@media (max-width: 680px) {
  .msg-item { flex-direction: column; }
}
</style>
