<template>
  <div class="guide-page">
    <!-- 头图 -->
    <section class="guide-hero k-card">
      <div class="hero-text">
        <h1>👋 欢迎来到 AI 启蒙星球</h1>
        <p>花 3 分钟看完这份教程，你就能自己玩转整个平台 —— 看课、拼积木、玩 AI、交作业，一条线走完。</p>
        <div class="hero-actions">
          <button class="k-btn" @click="$router.push('/student/courses')">
            <Rocket :size="16" weight="bold" /> 现在就去学第一课
          </button>
          <button class="k-btn k-btn--ghost" @click="$router.push('/student/qa')">
            <ChatCircleDots :size="16" weight="bold" /> 还有问题，问老师
          </button>
        </div>
      </div>
      <div class="hero-emoji">🚀</div>
    </section>

    <!-- 快速上手 -->
    <div class="k-section-head"><h2>五分钟快速上手</h2></div>
    <section class="steps">
      <div v-for="(s, i) in steps" :key="s.title" class="step k-card">
        <div class="step-no">{{ i + 1 }}</div>
        <div class="step-body">
          <h3>{{ s.emoji }} {{ s.title }}</h3>
          <p>{{ s.desc }}</p>
          <button class="k-btn k-btn--ghost step-btn" @click="$router.push(s.path)">
            {{ s.action }} ›
          </button>
        </div>
      </div>
    </section>

    <!-- 功能地图 -->
    <div class="k-section-head"><h2>各个页面能做什么</h2></div>
    <section class="modules k-card">
      <div v-for="m in modules" :key="m.name" class="module-row">
        <span class="m-emoji">{{ m.emoji }}</span>
        <span class="m-name">{{ m.name }}</span>
        <span class="m-desc">{{ m.desc }}</span>
      </div>
    </section>

    <!-- 常见问题 -->
    <div class="k-section-head"><h2>常见问题</h2></div>
    <section class="faq k-card">
      <el-collapse v-model="openedFaq">
        <el-collapse-item v-for="(f, i) in faqs" :key="f.q" :title="f.q" :name="String(i)">
          <p class="faq-a">{{ f.a }}</p>
        </el-collapse-item>
      </el-collapse>
    </section>

    <!-- 底部 -->
    <section class="guide-foot k-card">
      <div>
        <h3>没找到答案？</h3>
        <p>把问题发到「答疑互动」，你的班主任老师会收到并回复你；简单的知识问题 AI 也会先给你讲讲。</p>
      </div>
      <button class="k-btn" @click="$router.push('/student/qa')">
        <ChatCircleDots :size="16" weight="bold" /> 去提问
      </button>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  PhRocket as Rocket,
  PhChatCircleDots as ChatCircleDots
} from '@phosphor-icons/vue'

const openedFaq = ref(['0'])

const steps = [
  {
    emoji: '🪪',
    title: '先完善一下资料',
    desc: '去「个人中心」换个昵称和头像，这样老师和同学才认得你。',
    action: '去个人中心',
    path: '/student/profile'
  },
  {
    emoji: '🏫',
    title: '加入你的班级',
    desc: '注册时选了班级的话，等班主任在「班级管理」里同意就正式入班了；入班后才能收到老师布置的作业。',
    action: '去个人中心看班级状态',
    path: '/student/profile'
  },
  {
    emoji: '📺',
    title: '看完第一门课',
    desc: '到「课程中心」点开《什么是人工智能？》。视频会边看边记录进度，看完 100% 老师那边会显示任务完成。',
    action: '去课程中心',
    path: '/student/courses'
  },
  {
    emoji: '🧩',
    title: '拼一个积木作品',
    desc: '「编程实验室」里挑一个模板，把积木拖到工作区，点运行看效果。满意后点「保存」是草稿，点「提交作品」老师才能看到。',
    action: '去编程实验室',
    path: '/student/lab'
  },
  {
    emoji: '✨',
    title: '让 AI 帮你写个故事',
    desc: '「AI 魔法实验室」能根据你给的主题写童话、出知识闯关题，试试输入「会飞的学校」。',
    action: '去 AI 实验室',
    path: '/student/ai'
  }
]

const modules = [
  { emoji: '🏠', name: '首页', desc: '公告、班级通知、继续学上次没学完的课' },
  { emoji: '📚', name: '课程中心', desc: '按分类和难度找课，卡片上直接显示你的学习进度' },
  { emoji: '🧩', name: '编程实验室', desc: '拖拽积木编程，可保存草稿、提交作品给老师' },
  { emoji: '✨', name: 'AI 魔法实验室', desc: 'AI 创意写作 + 知识闯关，边玩边学' },
  { emoji: '📝', name: '作业中心', desc: '接收老师布置的任务、提交作业、查看批改和得分' },
  { emoji: '💬', name: '答疑互动', desc: '把不懂的问题发给班主任老师，等着回复' },
  { emoji: '📈', name: '学习数据', desc: '连续学习天数、作品数、闯关次数、得分趋势' },
  { emoji: '🔔', name: '消息中心', desc: '任务、批改、公告都会在这里通知你，顶栏铃铛可以快速查看' }
]

const faqs = [
  {
    q: '学习进度是怎么算的？会不会算错？',
    a: '视频播放时平台会按时间上报进度，后端只取「已存进度」和「本次上报」里更大的那个，所以进度只增不减。进度到 100% 时，老师布置的对应课程任务会自动标记为完成。'
  },
  {
    q: '为什么有的课程显示「该课程暂无站内视频」？',
    a: '那门课的视频目前是站外链接（比如发布在短视频平台），平台不能直接播放，点「前往观看原视频」可以去原地址看；看完回到平台，进度仍会照常记录。'
  },
  {
    q: '我保存了作品，为什么老师那边看不到？',
    a: '「保存」存的是草稿，只有你自己能看到。要让老师批改，需要在编程实验室点「提交作品」，或在作业中心提交。'
  },
  {
    q: '登录密码忘了怎么办？',
    a: '登录状态下可以自己改：个人中心 → 设置 → 修改密码，需要先输入原密码。如果已经登不进来，请联系班主任或平台管理员重置。'
  },
  {
    q: '「连续学习天数」是怎么统计的？',
    a: '只要当天有任意一种学习行为就算数：看课进度更新、完成一次知识闯关、提交作业、提交编程作品、用 AI 写一篇作文。今天还没开始学也不会立刻清零，会从昨天往前算。'
  },
  {
    q: '为什么我的班级一直显示「待审批」？',
    a: '注册时选班级只是发起申请，需要班主任在「班级管理」里点同意才算正式入班。等不及的话可以直接问一下老师。'
  },
  {
    q: '消息太多，能关掉通知吗？',
    a: '可以。个人中心 → 设置里，「消息通知」是总开关，关掉后不再接收任何站内消息；「学习提醒」是子开关，控制是否在你连续几天没学习时提醒你。'
  }
]
</script>

<style scoped>
.guide-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 20px 40px;
}

/* ---------- 头图 ---------- */
.guide-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 34px;
  background: linear-gradient(135deg, #1e4fd8 0%, #2f5cd8 55%, #4f7ff0 100%);
  color: #fff;
  border: none;
}

.hero-text h1 {
  font-family: var(--font-title);
  font-size: 26px;
  margin: 0 0 10px;
  color: #fff;
}

.hero-text p {
  margin: 0 0 18px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
  max-width: 620px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.guide-hero .k-btn {
  background: #fff;
  color: var(--brand-deep);
  box-shadow: none;
}

.guide-hero .k-btn--ghost {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.45);
}

.hero-emoji {
  font-size: 76px;
  line-height: 1;
  flex-shrink: 0;
}

/* ---------- 快速上手 ---------- */
.steps {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.step {
  display: flex;
  gap: 14px;
  padding: 18px 20px;
  align-items: flex-start;
}

.step-no {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-body h3 {
  margin: 0 0 6px;
  font-size: 15px;
  color: var(--ink);
}

.step-body p {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-2);
}

.step-btn {
  font-size: 13px;
  padding: 5px 12px;
}

/* ---------- 功能地图 ---------- */
.modules {
  padding: 8px 22px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px solid var(--line);
  font-size: 13.5px;
}

.module-row:last-child {
  border-bottom: none;
}

.m-emoji {
  font-size: 20px;
  flex-shrink: 0;
}

.m-name {
  width: 110px;
  flex-shrink: 0;
  font-weight: 600;
  color: var(--ink);
}

.m-desc {
  color: var(--ink-2);
  line-height: 1.6;
}

/* ---------- FAQ ---------- */
.faq {
  padding: 6px 22px;
}

.faq-a {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-2);
}

/* ---------- 底部 ---------- */
.guide-foot {
  margin-top: 20px;
  padding: 24px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.guide-foot h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--ink);
}

.guide-foot p {
  margin: 0;
  font-size: 13.5px;
  color: var(--ink-2);
  line-height: 1.7;
  max-width: 640px;
}

@media (max-width: 720px) {
  .hero-emoji { display: none; }
  .m-name { width: auto; }
  .module-row { flex-wrap: wrap; }
}
</style>
