<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NButton, NDataTable, NTag, NModal, NSpace, NInput, useMessage } from 'naive-ui'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'

interface Exam { id: number; name: string; startTime: string; endTime: string; weight: number }
interface Submission { id: number; status: string; score: number | null }

const message = useMessage()
const exams = ref<Exam[]>([])
const mySubs = ref<Record<number, Submission>>({})
const showExam = ref(false)
const activeExam = ref<Exam | null>(null)
const answers = ref('')

async function loadData() {
  try {
    const courses: any[] = await http.get('/courses?page=1&size=50')
    if (!courses.length) return
    const semesters: any[] = await http.get(`/courses/${courses[0].id}/semesters`)
    if (!semesters.length) return
    exams.value = await http.get(`/semesters/${semesters[0].id}/exams`)
  } catch { /* ignore */ }
}

function startExam(exam: Exam) {
  activeExam.value = exam
  answers.value = ''
  showExam.value = true
}

async function submitExam() {
  if (!activeExam.value) return
  try {
    await http.post(`/exams/${activeExam.value.id}/submit`, { answers: answers.value })
    message.success('提交成功')
    showExam.value = false
    await loadData()
  } catch (e: any) { message.error(e.message || '提交失败') }
}

function statusLabel(s: string) { return s === 'submitted' ? '已提交' : s === 'graded' ? '已评分' : s }
function statusColor(s: string) { return s === 'submitted' ? '#4CAF50' : s === 'graded' ? '#2196F3' : '#999' }

onMounted(loadData)
</script>

<template>
  <div class="page">
    <PageHeader title="考试" subtitle="参加学期考试" />
    <div v-if="exams.length" class="exam-list">
      <div v-for="e in exams" :key="e.id" class="exam-row">
        <div class="exam-info">
          <span class="exam-name">{{ e.name }}</span>
          <span class="exam-time">{{ formatDate(e.startTime, 'datetime') }} - {{ formatDate(e.endTime, 'datetime') }}</span>
        </div>
        <NButton size="small" @click="startExam(e)">进入考试</NButton>
      </div>
    </div>
    <NEmpty v-else description="暂无考试" />
    <NModal v-model:show="showExam" :title="activeExam?.name" preset="card" style="width:640px;max-height:80vh">
      <NInput v-model:value="answers" type="textarea" placeholder="请输入答案" :autosize="{ minRows: 8, maxRows: 20 }" />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showExam = false">取消</NButton>
          <NButton type="primary" @click="submitExam">提交</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.exam-list { display: flex; flex-direction: column; gap: 12px; margin-top: 24px; }
.exam-row { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 10px; }
.exam-info { display: flex; flex-direction: column; gap: 4px; }
.exam-name { font-size: 15px; font-weight: 600; }
.exam-time { font-size: 12px; color: var(--n-text-color-3); }
</style>
