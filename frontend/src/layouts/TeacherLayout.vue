<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { NSelect } from 'naive-ui'
import {
  BarChartOutline,
  BookOutline,
  CloudDownloadOutline,
  PeopleOutline,
  ReaderOutline,
  SchoolOutline,
} from '@vicons/ionicons5'
import AppShell from '@/components/AppShell.vue'
import { listAllClasses } from '@/api/classes'
import { useClassFilterStore } from '@/stores/classFilter'
import type { ClassVO } from '@/types/api'

const classFilter = useClassFilterStore()
const classes = ref<ClassVO[]>([])
const route = useRoute()

listAllClasses().then(list => { classes.value = list }).catch(() => {})

const selectedClassId = computed({
  get: () => classFilter.selectedClassId,
  set: (value) => classFilter.setClassId(value),
})
const showClassFilter = computed(() =>
  route.path === '/teacher/courses'
  || route.path === '/teacher/stats'
  || route.path.startsWith('/teacher/tasks'),
)

const menu = [
  { label: '工作台', key: '/teacher/home', icon: BarChartOutline, match: ['/teacher/home'] },
  { label: '课程', key: '/teacher/courses', icon: BookOutline, match: ['/teacher/courses'] },
  { label: '作业与评分', key: '/teacher/tasks', icon: ReaderOutline, match: ['/teacher/tasks', '/teacher/grading'] },
  { label: '学生', key: '/teacher/students', icon: PeopleOutline, match: ['/teacher/students'] },
  {
    label: '考试与项目',
    key: '/teacher/exams',
    icon: SchoolOutline,
    match: ['/teacher/exams', '/teacher/projects'],
    children: [
      { label: '考试管理', key: '/teacher/exams', match: ['/teacher/exams'] },
      { label: '项目管理', key: '/teacher/projects', match: ['/teacher/projects'] },
    ],
  },
  { label: '数据分析', key: '/teacher/stats', icon: BarChartOutline, match: ['/teacher/stats'] },
  { label: '成绩导出', key: '/teacher/grade-export', icon: CloudDownloadOutline, match: ['/teacher/grade-export'] },
]
</script>

<template>
  <AppShell brand="课堂管理" section="教师工作台" :menu="menu">
    <template #header-left>
      <NSelect
        v-if="showClassFilter"
        v-model:value="selectedClassId"
        :options="classes.map(c => ({ label: c.grade + '级' + c.name, value: c.id }))"
        placeholder="当前班级"
        clearable
        class="class-filter desktop-filter"
        size="small"
      />
    </template>
    <template #drawer-extra>
      <NSelect
        v-if="showClassFilter"
        v-model:value="selectedClassId"
        :options="classes.map(c => ({ label: c.grade + '级' + c.name, value: c.id }))"
        placeholder="当前班级"
        clearable
        class="mobile-filter"
      />
    </template>
  </AppShell>
</template>

<style scoped>
.class-filter {
  width: 180px;
}

@media (max-width: 640px) {
  .desktop-filter {
    display: none;
  }
}

.mobile-filter { width: 100%; }
</style>
