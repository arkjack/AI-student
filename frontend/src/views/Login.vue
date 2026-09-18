<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-inner">
        <div class="brand-logo">
          <img src="@/assets/images/logo-badge.jpg" alt="AI 启蒙星球徽章" class="logo-badge k-float" />
          <h1>AI 启蒙星球</h1>
          <p>AI 科普课、积木编程、动手实验，一条线学完。</p>
        </div>

        <div class="brand-mascot">
          <div ref="ballBox" class="ball-box"></div>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="form-card">
        <h2>{{ isRegister ? '加入启蒙星球' : '欢迎回来' }}</h2>
        <p class="sub">{{ isRegister ? '注册一个账号，开始你的学习记录' : '登录后继续你的学习进度' }}</p>

        <!-- 登录/注册 Tab -->
        <div class="role-tabs">
          <span :class="{ active: !isRegister }" @click="isRegister = false">登录</span>
          <span :class="{ active: isRegister }" @click="isRegister = true">注册</span>
        </div>

        <el-form :model="form" class="login-form" @submit.prevent>
          <template v-if="isRegister">
            <el-input v-model="form.nickname" placeholder="昵称（如：小明）" size="large">
              <template #prefix><Smiley :size="16" weight="bold" /></template>
            </el-input>
            <el-select v-model="form.classId" placeholder="选择班级（可选，需老师审批后入班）" size="large" class="class-select" clearable>
              <template #prefix><GraduationCap :size="16" weight="bold" /></template>
              <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </template>

          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><User :size="16" weight="bold" /></template>
          </el-input>

          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large">
            <template #prefix><LockKey :size="16" weight="bold" /></template>
          </el-input>

          <el-button type="primary" size="large" class="submit-btn" :loading="authLoading" @click="isRegister ? doRegister() : doLogin()">
            {{ isRegister ? '注册并进入星球' : '进入星球' }}
          </el-button>
        </el-form>

      </div>

      <p class="copyright">AI 启蒙星球 · 基于 Vue 3 + Element Plus 高保真原型</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getClasses } from '@/api/auth'
import { PhSmiley as Smiley, PhGraduationCap as GraduationCap, PhUser as User, PhLockKey as LockKey } from '@phosphor-icons/vue'

const router = useRouter()
const route = useRoute()
const isRegister = ref(false)
const form = ref({
  nickname: '',
  className: '',
  classId: null,
  username: '',
  password: ''
})

// 班级列表（注册选班用，教师端新建班级后同步更新）
const classOptions = ref([])
const loadClasses = async () => {
  try {
    const list = await getClasses()
    classOptions.value = Array.isArray(list) ? list : []
  } catch (e) {}
}

const ballBox = ref(null)
let ball = null
let removePointer = null
const authLoading = ref(false)

const doLogin = () => {
  authLoading.value = true
  login(form.value)
    .then((data) => {
      ElMessage.success('登录成功')
      afterAuth(data)
    })
    .finally(() => { authLoading.value = false })
}

const doRegister = () => {
  authLoading.value = true
  register(form.value)
    .then((data) => {
      ElMessage.success('注册成功，欢迎加入！')
      afterAuth(data)
    })
    .finally(() => { authLoading.value = false })
}

const ROLE_HOME = { 0: '/admin/dashboard', 1: '/teacher/workbench', 2: '/student/home' }
const ROLE_PREFIX = { 0: '/admin', 1: '/teacher', 2: '/student' }

const afterAuth = (data) => {
  localStorage.setItem('token', data.token)
  localStorage.setItem('userInfo', JSON.stringify(data))
  const home = ROLE_HOME[data.role] || '/student/home'
  // 被路由守卫拦下时会带上 ?redirect=原地址，登录后回跳；
  // 仅当该地址属于当前登录角色时才采用，避免跨角色跳转
  const redirect = route.query.redirect
  const prefix = ROLE_PREFIX[data.role]
  router.push(typeof redirect === 'string' && prefix && redirect.startsWith(prefix) ? redirect : home)
}

/* 按序加载 Emotion Ball 引擎脚本（rings → emotions → ball → engine） */
const loadScript = (src) =>
  new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = src
    s.onload = resolve
    s.onerror = () => reject(new Error('脚本加载失败: ' + src))
    document.head.appendChild(s)
  })

onMounted(async () => {
  loadClasses()
  if (!ballBox.value) return
  try {
    const base = '/aora-ball/js/'
    for (const f of ['rings.js', 'emotions.js', 'ball.js', 'engine.js']) {
      await loadScript(base + f)
    }
    ball = window.EmotionBall.create(ballBox.value, {
      emotion: '02',
      idle: true,
      eyeScale: 1.35
    })
    // 表情巡演：开心 → 待机 → 满意 → 好奇 → 专注
    ball.startTour(['10', '02', '19', '03', '16'], 4500)

    // 鼠标注视跟随（全页面：以窗口中心为基准归一化 → 归一化目光）
    const onMove = (e) => {
      const nx = (e.clientX / window.innerWidth) * 2 - 1
      const ny = (e.clientY / window.innerHeight) * 2 - 1
      ball.setGaze(nx, ny)
    }
    window.addEventListener('pointermove', onMove)
    removePointer = () => window.removeEventListener('pointermove', onMove)

    // 点击互动：自旋甩彩带 + 撒花
    ballBox.value.addEventListener('click', () => {
      ball.spin(2.5)
      ball.burst(18)
    })
  } catch (e) {
    console.error(e)
  }
})

onUnmounted(() => {
  removePointer?.()
  ball?.destroy()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}

/* ---------- 左侧品牌区 ---------- */
.brand-side {
  flex: 1.1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(
    180deg,
    rgba(21, 40, 92, 0.32) 0%,
    rgba(21, 40, 92, 0.06) 48%,
    rgba(21, 40, 92, 0.42) 100%
  );
}

/* 星球背景插画层（提亮显示，位于内容之下） */
.brand-side::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  background: url('@/assets/images/login-bg.jpg') center / cover no-repeat;
  filter: brightness(1.18) saturate(1.05);
  pointer-events: none;
}

.brand-inner {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.logo-badge {
  display: block;
  width: 86px;
  height: 86px;
  margin: 0 auto 8px;
  border-radius: 50%;
  object-fit: cover;
  border: 2.5px solid rgba(255, 255, 255, 0.55);
  box-shadow:
    0 12px 30px rgba(0, 0, 0, 0.28),
    0 0 0 5px rgba(255, 255, 255, 0.08);
}

.brand-logo h1 {
  font-size: 44px;
  margin: 12px 0 6px;
  color: #fff;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.brand-logo p {
  font-size: 16px;
  opacity: 0.92;
  margin: 0;
}

.brand-mascot {
  margin: 26px auto 14px;
}

.ball-box {
  width: 224px;
  height: 224px;
  margin: 0 auto;
}

/* ---------- 右侧表单区 ---------- */
.form-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-float);
  padding: 36px 32px 28px;
}

.form-card h2 {
  margin: 0;
  font-size: 26px;
}

.sub {
  color: var(--ink-2);
  font-size: 14px;
  margin: 6px 0 18px;
}

.role-tabs {
  display: flex;
  background: var(--brand-grad-soft);
  border-radius: 999px;
  padding: 4px;
  margin-bottom: 18px;
}

.role-tabs span {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-2);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.role-tabs span.active {
  background: #fff;
  color: var(--brand);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.login-form :deep(.el-input) {
  margin-bottom: 14px;
}

.login-form :deep(.el-select) {
  margin-bottom: 14px;
}

.class-select {
  width: 100%;
}

.input-emoji {
  font-size: 16px;
}

.submit-btn {
  width: 100%;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  background: var(--brand-grad);
  border: none;
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.35);
  margin-top: 4px;
}

.copyright {
  margin-top: 22px;
  color: var(--ink-3);
  font-size: 12px;
}

@media (max-width: 900px) {
  .brand-side { display: none; }
}
</style>
