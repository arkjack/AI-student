<template>
  <div class="stu-layout">
    <!-- 顶部导航 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="$router.push('/student/home')">
          <span class="logo-emoji">🤖</span>
          <span class="logo-text">AI 启蒙星球</span>
        </div>

        <nav class="menus">
          <router-link
            v-for="m in menus"
            :key="m.path"
            :to="m.path"
            class="menu-item"
            :class="{ active: isActive(m.path) }"
          >
            <component :is="m.icon" :size="17" weight="bold" />
            <span>{{ m.title }}</span>
          </router-link>
        </nav>

        <div class="right-area">
          <NoticeBell more-path="/student/messages" />
          <el-dropdown>
            <div class="user-chip">
              <span class="avatar">🦊</span>
              <span class="name">{{ user.nickname }}</span>
              <span class="lv">Lv.{{ user.level }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/student/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/student/stats')">学习数据</el-dropdown-item>
                <el-dropdown-item divided @click="$router.push('/login')">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade-page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      AI 启蒙星球 · 中小学生 AI 启蒙平台 · 毕业设计原型版
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PhHouse as House, PhBookOpen as BookOpen, PhPuzzlePiece as PuzzlePiece, PhSparkle as Sparkle, PhNotePencil as NotebookPen, PhChartLineUp as ChartLineUp, PhChatCircleDots as ChatCircleDots } from '@phosphor-icons/vue'
import NoticeBell from '@/components/NoticeBell.vue'

// 从 localStorage.userInfo 读取当前登录用户，回退默认值
function readUserInfo() {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return {
      nickname: info.nickname || info.realName || info.username || '同学',
      avatar: info.avatar || '🦊',
      level: 1
    }
  } catch (e) {
    return { nickname: '同学', avatar: '🦊', level: 1 }
  }
}
const user = ref(readUserInfo())

const menus = [
  { title: '首页', path: '/student/home', icon: House },
  { title: '课程中心', path: '/student/courses', icon: BookOpen },
  { title: '编程实验室', path: '/student/lab', icon: PuzzlePiece },
  { title: 'AI 魔法实验室', path: '/student/ai', icon: Sparkle },
  { title: '作业中心', path: '/student/homework', icon: NotebookPen },
  { title: '答疑互动', path: '/student/qa', icon: ChatCircleDots },
  { title: '学习数据', path: '/student/stats', icon: ChartLineUp }
]

const isActive = (path) => {
  const cur = window.location.pathname
  return path === '/student/home' ? cur === path : cur.startsWith(path)
}
</script>

<style scoped>
.stu-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* topbar */
.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}

.topbar-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 20px;
  height: 66px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-emoji {
  font-size: 27px;
}

.logo-text {
  font-family: var(--font-title);
  font-size: 19px;
  color: var(--brand-deep);
  letter-spacing: 0.02em;
}

/* menus */
.menus {
  display: flex;
  gap: 3px;
  flex: 1;
  justify-content: center;
}

.menu-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 9px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
  color: var(--ink-2);
  transition: color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.menu-item span {
  white-space: nowrap;
}

.menu-item svg {
  color: var(--ink-3);
  transition: color 0.18s ease;
}

.menu-item:hover {
  color: var(--brand);
  background: var(--brand-grad-soft);
}

.menu-item:hover svg {
  color: var(--brand);
}

.menu-item.active {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: 0 4px 12px rgba(30, 79, 216, 0.32);
}

.menu-item.active svg {
  color: #fff;
}

/* right */
.right-area {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.bell {
  position: relative;
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: background 0.2s;
}

.bell:hover {
  background: var(--brand-grad-soft);
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
  background: var(--c-pink);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 999px;
  padding: 1px 5px;
  line-height: 1.3;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--brand-grad-soft);
  cursor: pointer;
  transition: all 0.2s;
}

.user-chip:hover {
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.avatar {
  font-size: 22px;
}

.name {
  font-weight: 600;
  font-size: 14px;
  color: var(--ink);
}

.lv {
  background: var(--brand-grad);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 700;
}

/* content */
.content {
  flex: 1;
}

.footer {
  text-align: center;
  color: var(--ink-3);
  font-size: 12px;
  padding: 18px 0 24px;
}

@media (max-width: 1080px) {
  .menu-item span:last-child { display: none; }
}
</style>
