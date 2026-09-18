<template>
  <div class="k-page">
    <div class="page-head">
      <div>
        <h1 class="k-h1-icon"><BookOpen weight="bold" /> 课程中心</h1>
        <p class="sub">按主题选课，从视频、实验到闯关任务一条线学完。</p>
      </div>
      <el-input v-model="keyword" placeholder="搜索课程…" class="search" clearable>
        <template #prefix><MagnifyingGlass weight="bold" /></template>
      </el-input>
    </div>

    <!-- 分类 + 难度 -->
    <div class="filters">
      <div class="filter-row">
        <span class="flabel">分类</span>
        <div class="chips">
          <span
            v-for="c in categories"
            :key="c.id"
            class="chip"
            :class="{ active: activeCat === c.id }"
            @click="activeCat = c.id"
          >{{ c.emoji }} {{ c.name }}</span>
        </div>
      </div>
      <div class="filter-row">
        <span class="flabel">难度</span>
        <div class="chips">
          <span
            v-for="d in levels"
            :key="d.value"
            class="chip"
            :class="{ active: activeLevel === d.value }"
            @click="activeLevel = d.value"
          >{{ d.label }}</span>
        </div>
      </div>
    </div>

    <!-- 课程网格 -->
    <div class="k-grid course-grid">
      <CourseCard v-for="c in filteredCourses" :key="c.id" :course="c" />
    </div>

    <el-empty v-if="filteredCourses.length === 0" description="没有找到相关课程" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import CourseCard from '@/components/CourseCard.vue'
import request from '@/api/request'
import { PhBookOpen as BookOpen, PhMagnifyingGlass as MagnifyingGlass } from '@phosphor-icons/vue'

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_CATEGORIES = [
  { id: 0, name: '全部', emoji: '🌈' },
  { id: 1, name: 'AI 入门', emoji: '🚀' },
  { id: 2, name: '图形化编程', emoji: '🧩' },
  { id: 3, name: '机器学习', emoji: '🤖' },
  { id: 4, name: 'AI 与生活', emoji: '🏠' },
  { id: 5, name: '趣味实验', emoji: '🧪' }
]
const FALLBACK_COURSES = [
  {
    id: 1, title: '什么是人工智能？', category: 'AI 入门', difficulty: 1,
    duration: 18, coverEmoji: '🤖', coverBg: 'linear-gradient(135deg,#60a5fa,#3b82f6)',
    summary: '和机器人小智一起认识 AI 世界的奇妙旅程！',
    progress: 100, completed: true, views: 1284, teacher: '秦老师'
  },
  {
    id: 2, title: '有趣的图像识别', category: 'AI 入门', difficulty: 2,
    duration: 25, coverEmoji: '👀', coverBg: 'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    summary: '为什么手机能认出你的脸？图像识别原理大揭秘。',
    progress: 60, completed: false, views: 986, teacher: '秦老师'
  },
  {
    id: 3, title: '方块编程第一课', category: '图形化编程', difficulty: 1,
    duration: 30, coverEmoji: '🧩', coverBg: 'linear-gradient(135deg,#2f5cd8,#163a9e)',
    summary: '拖一拖、拼一拼，用积木让角色动起来！',
    progress: 0, completed: false, views: 1523, teacher: '刘老师'
  },
  {
    id: 4, title: '认识机器学习', category: '机器学习', difficulty: 3,
    duration: 35, coverEmoji: '🧠', coverBg: 'linear-gradient(135deg,#f5a35c,#e06e1f)',
    summary: '机器是怎么学习的？像训练小狗一样训练 AI。',
    progress: 0, completed: false, views: 872, teacher: '秦老师'
  },
  {
    id: 5, title: '语音助手会说话', category: 'AI 与生活', difficulty: 2,
    duration: 20, coverEmoji: '🎙️', coverBg: 'linear-gradient(135deg,#ff8a3d,#e06e1f)',
    summary: '你好呀！看看语音助手是怎么听懂你的话。',
    progress: 0, completed: false, views: 764, teacher: '王老师'
  },
  {
    id: 6, title: 'AI 魔法画师', category: '趣味实验', difficulty: 2,
    duration: 22, coverEmoji: '🎨', coverBg: 'linear-gradient(135deg,#2f5cd8,#223a75)',
    summary: '输入一句话，AI 就能画出你心中的神奇画面！',
    progress: 0, completed: false, views: 1105, teacher: '刘老师'
  }
]

// 后端字段 → 页面字段 映射（CourseCard / 当前模板所需）
const defaultCoverBg = (c) => {
  const grads = [
    'linear-gradient(135deg,#60a5fa,#3b82f6)',
    'linear-gradient(135deg,#2f5cd8,#1e4fd8)',
    'linear-gradient(135deg,#2f5cd8,#163a9e)',
    'linear-gradient(135deg,#f5a35c,#e06e1f)',
    'linear-gradient(135deg,#ff8a3d,#e06e1f)'
  ]
  return grads[(c.categoryId || c.id || 0) % grads.length]
}
const mapCourse = (c) => ({
  id: c.id,
  title: c.title,
  category: c.categoryName || c.category || '',
  categoryId: c.categoryId,
  difficulty: c.difficulty || 1,
  duration: c.durationMinutes || c.duration || 0,
  coverEmoji: c.coverEmoji,
  coverImage: c.coverImage,
  coverBg: c.coverBg || defaultCoverBg(c),
  summary: c.summary || '',
  videoUrl: c.videoUrl,
  teacher: c.teacher,
  views: c.views,
  status: c.status,
  createdAt: c.createdAt,
  progress: typeof c.progress === 'number' ? c.progress : 0,
  completed: !!c.completed
})

const categories = ref(FALLBACK_CATEGORIES)
const courses = ref(FALLBACK_COURSES.map(mapCourse))

const levels = [
  { label: '全部', value: 0 },
  { label: '💚 入门', value: 1 },
  { label: '💛 进阶', value: 2 },
  { label: '🧡 挑战', value: 3 }
]

const activeCat = ref(0)
const activeLevel = ref(0)
const keyword = ref('')

const filteredCourses = computed(() => {
  let list = courses.value
  if (activeCat.value !== 0) {
    list = list.filter(c => c.category === categories.value.find(x => x.id === activeCat.value)?.name)
  }
  if (activeLevel.value !== 0) {
    list = list.filter(c => c.difficulty === activeLevel.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim()
    list = list.filter(c => c.title.includes(kw) || c.summary.includes(kw))
  }
  return list
})

const ALL_CAT = { id: 0, name: '全部', emoji: '🌈' }

const loadCategories = async () => {
  try {
    const list = await request.get('/course/categories')
    categories.value = [ALL_CAT, ...(list || []).map(c => ({ id: c.id, name: c.name, emoji: c.emoji }))]
  } catch (e) {
    // 接口失败则保留 FALLBACK_CATEGORIES
  }
}

const loadCourses = async () => {
  try {
    const list = await request.get('/course/list', {
      params: {
        categoryId: activeCat.value === 0 ? undefined : activeCat.value,
        difficulty: activeLevel.value === 0 ? undefined : activeLevel.value,
        keyword: keyword.value.trim() || undefined
      }
    })
    courses.value = (list || []).map(mapCourse)
  } catch (e) {
    courses.value = FALLBACK_COURSES.map(mapCourse)
  }
}

watch([activeCat, activeLevel], loadCourses)
let kwTimer
watch(keyword, () => {
  clearTimeout(kwTimer)
  kwTimer = setTimeout(loadCourses, 300)
})

onMounted(() => {
  loadCategories()
  loadCourses()
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.page-head h1 {
  margin: 4px 0 6px;
  font-size: 30px;
}

.sub {
  color: var(--ink-2);
  margin: 0;
}

.search {
  width: 280px;
}

.filters {
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 16px 22px;
  margin: 20px 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 6px 0;
}

.filter-row + .filter-row {
  border-top: 1px dashed var(--line);
}

.flabel {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-3);
  flex-shrink: 0;
  width: 44px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  padding: 5px 16px;
  border-radius: 999px;
  font-size: 14px;
  background: #f3f7fe;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.2s;
  border: 1.5px solid transparent;
  font-weight: 600;
}

.chip:hover {
  color: var(--brand);
}

.chip.active {
  background: var(--brand-grad);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.course-grid {
  grid-template-columns: repeat(3, 1fr);
  margin-top: 20px;
}

@media (max-width: 1000px) {
  .course-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
