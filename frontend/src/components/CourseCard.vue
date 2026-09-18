<template>
  <div class="course-card k-card k-card--hover" @click="$router.push(`/student/course/${course.id}`)">
    <!-- 封面 -->
    <div class="cover" :style="{ background: course.coverBg }">
      <img v-if="coverImg" :src="coverImg" alt="" class="cover-img" />
      <span v-else class="cover-emoji">{{ course.coverEmoji }}</span>
      <span class="cat-tag">{{ course.category }}</span>
      <span v-if="course.completed" class="done-badge"><CheckCircle :size="12" weight="fill" /> 已完成</span>
    </div>

    <div class="body">
      <h3 class="title">{{ course.title }}</h3>
      <p class="summary">{{ course.summary }}</p>

      <div class="meta">
        <span class="k-stars">★★★★<i>★</i></span>
        <span class="duration"><Timer weight="bold" /> {{ course.duration }} 分钟</span>
      </div>

      <!-- 进度 -->
      <div v-if="showProgress && course.progress > 0" class="progress-wrap">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ transform: 'scaleX(' + (course.progress / 100) + ')' }"></div>
        </div>
        <span class="progress-text">{{ course.completed ? '已完成' : '学习中 ' + course.progress + '%' }}</span>
      </div>
      <div v-else-if="showProgress" class="progress-wrap">
        <div class="progress-bar"><div class="progress-fill"></div></div>
        <span class="progress-text">未开始</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { PhCheckCircle as CheckCircle, PhTimer as Timer } from '@phosphor-icons/vue'

const props = defineProps({
  course: { type: Object, required: true },
  showProgress: { type: Boolean, default: true }
})

// 封面图：AI 生成的插画，按 id 匹配；缺失时回退 emoji 封面
const covers = import.meta.glob('/src/assets/images/course-*.jpg', { eager: true, import: 'default' })
const coverImg = computed(() => covers[`/src/assets/images/course-${props.course.id}.jpg`] || '')
</script>

<style scoped>
.course-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.cover {
  position: relative;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-emoji {
  font-size: 58px;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.18));
}

.cat-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
  padding: 3px 12px;
}

.done-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(46, 204, 113, 0.95);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
  padding: 3px 12px;
}

.body {
  padding: 14px 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.title {
  margin: 0;
  font-size: 16px;
  color: var(--ink);
  line-height: 1.4;
}

.summary {
  margin: 6px 0 10px;
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-3);
}

.duration {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.duration svg {
  color: var(--brand);
  opacity: 0.75;
}

.k-stars i {
  color: #e4eaf7;
  font-style: normal;
}

.progress-wrap {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #edf2fc;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--brand-grad);
  border-radius: 999px;
  transform-origin: left;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.progress-text {
  font-size: 12px;
  color: var(--brand);
  font-weight: 600;
  white-space: nowrap;
}
</style>
