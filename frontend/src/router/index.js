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
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/Profile.vue'), meta: { title: '个人中心' } }
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

router.beforeEach((to) => {
  // 原型阶段：默认已登录学生身份，直接放行；正式版这里做 JWT 校验
  document.title = `${to.meta.title ? to.meta.title + ' · ' : ''}AI 启蒙星球`
  return true
})

export default router
