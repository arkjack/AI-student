<template>
  <div class="nb" ref="root">
    <div class="nb-bell" title="消息" @click="toggle">
      <Bell :size="20" weight="bold" />
      <span v-if="unread > 0" class="nb-badge">{{ unread > 99 ? '99+' : unread }}</span>
    </div>

    <transition name="nb-pop">
      <div v-if="open" class="nb-pop">
        <div class="nb-head">
          <span class="nb-title"><Bell :size="15" weight="bold" /> 消息通知</span>
          <button class="nb-readall" @click="markAll">全部已读</button>
        </div>
        <div class="nb-list">
          <div v-if="!list.length" class="nb-empty">暂无消息</div>
          <div
            v-for="n in list"
            :key="n.id"
            class="nb-item"
            :class="{ unread: !n.read }"
            @click="openItem(n)"
          >
            <span class="nb-ico" :class="n.type">{{ typeIcon(n.type) }}</span>
            <div class="nb-main">
              <div class="nb-row">
                <b>{{ n.title }}</b>
                <span class="nb-type" :class="n.type">{{ typeLabel(n.type) }}</span>
              </div>
              <p class="nb-content">{{ n.content }}</p>
              <span class="nb-time">{{ n.time }}</span>
            </div>
          </div>
        </div>
        <div class="nb-foot" @click="goMore"><Bell :size="13" weight="bold" /> 查看全部消息</div>      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { PhBell as Bell } from '@phosphor-icons/vue'

const router = useRouter()
const props = defineProps({
  morePath: { type: String, default: '' }
})
const open = ref(false)
const unread = ref(0)
const list = ref([])
const root = ref(null)

const TYPE_ICON = { task: '📘', grade: '✅', announce: '📣', apply: '👥' }
const TYPE_LABEL = { task: '新任务', grade: '批改', announce: '公告', apply: '入班' }
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
  return `${mm}-${dd} ${hh}:${mi}`
}

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
  } catch (e) { /* 拦截器已提示 */ }
}

const toggle = () => {
  open.value = !open.value
  if (open.value) load()
}

async function openItem(n) {
  if (!n.read) {
    try { await request.post(`/notify/${n.id}/read`) } catch (e) {}
    n.read = true
    unread.value = Math.max(0, unread.value - 1)
  }
  open.value = false
  if (n.link) router.push(n.link)
}

async function markAll() {
  try {
    await request.post('/notify/read-all')
    unread.value = 0
    list.value.forEach(n => { n.read = true })
  } catch (e) {}
}

const goMore = () => {
  open.value = false
  if (props.morePath) router.push(props.morePath)
}

const onDocClick = (e) => {
  if (open.value && root.value && !root.value.contains(e.target)) open.value = false
}

onMounted(() => {
  load()
  document.addEventListener('click', onDocClick)
  window._nbTimer = setInterval(load, 30000)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  clearInterval(window._nbTimer)
})
</script>

<style scoped>
.nb { position: relative; }

.nb-bell {
  position: relative;
  display: inline-flex;
  padding: 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--ink-2);
  transition: background 0.18s, color 0.18s;
}
.nb-bell:hover { background: var(--brand-grad-soft); color: var(--brand); }

.nb-badge {
  position: absolute;
  top: 1px;
  right: 1px;
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 999px;
  padding: 1px 5px;
  line-height: 1.3;
}

.nb-pop {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 340px;
  max-height: 460px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 14px 40px rgba(21, 40, 92, 0.16);
  z-index: 300;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.nb-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}
.nb-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
}
.nb-title svg { color: var(--brand); }
.nb-readall {
  border: none;
  background: var(--brand-soft);
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  padding: 5px 12px;
  cursor: pointer;
  transition: all 0.18s;
}
.nb-readall:hover { background: var(--brand); color: #fff; }

.nb-list { overflow-y: auto; flex: 1; }

.nb-empty { padding: 40px 0; text-align: center; color: var(--ink-3); font-size: 13px; }

.nb-item {
  display: flex;
  gap: 11px;
  padding: 13px 16px;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: background 0.18s;
}
.nb-item:hover { background: var(--surface-soft); }
.nb-item.unread { background: #f4f7ff; }

.nb-ico {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
  background: var(--brand-grad-soft);
}
.nb-ico.grade { background: #e5f9ee; }
.nb-ico.announce { background: var(--accent-soft); }
.nb-ico.apply { background: #e0f3f0; }

.nb-main { flex: 1; min-width: 0; }
.nb-row { display: flex; align-items: center; gap: 8px; }
.nb-row b { font-size: 13.5px; color: var(--ink); }
.nb-type { font-size: 11px; color: var(--ink-3); margin-left: auto; }
.nb-type.task { color: var(--brand); }
.nb-type.grade { color: #17934e; }
.nb-type.announce { color: var(--accent-deep); }
.nb-type.apply { color: #0f6b61; }

.nb-content {
  margin: 4px 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nb-item.unread .nb-content { color: var(--ink); }

.nb-time { font-size: 11.5px; color: var(--ink-3); }

.nb-foot {
  padding: 10px 16px;
  text-align: center;
  font-size: 12.5px;
  color: var(--ink-3);
  border-top: 1px solid var(--line);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.nb-foot:hover { color: var(--brand); }

/* 过渡 */
.nb-pop-enter-active, .nb-pop-leave-active { transition: opacity 0.16s, transform 0.16s; }
.nb-pop-enter-from, .nb-pop-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
