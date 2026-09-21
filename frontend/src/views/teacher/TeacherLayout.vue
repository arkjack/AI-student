<template>
  <SidebarLayout :user="user" :menus="menus" brand-path="/teacher/workbench" />
</template>

<script setup>
import SidebarLayout from '@/views/shared/SidebarLayout.vue'
import { PhHouse as House, PhUsers as Users, PhPushPin as PushPin, PhChartLineUp as ChartLineUp, PhNotePencil as NotePencil, PhChatCircleDots as ChatCircleDots, PhShieldCheck as ShieldCheck } from '@phosphor-icons/vue'

const user = { nickname: '秦老师', avatar: '👩‍🏫', roleLabel: '教师 · 五年级(2)班' }

const menus = [
  { title: '工作台', path: '/teacher/workbench', icon: House },
  { title: '班级管理', path: '/teacher/classes', icon: Users },
  { title: '任务布置', path: '/teacher/tasks', icon: PushPin },
  { title: '进度追踪', path: '/teacher/progress', icon: ChartLineUp },
  { title: '作业批改', path: '/teacher/review', icon: NotePencil },
  { title: '内容复核', path: '/teacher/content-review', icon: ShieldCheck },
  { title: '答疑互动', path: '/teacher/qa', icon: ChatCircleDots }
]
</script>
