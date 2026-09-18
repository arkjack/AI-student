<template>
  <div class="k-page soon">
    <div class="soon-card">
      <span class="soon-emoji k-float">🚀</span>
      <h2>模块建设中</h2>
      <p>「{{ title }}」功能正在开发中，敬请期待。</p>
      <button class="k-btn k-btn--ghost" @click="$router.back()">
        <ArrowLeft :size="15" weight="bold" /> 返回
      </button>
    </div>
  </div>
</template>

<script setup>
import { PhArrowLeft as ArrowLeft } from '@phosphor-icons/vue'
defineProps({ title: { type: String, default: '此模块' } })
</script>

<style scoped>
.soon {
  display: flex;
  justify-content: center;
  min-height: 60vh;
  align-items: center;
}

.soon-card {
  text-align: center;
  padding: 40px 60px;
}

.soon-emoji {
  font-size: 64px;
  display: inline-block;
}

.soon-card h2 {
  margin: 12px 0 6px;
  font-size: 24px;
}

.soon-card p {
  color: var(--ink-2);
  margin: 0 0 20px;
}
</style>
