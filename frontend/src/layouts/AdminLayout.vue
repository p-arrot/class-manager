<script setup lang="ts">
import {
  BookOutline,
  PeopleOutline,
  SchoolOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import AppShell from '@/components/AppShell.vue'

const menu = [
  { label: '概览', key: '/admin/overview', icon: StatsChartOutline, match: ['/admin/overview'] },
  { label: '班级', key: '/admin/classes', icon: SchoolOutline, match: ['/admin/classes'] },
  { label: '教师', key: '/admin/teachers', icon: PeopleOutline, match: ['/admin/teachers'] },
  { label: '学生', key: '/admin/students', icon: BookOutline, match: ['/admin/students'] },
]
</script>

<template>
  <AppShell brand="课堂管理" section="管理员工作台" :menu="menu" />
</template>
