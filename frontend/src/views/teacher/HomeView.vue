<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NGrid, NGi, NTag, NButton, NSpin, NEmpty, NIcon, useMessage } from 'naive-ui'
import { ArrowForwardOutline, TimeOutline, DocumentOutline } from '@vicons/ionicons5'
import { getTeacherDashboard } from '@/api/dashboard'
import PageHeader from '@/components/PageHeader.vue'
import { getErrorMessage } from '@/utils/error'
import type { SubmissionVO, TaskVO } from '@/types/api'

interface UpcomingTask extends TaskVO {
  semesterName: string
  lessonName: string
}

interface RecentSubmission extends SubmissionVO {
  taskTitle: string
  semesterName: string
}

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const stats = ref({ pendingGrading: 0, upcomingDeadlines: 0, recentCount: 0 })
const pendingSubmissions = ref<RecentSubmission[]>([])
const recentSubmissions = ref<RecentSubmission[]>([])
const upcomingTasks = ref<UpcomingTask[]>([])
const showPending = ref(true)
const showUpcoming = ref(false)

async function loadDashboard() {
  loading.value = true
  try {
    const dashboard = await getTeacherDashboard()
    stats.value = {
      pendingGrading: dashboard.pendingGrading,
      upcomingDeadlines: dashboard.upcomingDeadlines,
      recentCount: dashboard.recentCount,
    }
    pendingSubmissions.value = dashboard.pendingSubmissions.map(item => ({
      ...item.submission,
      taskTitle: item.taskTitle,
      semesterName: item.semesterName,
    }))
    recentSubmissions.value = dashboard.recentSubmissions.map(item => ({
      ...item.submission,
      taskTitle: item.taskTitle,
      semesterName: item.semesterName,
    }))
    upcomingTasks.value = dashboard.upcomingTasks.map(item => ({
      ...item.task,
      semesterName: item.semesterName,
      lessonName: item.lessonName,
    }))
  } catch (e) {
    pendingSubmissions.value = []
    recentSubmissions.value = []
    upcomingTasks.value = []
    stats.value = { pendingGrading: 0, upcomingDeadlines: 0, recentCount: 0 }
    message.error(getErrorMessage(e, '加载工作台数据失败'))
  } finally {
    loading.value = false
  }
}

function goGrading(taskId: number) {
  router.push(`/teacher/grading/${taskId}`)
}

onMounted(loadDashboard)
</script>

<template>
  <div class="page">
    <PageHeader title="工作台" subtitle="今日概览" />
    <NSpin :show="loading">
      <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="12" class="stat-grid">
        <NGi><NCard size="small" class="stat-card pending interactive" hoverable role="button" tabindex="0" :aria-expanded="showPending" @click="showPending = !showPending" @keydown.enter="showPending = !showPending" @keydown.space.prevent="showPending = !showPending"><div class="stat-num">{{ stats.pendingGrading }}</div><div class="stat-label">{{ showPending ? '待评分 ↑' : '待评分 ↓' }}</div></NCard></NGi>
        <NGi><NCard size="small" class="stat-card upcoming interactive" hoverable role="button" tabindex="0" :aria-expanded="showUpcoming" @click="showUpcoming = !showUpcoming" @keydown.enter="showUpcoming = !showUpcoming" @keydown.space.prevent="showUpcoming = !showUpcoming"><div class="stat-num">{{ stats.upcomingDeadlines }}</div><div class="stat-label">{{ showUpcoming ? '即将截止 ↑' : '即将截止 ↓' }}</div></NCard></NGi>
        <NGi><NCard size="small" class="stat-card info"><div class="stat-num">{{ stats.recentCount }}</div><div class="stat-label">近期提交</div></NCard></NGi>
      </NGrid>

      <!-- Expandable: 待评分 -->
      <div v-if="showPending && pendingSubmissions.length" class="section">
        <h3 class="section-title">待评分</h3>
        <div v-for="s in pendingSubmissions" :key="'p'+s.id" class="row">
          <span class="row-title">{{ s.studentName || '学生' }}</span>
          <span class="row-sub">提交了 {{ s.taskTitle }}</span>
          <NButton size="tiny" type="primary" class="row-action" @click="goGrading(s.taskId)">去评分</NButton>
        </div>
      </div>

      <!-- Expandable: 即将截止 -->
      <div v-if="showUpcoming && upcomingTasks.length" class="section">
        <h3 class="section-title"><NIcon :size="16"><TimeOutline /></NIcon> 即将截止</h3>
        <div v-for="t in upcomingTasks" :key="'u'+t.id" class="row">
          <span class="row-title">{{ t.title }}</span>
          <NTag size="tiny" :bordered="false">{{ t.semesterName }}</NTag>
          <span class="row-meta">截止 {{ t.deadline?.split('T')[0] }}</span>
        </div>
      </div>

      <div class="section" v-if="recentSubmissions.length">
        <h3 class="section-title"><NIcon :size="16"><DocumentOutline /></NIcon> 最近提交</h3>
        <div v-for="s in recentSubmissions" :key="s.id" class="row">
          <span class="row-title">{{ s.studentName || '学生' }}</span>
          <span class="row-sub">提交了 {{ s.taskTitle }}</span>
          <NTag size="tiny" :type="s.status === 'submitted' ? 'warning' : 'success'" :bordered="false">{{ s.status === 'submitted' ? '待评分' : '已评分' }}</NTag>
          <NButton v-if="s.status === 'submitted'" size="tiny" text type="primary" class="row-action" @click="goGrading(s.taskId)"><template #icon><NIcon :size="12"><ArrowForwardOutline /></NIcon></template>去评分</NButton>
        </div>
      </div>

      <NEmpty v-if="!loading && !upcomingTasks.length && !recentSubmissions.length" description="暂无数据。创建课程并添加课堂任务后，这里会显示概览。">
        <template #extra><NButton size="small" @click="router.push('/teacher/courses')">去创建课程</NButton></template>
      </NEmpty>
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.stat-grid { margin-bottom: 28px; }
.stat-card { text-align: center; padding: 24px 16px; }
.interactive { cursor: pointer; }
.interactive:focus-visible { outline: 2px solid var(--n-primary-color); outline-offset: 2px; }
.stat-num { font-size: 34px; font-weight: 700; }
.stat-label { font-size: 13px; color: var(--n-text-color-3); margin-top: 4px; }
.pending .stat-num { color: #F97316; }
.upcoming .stat-num { color: #7C3AED; }
.info .stat-num { color: #2196F3; }
.section { margin-bottom: 24px; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; }
.row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 6px; }
.row:hover { background: var(--n-color-embedded); }
.row-title { font-size: 14px; font-weight: 500; }
.row-sub { font-size: 13px; color: var(--n-text-color-2); }
.row-meta { font-size: 12px; color: var(--n-text-color-3); margin-left: auto; }
.row-action { margin-left: auto; }
@media (max-width: 640px) {
  .stat-card { padding: 14px 16px; }
  .stat-num { font-size: 28px; }
  .row { align-items: flex-start; flex-wrap: wrap; }
  .row-action { min-height: 44px; margin-left: auto; }
}
</style>
