<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import PageHeader from '@/components/PageHeader.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { listLessons } from '@/api/lessons'
import { getMyTaskSubmission, listTasks } from '@/api/tasks'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { SemesterVO, SubmissionVO, TaskVO } from '@/types/api'

interface TaskRow extends TaskVO {
  lessonName: string
  submission: SubmissionVO | null
}

const router = useRouter()
const message = useMessage()
const { courses, semesters, loading: contextLoading, loadSemesters } = useStudentContext()
const courseId = ref<number | null>(null)
const semesterId = ref<number | null>(null)
const rows = ref<TaskRow[]>([])
const loading = ref(false)
const courseOptions = computed(() => courses.value.map(item => ({ label: item.name, value: item.id })))
const semesterOptions = computed(() => semesters.value.map(item => ({ label: item.name, value: item.id })))

watch(courses, list => { if (!courseId.value && list.length) courseId.value = list[0].id }, { immediate: true })
watch(courseId, async value => {
  if (!value) return
  await loadSemesters(value)
  semesterId.value = currentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
})
watch(semesterId, loadRows)

function currentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(item => new Date(item.startTime).getTime() <= now && now <= new Date(item.endTime).getTime())
}

async function loadRows(value: number | null) {
  if (!value) { rows.value = []; return }
  loading.value = true
  try {
    const lessons = await listLessons(value)
    const taskGroups = await Promise.all(lessons.map(async lesson => ({ lesson, tasks: await listTasks(lesson.id) })))
    const tasks = taskGroups.flatMap(group => group.tasks.map(task => ({ task, lessonName: group.lesson.name })))
    rows.value = await Promise.all(tasks.map(async item => ({ ...item.task, lessonName: item.lessonName, submission: await getMyTaskSubmission(item.task.id) })))
  } catch (error) {
    rows.value = []
    message.error(getErrorMessage(error, '加载课堂任务失败'))
  } finally { loading.value = false }
}

function statusLabel(row: TaskRow) {
  if (!row.submission) return '未提交'
  if (row.submission.status === 'graded') return '已批改'
  if (row.submission.status === 'returned') return '已退回'
  if (row.submission.status === 'special') return '特殊处理'
  return '待批改'
}

function statusType(row: TaskRow) {
  if (row.submission?.status === 'graded') return 'success'
  if (row.submission?.status === 'returned' || row.submission?.status === 'special') return 'error'
  if (row.submission?.status === 'submitted') return 'warning'
  return 'default'
}

function openTask(row: TaskRow) {
  const result = row.submission && ['graded', 'special'].includes(row.submission.status)
  router.push(result ? `/student/tasks/${row.id}/result` : `/student/tasks/${row.id}`)
}
</script>

<template>
  <div class="page">
    <PageHeader title="课堂任务" subtitle="集中查看各课程需要完成的学习任务" />
    <div class="filters"><NSelect v-model:value="courseId" :options="courseOptions" placeholder="选择课程" /><NSelect v-model:value="semesterId" :options="semesterOptions" placeholder="选择学期" /></div>
    <NSpin :show="contextLoading || loading">
      <div v-if="rows.length" class="task-list">
        <article v-for="row in rows" :key="row.id" class="task-row">
          <div class="task-main"><div><strong>{{ row.title }}</strong><NTag size="small" :bordered="false" :type="statusType(row)">{{ statusLabel(row) }}</NTag></div><span>{{ row.lessonName }} · {{ row.deadline ? `截止 ${formatDate(row.deadline, 'datetime')}` : '无截止时间' }}</span><p v-if="row.submission?.status === 'returned'">退回原因：{{ row.submission.returnReason }}</p></div>
          <NButton @click="openTask(row)">{{ row.submission?.status === 'graded' ? '查看批改' : row.submission ? '查看或修改' : '开始作答' }}</NButton>
        </article>
      </div>
      <NEmpty v-else-if="semesterId" description="本学期暂无课堂任务" />
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.filters { display: grid; grid-template-columns: repeat(2, minmax(0, 220px)); gap: 12px; margin: 16px 0 24px; }
.task-list { display: grid; gap: 10px; }
.task-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 14px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.task-main { display: grid; gap: 5px; min-width: 0; }
.task-main > div { display: flex; align-items: center; gap: 8px; }
.task-main span { color: var(--n-text-color-3); font-size: 12px; }
.task-main p { margin: 0; color: var(--n-warning-color); font-size: 13px; }
@media (max-width: 640px) { .filters { grid-template-columns: 1fr; } .task-row { align-items: stretch; flex-direction: column; } }
</style>
