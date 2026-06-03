<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NGrid, NGi, NTag, NButton, NSpin, NEmpty, NIcon } from 'naive-ui'
import { ArrowForwardOutline, TimeOutline, DocumentOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const loading = ref(false)
const stats = ref({ pendingGrading: 0, upcomingDeadlines: 0, recentCount: 0 })
const recentSubmissions = ref<any[]>([])
const upcomingTasks = ref<any[]>([])
const showPending = ref(false)
const showUpcoming = ref(false)

async function loadDashboard() {
  loading.value = true
  try {
    const courses: any = await http.get('/courses?page=1&size=50')
    if (!courses?.records?.length) { loading.value = false; return }
    const cid = courses.records[0].id
    const semesters: any[] = await http.get(`/courses/${cid}/semesters`) || []
    let pending = 0; let upcoming = 0
    const allTasks: any[] = []; const allSubs: any[] = []
    for (const sem of (semesters || [])) {
      try {
        const lessons: any[] = await http.get(`/semesters/${sem.id}/lessons`) || []
        for (const les of (lessons || [])) {
          try {
            const tasks: any[] = await http.get(`/lessons/${les.id}/tasks`) || []
            for (const t of (tasks || [])) {
              allTasks.push({ ...t, semesterName: sem.name, lessonName: les.name })
              if (t.deadline && new Date(t.deadline) > new Date()) upcoming++
              try {
                const subs: any[] = await http.get(`/tasks/${t.id}/submissions`) || []
                for (const s of (subs || [])) {
                  if (s.status === 'submitted') pending++
                  allSubs.push({ ...s, taskTitle: t.title, taskId: t.id, semesterName: sem.name })
                }
              } catch (e) { console.error('加载提交记录失败', e) }
            }
          } catch (e) { console.error('加载任务列表失败', e) }
        }
      } catch (e) { console.error('加载课时列表失败', e) }
    }
    stats.value = { pendingGrading: pending, upcomingDeadlines: upcoming, recentCount: allSubs.length }
    // Keep all pending submissions and upcoming tasks (not just recent slices)
    recentSubmissions.value = allSubs.slice().reverse()
    upcomingTasks.value = allTasks.filter(t => t.deadline && new Date(t.deadline) > new Date())
  } catch (e) { console.error('加载工作台数据失败', e) }
  finally { loading.value = false }
}

function goGrading(taskId: number) { router.push(`/teacher/grading/${taskId}`) }

onMounted(loadDashboard)
</script>

<template>
  <div class="page">
    <PageHeader title="工作台" subtitle="今日概览" />
    <NSpin :show="loading">
      <NGrid :cols="3" :x-gap="16" :y-gap="16" class="stat-grid">
        <NGi><NCard size="small" class="stat-card pending" hoverable @click="showPending = !showPending"><div class="stat-num">{{ stats.pendingGrading }}</div><div class="stat-label">{{ showPending ? '待评分 ↑' : '待评分 ↓' }}</div></NCard></NGi>
        <NGi><NCard size="small" class="stat-card upcoming" hoverable @click="showUpcoming = !showUpcoming"><div class="stat-num">{{ stats.upcomingDeadlines }}</div><div class="stat-label">{{ showUpcoming ? '即将截止 ↑' : '即将截止 ↓' }}</div></NCard></NGi>
        <NGi><NCard size="small" class="stat-card info"><div class="stat-num">{{ stats.recentCount }}</div><div class="stat-label">近期提交</div></NCard></NGi>
      </NGrid>

      <!-- Expandable: 待评分 -->
      <div v-if="showPending && recentSubmissions.filter(s=>s.status==='submitted').length" class="section">
        <h3 class="section-title">待评分</h3>
        <div v-for="s in recentSubmissions.filter(s=>s.status==='submitted')" :key="'p'+s.id" class="row">
          <span class="row-title">{{ s.studentName || '学生' }}</span>
          <span class="row-sub">提交了 {{ s.taskTitle }}</span>
          <NButton size="tiny" type="primary" @click="goGrading(s.taskId)" style="margin-left:auto">去评分</NButton>
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
          <NButton v-if="s.status === 'submitted'" size="tiny" text type="primary" @click="goGrading(s.taskId)" style="margin-left:auto"><template #icon><NIcon :size="12"><ArrowForwardOutline /></NIcon></template>去评分</NButton>
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
.stat-num { font-size: 34px; font-weight: 700; letter-spacing: -0.02em; }
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
</style>
