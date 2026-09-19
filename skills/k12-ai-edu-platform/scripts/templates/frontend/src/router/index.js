/* ============================================================
   路由骨架：三端分离（/student /teacher /admin）
   脚手架只挂最小可跑集合，业务页面按 SKILL.md 阶段 5 逐步补齐
   ============================================================ */
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/', redirect: '/student/home' },

  // ---------- 学生端：顶部导航 ----------
  {
    path: '/student',
    component: () => import('@/views/student/StudentLayout.vue'),
    children: [
      { path: 'home', name: 'StudentHome', component: () => import('@/views/shared/Home.vue'), meta: { title: '首页' } }
      // TODO 课程中心 courses / 课程详情 course/:id / 编程实验室 lab /
      //      AI 实验室 ai / 作业中心 homework / 答疑 qa / 学习数据 stats /
      //      个人中心 profile / 消息中心 messages
    ]
  },

  // ---------- 教师端：侧栏布局 ----------
  {
    path: '/teacher',
    component: () => import('@/views/shared/SidebarLayout.vue'),
    props: { role: 'teacher' },
    redirect: '/teacher/workbench',
    children: [
      { path: 'workbench', name: 'TeacherWorkbench', component: () => import('@/views/shared/Home.vue'), meta: { title: '工作台' } }
      // TODO 班级管理 classes / 任务布置 tasks / 进度追踪 progress /
      //      作业批改 review / 答疑 qa / 消息中心 messages
    ]
  },

  // ---------- 管理端：同一套侧栏布局，靠 props 区分菜单 ----------
  {
    path: '/admin',
    component: () => import('@/views/shared/SidebarLayout.vue'),
    props: { role: 'admin' },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/shared/Home.vue'), meta: { title: '数据看板' } }
      // TODO 用户管理 users / 课程管理 courses / 实验资源 lab / 公告与日志 announcements
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/* ============================================================
   登录态与角色守卫
   ------------------------------------------------------------
   为什么必须有这一段：守卫若不拦，未登录访问 /student/home 时
   页面会先挂载，组件各自打接口，多个请求全部 401，
   每个都弹一次「请先登录」→ 登录页堆成一摞提示。

   ⚠️ 这只是前端体验层拦截，真正的权限校验在后端
      AuthInterceptor + @RequireRole，前端守卫不能替代后端。
   ============================================================ */
const ROLE_HOME = { 0: '/admin/dashboard', 1: '/teacher/workbench', 2: '/student/home' }
const ROLE_PREFIX = { 0: '/admin', 1: '/teacher', 2: '/student' }
const GUARDED_PREFIXES = ['/admin', '/teacher', '/student']

/** 当前登录角色；无 token 返回 null，token 在但信息损坏时按学生处理 */
function currentRole() {
  if (!localStorage.getItem('token')) return null
  try {
    const role = Number(JSON.parse(localStorage.getItem('userInfo') || '{}').role)
    return Number.isInteger(role) && role >= 0 && role <= 2 ? role : 2
  } catch (e) {
    return 2
  }
}

router.beforeEach((to) => {
  document.title = to.meta.title || '教育平台'

  const role = currentRole()

  // ① 未登录：只放行登录页，其余转登录页并记录回跳地址
  if (role === null) {
    if (to.path === '/login') return true
    const query = to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {}
    return { path: '/login', query }
  }

  // ② 已登录还去登录页（含退出后手滑回退）→ 回自己角色的首页
  if (to.path === '/login') {
    return ROLE_HOME[role] || '/student/home'
  }

  // ③ 角色越权（如学生直接敲 /admin/dashboard）→ 送回自己的首页
  const allowed = ROLE_PREFIX[role]
  if (GUARDED_PREFIXES.some(p => to.path.startsWith(p)) && !to.path.startsWith(allowed)) {
    return ROLE_HOME[role] || '/student/home'
  }

  return true
})

export default router
