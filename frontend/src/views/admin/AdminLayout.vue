<template>
  <SidebarLayout :user="user" :menus="menus" brand-path="/admin/dashboard" :show-notice="false" />
</template>

<script setup>
import SidebarLayout from '@/views/shared/SidebarLayout.vue'
import { PhChartLineUp as ChartLineUp, PhUsers as Users, PhBookOpen as BookOpen, PhFloppyDisk as FloppyDisk, PhMegaphone as Megaphone, PhShieldCheck as ShieldCheck } from '@phosphor-icons/vue'

const user = { nickname: '管理员', avatar: '🛠️', roleLabel: '管理员 · 平台运营' }

const menus = [
  { title: '数据看板', path: '/admin/dashboard', icon: ChartLineUp },
  { title: '用户管理', path: '/admin/users', icon: Users },
  { title: '课程管理', path: '/admin/courses', icon: BookOpen },
  { title: '实验资源', path: '/admin/lab', icon: FloppyDisk },
  { title: '内容安全', path: '/admin/safety', icon: ShieldCheck },
  { title: '公告与日志', path: '/admin/announcements', icon: Megaphone }
]
</script>
