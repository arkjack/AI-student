<template>
  <div class="home">
    <div class="page-head">
      <div>
        <h1>{{ title }}</h1>
        <p class="sub">脚手架占位页 —— 把它替换成真实业务页面</p>
      </div>
      <span class="k-tag k-tag--orange">脚手架生成</span>
    </div>

    <section class="k-card">
      <h3>接下来做什么</h3>
      <ol class="steps">
        <li>把本文件替换为真实页面（各端目录见 <code>src/views/</code>）</li>
        <li>在 <code>src/router/index.js</code> 注册路由（把 TODO 行换成真实页面）</li>
        <li>在对应 Layout 的 <code>menus</code> 数组里加菜单项</li>
        <li>后端接口全部经 <code>@/api/request</code> 调用，不要裸写 axios</li>
      </ol>
      <p class="hint">
        设计系统变量见 <code>src/styles/main.css</code> 的 <code>:root</code>：
        用 <code>var(--brand)</code> / <code>var(--ink-2)</code> / <code>var(--radius-lg)</code>，
        不要写死颜色和圆角。
      </p>
    </section>

    <section class="k-card">
      <h3>连通性自检</h3>
      <el-button :loading="loading" @click="ping">调用一次公开接口</el-button>
      <pre v-if="result" class="result">{{ result }}</pre>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/api/request'

const route = useRoute()
const title = computed(() => route.meta.title || '首页')

const loading = ref(false)
const result = ref('')

/** 建议后端提供一个无需登录的公开接口（如 GET /api/public/ping）用于自检 */
async function ping() {
  loading.value = true
  result.value = ''
  try {
    const data = await request.get('/public/ping')
    result.value = '✅ 前后端连通\n' + JSON.stringify(data, null, 2)
  } catch (e) {
    result.value = '❌ 调用失败：' + (e.message || e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home { display: flex; flex-direction: column; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.page-head h1 { margin: 0 0 6px; font-size: var(--fs-h1); }
.sub { margin: 0; color: var(--ink-2); }
.k-card { padding: 20px 22px; }
.k-card h3 { margin: 0 0 12px; font-size: var(--fs-h3); }
.steps { margin: 0 0 12px; padding-left: 20px; line-height: var(--leading-body); color: var(--ink-2); }
.steps code { background: var(--surface-soft); padding: 1px 6px; border-radius: 6px; font-size: 12.5px; color: var(--brand-deep); }
.hint { margin: 0; color: var(--ink-3); font-size: var(--fs-sm); line-height: var(--leading-body); }
.result { margin-top: 12px; background: #0f172a; color: #a5f3fc; padding: 14px 16px; border-radius: var(--radius-md); font-size: 12.5px; line-height: 1.7; overflow: auto; }
</style>
