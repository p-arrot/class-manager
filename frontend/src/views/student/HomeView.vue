<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NGrid, NGi, NEmpty, NIcon, NTag, NSpin, useMessage } from 'naive-ui'
import { ArrowForwardOutline, TimeOutline, CheckmarkCircleOutline } from '@vicons/ionicons5'
import { listCourses } from '@/api/courses'
import CourseCard from '@/components/CourseCard.vue'
import PageHeader from '@/components/PageHeader.vue'
import http from '@/api/request'
import type { CourseVO, CoursePageQuery } from '@/types/api'

const router = useRouter(); const message = useMessage()
const loading = ref(false)
const records = ref<CourseVO[]>([])
const total = ref(0)
const query = reactive<CoursePageQuery>({ page: 1, size: 12 })
const dueTasks = ref<any[]>([])
const recentGrades = ref<any[]>([])

async function loadDueAndGrades(courses: CourseVO[]) {
  const due: any[] = []; const graded: any[] = []
  for (const course of courses.slice(0, 3)) {
    try {
      const semesters: any[] = await http.get(`/courses/${course.id}/semesters`) || []
      for (const sem of (semesters || []).slice(0, 1)) {
        const lessons: any[] = await http.get(`/semesters/${sem.id}/lessons`) || []
        for (const les of (lessons || []).slice(0, 3)) {
          const tasks: any[] = await http.get(`/lessons/${les.id}/tasks`) || []
          for (const t of (tasks || [])) {
            if (t.deadline && new Date(t.deadline) > new Date()) {
              due.push({ ...t, courseName: course.name, lessonName: les.name, courseId: course.id })
            }
            try {
              const subs: any[] = await http.get(`/tasks/${t.id}/submissions`) || []
              const mine = subs.find((s: any) => s.studentId === 127) // approximate
              if (mine && mine.status === 'graded') {
                graded.push({ ...mine, taskTitle: t.title, courseName: course.name })
              }
            } catch { /* */ }
          }
        }
      }
    } catch { /* */ }
  }
  dueTasks.value = due.slice(0, 4)
  recentGrades.value = graded.slice(0, 3)
}

async function fetchData() {
  loading.value = true
  try { const r = await listCourses(query); records.value = r.records; total.value = r.total; await loadDueAndGrades(r.records) }
  catch (e: any) { message.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function goDetail(id: number) { router.push(`/student/courses/${id}`) }
function goTask(taskId: number) { router.push(`/student/tasks/${taskId}`) }

onMounted(fetchData)
</script>

<template>
  <div class="page">
    <PageHeader title="首页" />

    <NSpin :show="loading">
      <!-- Due soon -->
      <div v-if="dueTasks.length" class="section">
        <h3 class="section-title"><NIcon :size="16"><TimeOutline /></NIcon> 即将截止</h3>
        <div v-for="t in dueTasks" :key="'d'+t.id" class="row" @click="goTask(t.id)" style="cursor:pointer">
          <NTag size="tiny" :bordered="false" :type="t.type==='worksheet'?'info':'warning'">{{ t.type==='worksheet'?'学习单':'课堂作品' }}</NTag>
          <span class="row-title">{{ t.title }}</span>
          <span class="row-sub">{{ t.courseName }}</span>
          <span class="row-deadline">截止 {{ t.deadline?.split('T')[0] }}</span>
          <NIcon :size="14"><ArrowForwardOutline /></NIcon>
        </div>
      </div>

      <!-- Recently graded -->
      <div v-if="recentGrades.length" class="section">
        <h3 class="section-title"><NIcon :size="16"><CheckmarkCircleOutline /></NIcon> 最近评分</h3>
        <div v-for="g in recentGrades" :key="'g'+g.id" class="row">
          <span class="row-title">{{ g.taskTitle }}</span>
          <span class="row-sub">{{ g.courseName }}</span>
          <NTag size="tiny" type="success" :bordered="false">已评分</NTag>
        </div>
      </div>

      <!-- Course grid -->
      <h3 class="section-title" style="margin-top:24px">我的课程</h3>
      <div v-if="records.length" class="course-grid">
        <NGrid :cols="3" :x-gap="16" :y-gap="16" responsive="screen">
          <NGi v-for="c in records" :key="c.id">
            <CourseCard :course="c" @enter="goDetail">
              <template #actions="{ course }">
                <NButton size="tiny" quaternary @click="goDetail(course.id)">
                  <template #icon><NIcon :size="14"><ArrowForwardOutline /></NIcon></template>进入课程
                </NButton>
              </template>
            </CourseCard>
          </NGi>
        </NGrid>
      </div>
      <NEmpty v-if="!records.length && !loading" description="暂无课程。你所在的班级尚未被分配任何课程，请联系教师。" class="empty-wrap" />
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.section { margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 6px; }
.row:hover { background: var(--n-color-embedded); }
.row-title { font-size: 14px; font-weight: 500; }
.row-sub { font-size: 13px; color: var(--n-text-color-2); }
.row-deadline { font-size: 12px; color: var(--n-text-color-3); margin-left: auto; }
.course-grid { display: flex; flex-direction: column; gap: 16px; }
.empty-wrap { padding: 40px 0; }
</style>
