<template>
  <div class="k-page">
    <!-- 个人信息卡 -->
    <section class="k-card profile-card">
      <div class="avatar-wrap">
        <div class="big-avatar k-float">{{ user.avatar }}</div>
        <div class="avatar-edit" @click="showAvatar = !showAvatar"><PencilSimple :size="16" weight="bold" /></div>
        <div v-if="showAvatar" class="avatar-picker">
          <span v-for="e in avatarList" :key="e" class="av-opt" :class="{ sel: user.avatar === e }" @click="user.avatar = e; showAvatar = false">{{ e }}</span>
        </div>
      </div>
      <div class="profile-info">
        <h2>{{ user.nickname }} <el-tag round type="success" size="small">一星学员</el-tag></h2>
        <div class="tags">
          <span class="k-tag"><GraduationCap :size="16" weight="bold" /> {{ classText }}</span>
          <span class="k-tag k-tag--orange"><Sun :size="16" weight="bold" /> 连续学习 {{ user.streakDays }} 天</span>
          <span class="k-tag k-tag--orange"><Star :size="16" weight="fill" /> 经验值 {{ user.exp }}</span>
        </div>
        <div class="exp-line">
          <div class="exp-bar"><div class="exp-fill" :style="{ width: pct + '%' }"></div></div>
          <span>Lv.{{ user.level }} → Lv.{{ user.level + 1 }}</span>
        </div>
      </div>
    </section>

    <div class="two-col">
      <!-- 编辑资料 -->
      <section class="k-card">
        <h3><UserCircle :size="16" weight="bold" /> 个人资料</h3>
        <el-form label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="user.nickname" />
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input v-model="sign" placeholder="写一句你的学习宣言～" />
          </el-form-item>
          <el-form-item label="班级">
            <el-input v-model="classText" disabled />
          </el-form-item>
        </el-form>
        <button class="k-btn" @click="saveProfile">
          <Check :size="16" weight="bold" /> 保存资料
        </button>
      </section>

      <!-- 成就徽章 -->
      <section class="k-card">
        <h3><Trophy :size="16" weight="bold" /> 成就徽章</h3>
        <div class="badge-grid">
          <div v-for="a in achievements" :key="a.name" class="badge" :class="{ locked: !a.got }">
            <div class="badge-emoji">{{ a.emoji }}</div>
            <b>{{ a.name }}</b>
            <span>{{ a.got ? a.desc : '未解锁' }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 作品集 -->
    <div class="k-section-head">
      <h2>我的作品集</h2>
      <span class="more" @click="$router.push('/student/lab')">去编程实验室 ›</span>
    </div>
    <section class="works-row">
      <div v-for="p in works" :key="p.id" class="work-tile" @click="$router.push('/student/lab')">
        <span class="tile-icon"><PuzzlePiece :size="24" weight="bold" /></span>
        <b>{{ p.name }}</b>
        <div class="wt-status">
          <el-tag v-if="p.status === '已提交'" type="success" round size="small">{{ p.score }} 分</el-tag>
          <el-tag v-else type="info" round size="small">草稿</el-tag>
        </div>
      </div>
      <button class="work-tile add-tile" @click="$router.push('/student/lab')"><Plus :size="16" weight="bold" /> 新建作品</button>
    </section>

    <!-- 设置（无卡列表） -->
    <div class="k-section-head">
      <h2>设置</h2>
    </div>
    <section class="settings">
      <div class="set-item">
        <span class="set-label"><Bell :size="16" weight="bold" /> 学习提醒</span>
        <el-switch v-model="settings.remind" />
      </div>
      <div class="set-item">
        <span class="set-label"><MoonStars :size="16" weight="bold" /> 消息通知</span>
        <el-switch v-model="settings.notify" />
      </div>
      <div class="set-item">
        <span class="set-label"><LockKey :size="16" weight="bold" /> 修改密码</span>
        <el-button link type="primary" @click="ElMessage.info('原型阶段：修改密码功能待接入后端')">前往修改 ›</el-button>
      </div>
      <div class="set-item">
        <span class="set-label"><Question :size="16" weight="bold" /> 帮助与反馈</span>
        <el-button link type="primary" @click="ElMessage.info('原型阶段：帮助中心待接入后端')">查看帮助 ›</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { PhPencilSimple as PencilSimple, PhGraduationCap as GraduationCap, PhSun as Sun, PhStar as Star, PhUserCircle as UserCircle, PhCheck as Check, PhTrophy as Trophy, PhPuzzlePiece as PuzzlePiece, PhPlus as Plus, PhBell as Bell, PhMoonStars as MoonStars, PhLockKey as LockKey, PhQuestion as Question } from '@phosphor-icons/vue'

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
const FALLBACK_USER = {
  id: 1, username: 'xiaoming', nickname: '小明', role: 'student',
  class: '五年级(2)班', avatar: '🦊', level: 6, exp: 1860, expNext: 2000,
  streakDays: 12, studyMinutes: 462, completedCourses: 5, worksCount: 4, avgScore: 92
}
const FALLBACK_ACHIEVEMENTS = [
  { emoji: '🚀', name: '初来乍到', desc: '完成注册', got: true },
  { emoji: '📚', name: '学习达人', desc: '累计学习 5 小时', got: true },
  { emoji: '🧩', name: '积木大师', desc: '提交 3 个编程作品', got: true },
  { emoji: '✨', name: 'AI 探险家', desc: '完成 5 个 AI 实验', got: false },
  { emoji: '🏆', name: '闯关王者', desc: '单次闯关 10 题全对', got: false }
]
const FALLBACK_PROJECTS = [
  { id: 1, name: '小猫咪动起来', status: '已提交', score: 95 },
  { id: 2, name: '会算数的机器人', status: '已提交', score: 88 },
  { id: 3, name: '我的第一个作品', status: '草稿', score: null }
]

// 用户信息：优先 localStorage，回退 mock
const storedUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
const user = ref({ ...FALLBACK_USER, ...storedUser })

// 班级状态：joined 已入班 / pending 待审批 / none 未申请
const classStatus = ref('')
const classText = computed(() => {
  if (classStatus.value === 'joined') return user.value.class || '未加入班级'
  if (classStatus.value === 'pending') return `待审批（${user.value.class || ''}）`
  return '未加入班级'
})

const loadClassStatus = async () => {
  try {
    const data = await request.get('/homework/my-class')
    if (data && data.status) {
      classStatus.value = data.status
      if (data.className) user.value.class = data.className
    }
  } catch (e) {}
}
const sign = ref('和 AI 一起探索世界，长大要当科学家！🚀')
const showAvatar = ref(false)
const avatarList = ['🦊', '🐰', '🐱', '🐻', '🐼', '🦁', '🐸', '🐧']

const settings = ref({
  remind: true,
  notify: false
})

const pct = computed(() => Math.round(user.value.exp / user.value.expNext * 100))

// 无成就接口 → 保持静态 mock
const achievements = ref(FALLBACK_ACHIEVEMENTS)

// 作品集走接口
const works = ref([])
const loadWorks = async () => {
  try {
    const list = await request.get('/project/my')
    works.value = (list || []).map(p => ({
      id: p.id,
      name: p.title,
      status: p.status === 1 ? '已提交' : '草稿',
      score: p.score
    }))
  } catch (e) {
    works.value = FALLBACK_PROJECTS
  }
}

const saveProfile = () => {
  // 原型：仅保存到本地 userInfo（后端暂无更新接口）
  const info = { ...JSON.parse(localStorage.getItem('userInfo') || '{}') }
  info.nickname = user.value.nickname
  info.avatar = user.value.avatar
  localStorage.setItem('userInfo', JSON.stringify(info))
  ElMessage.success('资料保存成功 ✨')
}

onMounted(() => {
  loadWorks()
  loadClassStatus()
})
</script>

<style scoped>
.profile-card {
  display: flex;
  gap: 26px;
  align-items: center;
  padding: 28px 30px;
  background: linear-gradient(135deg, #ffffff 0%, #eff6ff 100%);
}

.avatar-wrap {
  position: relative;
}

.big-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--brand-grad);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 52px;
  box-shadow: 0 12px 30px rgba(59, 130, 246, 0.35);
  border: 4px solid #fff;
}

.avatar-edit {
  position: absolute;
  right: -2px;
  bottom: 0;
  background: #fff;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.avatar-picker {
  position: absolute;
  top: 110px;
  left: -50px;
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow-float);
  padding: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  width: 200px;
  z-index: 10;
}

.av-opt {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  background: #f3f7fe;
  border: 2px solid transparent;
}

.av-opt.sel {
  border-color: var(--brand);
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 26px;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.exp-line {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 480px;
}

.exp-bar {
  flex: 1;
  height: 12px;
  background: #e7eefb;
  border-radius: 999px;
  overflow: hidden;
}

.exp-fill {
  height: 100%;
  background: linear-gradient(90deg, #fbbf24, #f97316);
  border-radius: 999px;
}

.exp-line span {
  font-size: 12.5px;
  color: var(--ink-3);
  font-weight: 700;
  white-space: nowrap;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 20px;
  margin-top: 20px;
}

.two-col h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 17px;
}

.two-col h3 svg {
  color: var(--brand);
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.badge {
  text-align: center;
  background: linear-gradient(160deg, #fff7e6, #fff1d6);
  border-radius: 14px;
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.badge b { font-size: 13px; color: #b7791f; }
.badge span { font-size: 11px; color: #b08a4f; }
.badge-emoji { font-size: 30px; }

.badge.locked {
  background: #f2f5fb;
  filter: grayscale(1);
  opacity: 0.7;
}

.badge.locked b, .badge.locked span { color: var(--ink-3); }

.works-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.work-tile {
  width: 140px;
  background: #f8faff;
  border-radius: var(--radius-md);
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.tile-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--brand-grad-soft);
  color: var(--brand);
}

.work-tile:hover {
  border-color: var(--brand);
  transform: translateY(-3px);
}

.work-tile b { font-size: 13px; }

.add-tile {
  border: 2px dashed #c9d8f2;
  background: transparent;
  color: var(--ink-3);
  justify-content: center;
  font-weight: 700;
}

.add-tile:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.settings {
  max-width: 720px;
}

.set-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 14.5px;
}

.set-label {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--ink);
}

.set-label svg {
  color: var(--brand);
  opacity: 0.85;
}

.set-item:last-child {
  border-bottom: none;
}

@media (max-width: 900px) {
  .two-col { grid-template-columns: 1fr; }
  .badge-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
