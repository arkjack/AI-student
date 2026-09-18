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
        <h2>
          {{ user.nickname }}
          <el-tag round type="success" size="small">{{ user.levelEmoji }} {{ user.levelTitle }}</el-tag>
        </h2>
        <div class="tags">
          <span class="k-tag"><GraduationCap :size="16" weight="bold" /> {{ classText }}</span>
          <span class="k-tag k-tag--orange"><Sun :size="16" weight="bold" /> 连续学习 {{ user.streakDays }} 天</span>
          <span class="k-tag k-tag--orange exp-tag" title="点击查看经验明细" @click="openExpLog">
            <Star :size="16" weight="fill" /> 经验值 {{ user.exp }} · 明细 ›
          </span>
        </div>
        <div class="exp-line">
          <div class="exp-bar"><div class="exp-fill" :style="{ width: user.levelProgress + '%' }"></div></div>
          <span>
            Lv.{{ user.level }} {{ user.levelTitle }} · 本级 {{ user.levelExp }}/{{ user.levelExpNeed }}
            <em class="exp-next">（距升级还差 {{ user.nextLevelExp }} 经验）</em>
          </span>
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
          <!--
            作品状态共四个分支，判断顺序不能换：
            1) score 非 null/undefined → 老师已批改，显示「XX 分」。
               注意 0 分是合法分数，必须显式判空，不能用真值判断（if (p.score) 会把 0 分误判）。
            2) 来自教师布置的编程任务（assignmentId 非空）、已提交但还没批改 → 「待批改」，
               用 warning 橙色而不是 success 绿色，避免学生误以为已经得分。
            3) 已提交但不是任务作品（学生自己平时提交的）→ 「已提交」。
               这类作品老师不会批改，显示「待批改」会让学生一直空等，是错误的文案。
            4) 其余是草稿。
          -->
          <el-tag v-if="p.score !== null && p.score !== undefined" type="success" round size="small">{{ p.score }} 分</el-tag>
          <el-tag v-else-if="p.status === '已提交' && p.assignmentId" type="warning" round size="small">待批改</el-tag>
          <el-tag v-else-if="p.status === '已提交'" type="info" round size="small">已提交</el-tag>
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
        <span class="set-label">
          <Bell :size="16" weight="bold" /> 学习提醒
          <em class="set-hint">连续 2 天没有学习时发消息提醒你</em>
        </span>
        <el-switch v-model="settings.remind" :loading="settingsLoading" @change="savePreferences" />
      </div>
      <div class="set-item">
        <span class="set-label">
          <MoonStars :size="16" weight="bold" /> 消息通知
          <em class="set-hint">总开关，关闭后不再接收任何站内消息</em>
        </span>
        <el-switch v-model="settings.notify" :loading="settingsLoading" @change="savePreferences" />
      </div>
      <div class="set-item">
        <span class="set-label"><LockKey :size="16" weight="bold" /> 修改密码</span>
        <el-button link type="primary" @click="openPwdDialog">前往修改 ›</el-button>
      </div>
      <div class="set-item">
        <span class="set-label"><Question :size="16" weight="bold" /> 帮助与反馈</span>
        <el-button link type="primary" @click="$router.push('/student/guide')">查看新手教程 ›</el-button>
      </div>
    </section>

    <!-- ========== 修改密码 ========== -->
    <el-dialog
      v-model="pwdDialog"
      title="修改密码"
      width="430px"
      :close-on-click-modal="false"
      @closed="resetPwdForm"
    >
      <el-form :model="pwdForm" label-position="top" @submit.prevent>
        <el-form-item label="原密码">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            size="large"
            placeholder="请输入当前使用的密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            size="large"
            placeholder="8~32 位，需同时包含字母和数字"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            size="large"
            placeholder="再输入一次新密码"
            autocomplete="new-password"
          />
        </el-form-item>
        <p class="pwd-tip">
          ⚠️ 修改成功后需要<strong>用新密码重新登录</strong>，其他设备上的登录状态也会立即失效。
        </p>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- ========== 经验明细 ========== -->
    <el-drawer v-model="expDrawer" title="经验明细" size="400px">
      <p class="exp-drawer-tip">
        经验来自真实学习行为，只会增加不会减少。每天最多可获得 300 经验。
      </p>
      <div v-if="expLogs.length" class="exp-log">
        <div v-for="e in expLogs" :key="e.id" class="exp-item">
          <div class="exp-item-main">
            <span class="exp-item-remark">{{ e.remark || typeLabel(e.sourceType) }}</span>
            <span class="exp-item-time">{{ fmtExpTime(e.createdAt) }}</span>
          </div>
          <span class="exp-item-val" :class="{ minus: e.exp < 0 }">
            {{ e.exp > 0 ? '+' : '' }}{{ e.exp }}
          </span>
        </div>
      </div>
      <el-empty v-else description="还没有经验记录，去课程中心学第一课吧" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { clearAuth } from '@/api/request'
import { PhPencilSimple as PencilSimple, PhGraduationCap as GraduationCap, PhSun as Sun, PhStar as Star, PhUserCircle as UserCircle, PhCheck as Check, PhTrophy as Trophy, PhPuzzlePiece as PuzzlePiece, PhPlus as Plus, PhBell as Bell, PhMoonStars as MoonStars, PhLockKey as LockKey, PhQuestion as Question } from '@phosphor-icons/vue'

import { buildAchievements } from '@/utils/achievements'

/* ============ 组件内 mock 数据：接口失败时的 fallback ============ */
// 注意：连续学习 / 经验值 / 等级 / 成就徽章都不在这里 —— 它们读 /stats/student/my 的真实值
const FALLBACK_USER = {
  id: 1, username: 'xiaoming', nickname: '小明', role: 'student',
  class: '五年级(2)班', avatar: '🦊', level: 1, levelTitle: 'AI 新芽', levelEmoji: '🌱',
  levelExp: 0, levelExpNeed: 100, levelProgress: 0, nextLevelExp: 100,
  exp: 0, streakDays: 0
}
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

/* ============ 偏好设置（学习提醒 / 消息通知）—— 存后端，刷新不丢 ============ */
const settings = ref({ remind: true, notify: true })
const settingsLoading = ref(false)

const loadPreferences = async () => {
  try {
    const p = await request.get('/user/preferences')
    settings.value = { remind: p?.remindEnabled !== false, notify: p?.notifyEnabled !== false }
  } catch (e) {}
}

/** 切换开关立即落库；失败则回滚成服务端值，避免界面与实际不一致 */
const savePreferences = async () => {
  settingsLoading.value = true
  try {
    const p = await request.put('/user/preferences', {
      remindEnabled: settings.value.remind,
      notifyEnabled: settings.value.notify
    })
    settings.value = { remind: p?.remindEnabled !== false, notify: p?.notifyEnabled !== false }
    ElMessage.success('设置已保存')
  } catch (e) {
    await loadPreferences()
  } finally {
    settingsLoading.value = false
  }
}

/* ============ 修改密码 ============ */
const router = useRouter()
const pwdDialog = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const resetPwdForm = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
}

const openPwdDialog = () => {
  resetPwdForm()
  pwdDialog.value = true
}

/**
 * 前端预校验只为少发无效请求、给出即时反馈。
 * 真正的强度校验与身份校验全部在后端 —— 前端校验可被绕过，不能作为安全依据。
 */
const validatePwdForm = () => {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword) return '请输入原密码'
  if (!newPassword) return '请输入新密码'
  if (newPassword.length < 8 || newPassword.length > 32) return '新密码长度需为 8~32 位'
  if (!/[A-Za-z]/.test(newPassword) || !/\d/.test(newPassword)) return '新密码需同时包含字母和数字'
  if (newPassword === oldPassword) return '新密码不能与原密码相同'
  if (newPassword !== confirmPassword) return '两次输入的新密码不一致'
  return ''
}

const submitPassword = async () => {
  const err = validatePwdForm()
  if (err) {
    ElMessage.warning(err)
    return
  }
  pwdLoading.value = true
  try {
    await request.post('/user/change-password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    pwdDialog.value = false
    ElMessage.success('密码修改成功，请用新密码重新登录')
    // 后端已通过 password_changed_at 作废旧 token，这里同步清本地登录态并回登录页
    clearAuth()
    router.push('/login')
  } catch (e) {
    // 具体原因（原密码不正确 / 失败次数过多 / 强度不足）由响应拦截器统一提示
  } finally {
    pwdLoading.value = false
  }
}

// 成就徽章：按真实学习统计实时判定（接口回来后刷新）
const achievements = ref(buildAchievements(null))

/* 学习统计：连续学习天数 / 经验值 / 等级 / 成就徽章统一从统计接口取，
   接口失败时保持默认值（0 天 / Lv.1），不显示编造的数字 */
const loadStats = async () => {
  try {
    const s = await request.get('/stats/student/my')
    user.value.streakDays = s?.streakDays ?? 0
    user.value.exp = s?.exp ?? 0
    user.value.level = s?.level ?? 1
    user.value.levelTitle = s?.levelTitle || 'AI 新芽'
    user.value.levelEmoji = s?.levelEmoji || '🌱'
    user.value.levelExp = s?.levelExp ?? 0
    user.value.levelExpNeed = s?.levelExpNeed ?? 100
    user.value.levelProgress = s?.levelProgress ?? 0
    user.value.nextLevelExp = s?.nextLevelExp ?? 100
    achievements.value = buildAchievements(s)
  } catch (e) {}
}

/* ============ 经验明细 ============ */
const expDrawer = ref(false)
const expLogs = ref([])

const EXP_TYPE_LABEL = {
  daily: '每日首次学习',
  course: '课程学习',
  project: '编程作品',
  writing: 'AI 写作',
  quiz: '知识闯关',
  submission: '作业提交',
  streak: '连续学习奖励',
  backfill: '历史学习成果'
}
const typeLabel = (t) => EXP_TYPE_LABEL[t] || '学习奖励'

const fmtExpTime = (v) => {
  if (!v) return ''
  const d = new Date(String(v).replace(/-/g, '/'))
  if (isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const openExpLog = async () => {
  expDrawer.value = true
  try {
    expLogs.value = (await request.get('/exp/log', { params: { limit: 30 } })) || []
  } catch (e) {
    expLogs.value = []
  }
}

// 作品集走接口
const works = ref([])
const loadWorks = async () => {
  try {
    const list = await request.get('/project/my')
    works.value = (list || []).map(p => ({
      id: p.id,
      name: p.title,
      status: p.status === 1 ? '已提交' : '草稿',
      score: p.score,
      // 来源任务 id：非空表示这是为教师布置的编程任务提交的，老师会批改
      assignmentId: p.assignmentId
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
  loadStats()
  loadPreferences()
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
  flex-wrap: wrap;
}

/* 开关的说明文字：把「这个开关到底管什么」写清楚，避免用户不知道点了会怎样 */
.set-hint {
  font-style: normal;
  font-size: 12px;
  color: var(--ink-3);
  margin-left: 2px;
}

.pwd-tip {
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-3);
  background: var(--brand-grad-soft, #eef4ff);
  border-radius: var(--radius-sm, 8px);
  padding: 10px 12px;
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

/* ---------- 经验明细 ---------- */
.exp-tag {
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.exp-tag:hover {
  box-shadow: 0 3px 10px rgba(59, 130, 246, 0.18);
}

.exp-next {
  font-style: normal;
  color: var(--ink-3);
}

.exp-drawer-tip {
  margin: 0 0 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-3);
  background: var(--brand-grad-soft, #eef4ff);
  border-radius: var(--radius-sm, 8px);
  padding: 10px 12px;
}

.exp-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed var(--line);
}

.exp-item:last-child {
  border-bottom: none;
}

.exp-item-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.exp-item-remark {
  font-size: 13.5px;
  color: var(--ink);
  line-height: 1.5;
}

.exp-item-time {
  font-size: 12px;
  color: var(--ink-3);
}

.exp-item-val {
  flex-shrink: 0;
  font-weight: 700;
  font-size: 15px;
  color: #16a34a;
}

.exp-item-val.minus {
  color: #dc2626;
}
</style>
