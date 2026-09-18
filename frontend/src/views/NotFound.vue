<template>
  <div class="nf">
    <div class="nf-emoji k-float">🛸</div>
    <h1>404 · 飞船迷路啦</h1>
    <p>你要找的页面跑到外太空去了…</p>
    <button class="k-btn" @click="$router.push('/student/home')">
      <House :size="16" weight="bold" /> 带我回首页
    </button>
  </div>
</template>

<script setup>
import { PhHouse as House } from '@phosphor-icons/vue'
</script>

<style scoped>
.nf {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: radial-gradient(circle at 50% 30%, #eff6ff, #f3f7fe);
}

.nf-emoji {
  font-size: 90px;
}

.nf h1 {
  margin: 14px 0 6px;
  font-size: 34px;
}

.nf p {
  color: var(--ink-2);
  margin: 0 0 22px;
}
</style>
