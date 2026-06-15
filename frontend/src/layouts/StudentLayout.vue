<script setup lang="ts">
import {
  BookOutline,
  ClipboardOutline,
  CloudOutline,
  HomeOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import AppShell from '@/components/AppShell.vue'

const menu = [
  { label: '首页', key: '/student/home', icon: HomeOutline, match: ['/student/home'] },
  { label: '我的课程', key: '/student/courses', icon: BookOutline, match: ['/student/courses'] },
  {
    label: '待完成',
    key: '/student/exams',
    icon: ClipboardOutline,
    match: ['/student/exams', '/student/projects', '/student/tasks'],
    children: [
      { label: '考试', key: '/student/exams', match: ['/student/exams'] },
      { label: '项目', key: '/student/projects', match: ['/student/projects'] },
    ],
  },
  { label: '学习评价', key: '/student/evaluation', icon: StatsChartOutline, match: ['/student/evaluation'] },
  { label: '我的网盘', key: '/student/drive', icon: CloudOutline, match: ['/student/drive'] },
]
</script>

<template>
  <AppShell brand="信息科技课堂" section="学生空间" :menu="menu" />
</template>
