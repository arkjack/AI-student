<template>
  <div class="stu-layout">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="$router.push('/student/home')">
          <span class="logo-emoji">🤖</span>
          <span class="logo-text">AI 启蒙星球</span>
        </div>

        <nav class="menus">
          <RouterLink
            v-for="m in menus"
            :key="m.path"
            :to="m.path"
            class="menu-item"
            :class="{ active: isActive(m.path) }"
          >{{ m.title }}</RouterLink>
        </nav>

        <div class="right-area">
          <span class="user-chip" @click="logout">退出</span>
        </div>
      </div>
    </header>

    <main class="content k-page">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearAuth } from '@/api/request'

const router = useRouter()

/* 学生端用顶部导航（横向菜单多，且面向低龄用户更直观）
   新增页面时在这里加一条，并同步 router/index.js */
const menus = [
  { title: '首页', path: '/student/home' }
  // TODO 课程中心 / 编程实验室 / AI 实验室 / 作业中心 / 答疑互动 / 学习数据
]

function isActive(path) {
  const cur = router.currentRoute.value.path
  return path === '/student/home' ? cur === path : cur.startsWith(path)
}

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.stu-layout { min-height: 100vh; display: flex; flex-direction: column; }
.topbar { position: sticky; top: 0; z-index: 100; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(12px); border-bottom: 1px solid var(--line); }
.topbar-inner { max-width: 1240px; margin: 0 auto; padding: 0 20px; height: 66px; display: flex; align-items: center; gap: 12px; }
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; flex-shrink: 0; }
.logo-emoji { font-size: 27px; }
.logo-text { font-family: var(--font-title); font-size: 19px; color: var(--brand-deep); }
.menus { display: flex; gap: 4px; flex: 1; justify-content: center; flex-wrap: wrap; }
.menu-item { padding: 8px 12px; border-radius: var(--radius-md); font-size: var(--fs-sm); font-weight: 600; white-space: nowrap; color: var(--ink-2); text-decoration: none; transition: color .18s, background .18s; }
.menu-item:hover { color: var(--brand); background: var(--brand-soft); }
.menu-item.active { color: #fff; background: var(--brand); }
.right-area { flex-shrink: 0; }
.user-chip { padding: 6px 14px; border-radius: 999px; background: var(--surface-soft); font-size: var(--fs-sm); font-weight: 600; cursor: pointer; }
.user-chip:hover { color: var(--brand); }
.content { flex: 1; }
</style>
