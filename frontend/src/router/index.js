import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    redirect: '/student/home'
  },
  {
    path: '/student',
    component: () => import('@/views/student/StudentLayout.vue'),
    children: [
      { path: 'home', name: 'StudentHome', component: () => import('@/views/student/Home.vue'), meta: { title: '首页' } },
      { path: 'courses', name: 'StudentCourses', component: () => import('@/views/student/Courses.vue'), meta: { title: '课程中心' } },
      { path: 'course/:id', name: 'StudentCourseDetail', component: () => import('@/views/student/CourseDetail.vue'), meta: { title: '课程详情' } },
      { path: 'lab', name: 'StudentLab', component: () => import('@/views/student/ProgrammingLab.vue'), meta: { title: '编程实验室' } },
      { path: 'ai', name: 'StudentAiLab', component: () => import('@/views/student/AiLab.vue'), meta: { title: 'AI 魔法实验室' } },
      { path: 'homework', name: 'StudentHomework', component: () => import('@/views/student/Homework.vue'), meta: { title: '作业中心' } },
      { path: 'messages', name: 'StudentMessages', component: () => import('@/views/shared/MessageCenter.vue'), meta: { title: '消息中心' } },
      { path: 'qa', name: 'StudentQa', component: () => import('@/views/student/StudentQa.vue'), meta: { title: '答疑互动' } },
      { path: 'stats', name: 'StudentStats', component: () => import('@/views/student/Stats.vue'), meta: { title: '学习数据' } },
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'guide', name: 'StudentGuide', component: () => import('@/views/student/Guide.vue'), meta: { title: '新手教程' } }
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/views/teacher/TeacherLayout.vue'),
    redirect: '/teacher/workbench',
    children: [
      { path: 'workbench', name: 'TeacherWorkbench', component: () => import('@/views/teacher/TeacherWorkbench.vue'), meta: { title: '工作台' } },
      { path: 'classes', name: 'TeacherClasses', component: () => import('@/views/teacher/TeacherClasses.vue'), meta: { title: '班级管理' } },
      { path: 'tasks', name: 'TeacherTasks', component: () => import('@/views/teacher/TeacherTasks.vue'), meta: { title: '任务布置' } },
      { path: 'progress', name: 'TeacherProgress', component: () => import('@/views/teacher/TeacherProgress.vue'), meta: { title: '进度追踪' } },
      { path: 'review', name: 'TeacherReview', component: () => import('@/views/teacher/TeacherReview.vue'), meta: { title: '作业批改' } },
      { path: 'content-review', name: 'TeacherContentReview', component: () => import('@/views/teacher/TeacherContentReview.vue'), meta: { title: '内容复核' } },
      { path: 'messages', name: 'TeacherMessages', component: () => import('@/views/shared/MessageCenter.vue'), meta: { title: '消息中心' } },
      { path: 'qa', name: 'TeacherQa', component: () => import('@/views/teacher/TeacherQa.vue'), meta: { title: '答疑互动' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/AdminDashboard.vue'), meta: { title: '数据看板' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/AdminUsers.vue'), meta: { title: '用户管理' } },
      { path: 'courses', name: 'AdminCourses', component: () => import('@/views/admin/AdminCourses.vue'), meta: { title: '课程管理' } },
      { path: 'lab', name: 'AdminLab', component: () => import('@/views/admin/AdminLab.vue'), meta: { title: '实验资源' } },
      { path: 'safety', name: 'AdminContentSafety', component: () => import('@/views/admin/AdminContentSafety.vue'), meta: { title: '内容安全' } },
      { path: 'announcements', name: 'AdminAnnouncements', component: () => import('@/views/admin/AdminAnnouncements.vue'), meta: { title: '公告与日志' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/* ============================================================
   登录态与角色守卫
   ------------------------------------------------------------
   为什么必须有这一段：守卫若不拦，未登录访问 /student/home 时
   页面会先挂载，NoticeBell 打 2 个接口、Home 打 3 个接口，
   5 个请求全部 401，每个都弹一次「请先登录」→ 登录页堆一摞提示。
   注意：这只是前端体验层拦截，真正的权限校验在后端
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
  document.title = `${to.meta.title ? to.meta.title + ' · ' : ''}AI 启蒙星球`

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
