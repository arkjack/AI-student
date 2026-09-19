<template>
  <div class="side-layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="emoji">🤖</span>
        <span class="text">{{ role === 'admin' ? '管理后台' : '教师工作台' }}</span>
      </div>

      <nav class="nav">
        <RouterLink
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-item"
          :class="{ active: isActive(m.path) }"
        >{{ m.title }}</RouterLink>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="spacer" />
        <!-- 站内消息铃铛：管理端通常不需要，用 showNotice 关掉 -->
        <slot name="notice" />
        <span class="user-chip" @click="logout">退出</span>
      </header>

      <main class="content k-page">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth } from '@/api/request'

/* eslint-disable-next-line no-unused-vars */
const props = defineProps({
  /** 'teacher' | 'admin'，由 router 的 props 传入 */
  role: { type: String, default: 'teacher' },
  /** 是否显示顶栏消息铃铛（管理端建议 false） */
  showNotice: { type: Boolean, default: true }
})

const router = useRouter()

/* ⚠️ 教师端与管理端共用同一套布局，菜单按 role 区分。
   踩坑记录：早期管理端复用了教师端布局却仍去请求 /api/notify/**，
   管理员角色未放行 → 403 → 被前端当成登录失效踢回登录页。
   因此：布局复用没问题，但「布局里发起的请求」必须按角色收敛。 */
const MENUS = {
  teacher: [
    { title: '工作台', path: '/teacher/workbench' }
    // TODO 班级管理 / 任务布置 / 进度追踪 / 作业批改 / 答疑互动 / 消息中心
  ],
  admin: [
    { title: '数据看板', path: '/admin/dashboard' }
    // TODO 用户管理 / 课程管理 / 实验资源 / 公告与日志
  ]
}

const menus = computed(() => MENUS[props.role] || [])

function isActive(path) {
  return router.currentRoute.value.path.startsWith(path)
}

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.side-layout { min-height: 100vh; display: grid; grid-template-columns: 220px 1fr; }
.sidebar { background: var(--navy); color: #fff; padding: 20px 14px; display: flex; flex-direction: column; gap: 18px; }
.brand { display: flex; align-items: center; gap: 8px; padding: 0 8px; }
.brand .emoji { font-size: 24px; }
.brand .text { font-family: var(--font-title); font-size: 17px; }
.nav { display: flex; flex-direction: column; gap: 4px; }
.nav-item { padding: 10px 14px; border-radius: var(--radius-md); font-size: var(--fs-sm); font-weight: 600; color: rgba(255, 255, 255, 0.74); text-decoration: none; transition: background .18s, color .18s; }
.nav-item:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
.nav-item.active { background: var(--brand); color: #fff; }
.main { display: flex; flex-direction: column; min-width: 0; }
.topbar { height: 62px; display: flex; align-items: center; gap: 14px; padding: 0 24px; background: var(--card); border-bottom: 1px solid var(--line); }
.spacer { flex: 1; }
.user-chip { padding: 6px 14px; border-radius: 999px; background: var(--surface-soft); font-size: var(--fs-sm); font-weight: 600; cursor: pointer; }
.user-chip:hover { color: var(--brand); }
.content { flex: 1; }
@media (max-width: 900px) { .side-layout { grid-template-columns: 64px 1fr; } .brand .text, .nav-item { font-size: 0; } }
</style>
