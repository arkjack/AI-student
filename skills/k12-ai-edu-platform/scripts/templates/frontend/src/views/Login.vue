<template>
  <div class="login-page">
    <div class="brand-side">
      <div class="brand-inner">
        <div class="brand-logo">
          <span class="logo-emoji">🤖</span>
          <h1>AI 启蒙星球</h1>
          <p>AI 科普课、积木编程、动手实验，一条线学完。</p>
        </div>
      </div>
    </div>

    <div class="form-side">
      <div class="form-card">
        <h2>{{ isRegister ? '加入启蒙星球' : '欢迎回来' }}</h2>
        <p class="sub">{{ isRegister ? '注册一个账号，开始你的学习记录' : '登录后继续你的学习进度' }}</p>

        <div class="role-tabs">
          <span :class="{ active: !isRegister }" @click="isRegister = false">登录</span>
          <span :class="{ active: isRegister }" @click="isRegister = true">注册</span>
        </div>

        <el-form class="login-form" @submit.prevent>
          <template v-if="isRegister">
            <el-input v-model="form.nickname" placeholder="昵称（如：小明）" size="large" />
          </template>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large" />
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            {{ isRegister ? '注册并进入星球' : '进入星球' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()

const isRegister = ref(false)
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

/** 角色 → 首页。与 router/index.js 的 ROLE_HOME 必须保持一致 */
const ROLE_HOME = { 0: '/admin/dashboard', 1: '/teacher/workbench', 2: '/student/home' }

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await request.post('/auth/register', { ...form })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      return
    }

    const data = await request.post('/auth/login', {
      username: form.username,
      password: form.password
    })
    // 登录态统一存 localStorage：request.js 与路由守卫都从这里读
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.user || {}))

    const role = Number(data.user?.role ?? 2)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || ROLE_HOME[role] || '/student/home')
  } catch (e) {
    // 错误提示已由 request.js 拦截器统一处理，这里不重复弹
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: 1.15fr 1fr; }
.brand-side { background: var(--navy); color: #fff; display: flex; align-items: center; justify-content: center; }
.brand-inner { max-width: 420px; padding: 40px; }
.logo-emoji { font-size: 40px; }
.brand-logo h1 { font-family: var(--font-title); font-size: 40px; margin: 12px 0 8px; }
.brand-logo p { color: rgba(255, 255, 255, 0.72); line-height: var(--leading-body); }
.form-side { display: flex; align-items: center; justify-content: center; background: var(--bg); }
.form-card { width: 380px; background: var(--card); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 32px 30px; box-shadow: var(--shadow-card); }
.form-card h2 { margin: 0 0 6px; font-size: var(--fs-h2); }
.sub { color: var(--ink-2); font-size: var(--fs-sm); margin: 0 0 18px; }
.role-tabs { display: flex; gap: 6px; margin-bottom: 18px; }
.role-tabs span { flex: 1; text-align: center; padding: 8px 0; border-radius: var(--radius-sm); font-size: var(--fs-sm); font-weight: 700; color: var(--ink-2); cursor: pointer; background: var(--surface-soft); }
.role-tabs span.active { background: var(--brand); color: #fff; }
.login-form { display: flex; flex-direction: column; gap: 12px; }
.submit-btn { width: 100%; }
@media (max-width: 860px) { .login-page { grid-template-columns: 1fr; } .brand-side { display: none; } }
</style>
