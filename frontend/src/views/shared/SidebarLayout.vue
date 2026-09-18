<template>
  <div class="sb-layout">
    <!-- 左侧深蓝侧栏 -->
    <aside class="sidebar">
      <div class="sb-brand" @click="$router.push(brandPath)">
        <img src="@/assets/images/logo-badge.jpg" alt="" class="sb-logo" />
        <span class="sb-brand-name">AI 启蒙星球</span>
      </div>

      <div class="sb-user">
        <span class="sb-avatar">{{ user.avatar }}</span>
        <div class="sb-user-info">
          <b>{{ user.nickname }}</b>
          <span>{{ user.roleLabel }}</span>
        </div>
      </div>

      <nav class="sb-menu">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="sb-item"
          :class="{ active: isActive(m.path) }"
        >
          <component :is="m.icon" :size="18" weight="bold" />
          <span>{{ m.title }}</span>
        </router-link>
      </nav>

      <div class="sb-foot" @click="$router.push('/login')">
        <ArrowSquareOut :size="15" weight="bold" />
        <span>退出登录</span>
      </div>
    </aside>

    <!-- 右侧：顶栏 + 内容 -->
    <div class="sb-main">
      <header class="sb-topbar">
        <div class="sb-crumb">
          <component :is="currentMeta.icon" :size="17" weight="bold" />
          <span>{{ currentMeta.title }}</span>
        </div>
        <div class="sb-top-right">
          <el-input v-model="search" placeholder="搜索…" class="sb-search" clearable>
            <template #prefix><MagnifyingGlass :size="15" weight="bold" /></template>
          </el-input>
          <NoticeBell v-if="showNotice" more-path="/teacher/messages" />
          <el-dropdown>
            <div class="sb-chip">
              <span>{{ user.avatar }}</span>
              <span class="sb-chip-name">{{ user.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/login')">切换身份</el-dropdown-item>
                <el-dropdown-item divided @click="$router.push('/login')">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="sb-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { PhArrowSquareOut as ArrowSquareOut, PhMagnifyingGlass as MagnifyingGlass } from '@phosphor-icons/vue'
import NoticeBell from '@/components/NoticeBell.vue'

const props = defineProps({
  user: { type: Object, required: true },
  menus: { type: Array, required: true },
  brandPath: { type: String, default: '/teacher/workbench' },
  // 是否显示消息铃铛（管理端无站内消息，传 false 隐藏）
  showNotice: { type: Boolean, default: true }
})

const route = useRoute()
const search = ref('')

const currentMeta = computed(() => {
  const m = props.menus.find((x) => route.path.startsWith(x.path))
  return m ? { title: m.title, icon: m.icon } : { title: '工作台', icon: props.menus[0]?.icon }
})

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
</script>

<style scoped>
.sb-layout {
  display: flex;
  min-height: 100vh;
}

/* ---------- 侧栏 ---------- */
.sidebar {
  width: 248px;
  flex-shrink: 0;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.08) 0%, transparent 40%),
    var(--navy);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  position: sticky;
  top: 0;
  height: 100vh;
}

.sb-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px 18px;
  cursor: pointer;
}

.sb-logo {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.5);
  object-fit: cover;
}

.sb-brand-name {
  font-family: var(--font-title);
  font-size: 17px;
  letter-spacing: 0.02em;
}

.sb-user {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  margin-bottom: 20px;
}

.sb-avatar {
  font-size: 26px;
}

.sb-user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.sb-user-info b {
  font-size: 14px;
}

.sb-user-info span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.sb-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.sb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.78);
  font-size: 14.5px;
  font-weight: 600;
  transition: background 0.18s ease, color 0.18s ease;
}

.sb-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.sb-item.active {
  background: #fff;
  color: var(--brand);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.22);
}

.sb-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.6);
  font-size: 13.5px;
  cursor: pointer;
  transition: color 0.18s;
}

.sb-foot:hover {
  color: #fff;
}

/* ---------- 主区 ---------- */
.sb-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sb-topbar {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.sb-crumb {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink);
  font-family: var(--font-title);
}

.sb-crumb svg {
  color: var(--brand);
}

.sb-top-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sb-search {
  width: 220px;
}

.sb-bell {
  position: relative;
  display: inline-flex;
  padding: 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--ink-2);
  transition: background 0.18s;
}

.sb-bell:hover {
  background: var(--brand-grad-soft);
  color: var(--brand);
}

.sb-badge {
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

.sb-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--brand-grad-soft);
  border-radius: 999px;
  padding: 7px 14px;
  cursor: pointer;
  font-size: 14px;
}

.sb-chip-name {
  font-weight: 600;
  color: var(--ink);
}

.sb-content {
  flex: 1;
  background: var(--bg);
}

@media (max-width: 900px) {
  .sidebar { width: 72px; padding: 22px 10px; }
  .sb-brand-name, .sb-user-info, .sb-item span, .sb-foot span { display: none; }
  .sb-item { justify-content: center; padding: 12px; }
  .sb-user { justify-content: center; }
}
</style>
