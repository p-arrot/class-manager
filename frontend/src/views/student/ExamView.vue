<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { NAlert, NButton, NEmpty, NInput, NModal, NSelect, NSpace, NSpin, NTag, useMessage } from 'naive-ui'
import PageHeader from '@/components/PageHeader.vue'
import WorksheetRenderer from '@/components/WorksheetRenderer.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { getMyExamSubmission, listExams, saveExamDraft, startExam as startExamApi, submitExam as submitExamApi } from '@/api/exams'
import { parseTaskSchema } from '@/types/taskSchema'
import type { ExamSubmissionVO, ExamVO, SemesterVO } from '@/types/api'
import type { WorksheetAnswerMap } from '@/types/taskSchema'

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const exams = ref<ExamVO[]>([])
const loading = ref(false)
const showExam = ref(false)
const activeExam = ref<ExamVO | null>(null)
const activeSubmission = ref<ExamSubmissionVO | null>(null)
const answers = ref('')
const worksheetAnswer = ref<WorksheetAnswerMap>({})
const saveState = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')
const dirty = ref(false)
const now = ref(Date.now())
let saveTimer: ReturnType<typeof setTimeout> | null = null
const clock = window.setInterval(() => { now.value = Date.now() }, 1000)

const courseOptions = computed(() => courses.value.map(course => ({ label: course.name, value: course.id })))
const semesterOptions = computed(() => semesters.value.map(semester => ({ label: semester.name, value: semester.id })))
const schema = computed(() => parseTaskSchema(activeExam.value?.paperContent))
const readOnly = computed(() => Boolean(activeSubmission.value && ['graded', 'absent', 'special'].includes(activeSubmission.value.status)))
const remainingSeconds = computed(() => activeExam.value ? Math.max(0, Math.floor((new Date(activeExam.value.endTime).getTime() - now.value) / 1000)) : 0)
const countdown = computed(() => {
  const value = remainingSeconds.value
  const hours = Math.floor(value / 3600)
  const minutes = Math.floor(value % 3600 / 60)
  const seconds = value % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

watch(courses, list => {
  if (!activeCourseId.value && list.length) activeCourseId.value = list[0].id
}, { immediate: true })
watch(activeCourseId, async cid => {
  if (!cid) return
  await loadSemesters(cid)
  activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
})
watch(activeSemesterId, async sid => { if (sid) await loadExams(sid) })
watch([answers, worksheetAnswer], () => {
  if (!showExam.value || readOnly.value) return
  dirty.value = true
  scheduleSave()
}, { deep: true })

async function loadExams(semesterId: number) {
  loading.value = true
  try { exams.value = await listExams(semesterId) }
  catch (error) { exams.value = []; message.error(getErrorMessage(error, '加载考试列表失败')) }
  finally { loading.value = false }
}

function pickCurrentSemester(list: SemesterVO[]) {
  const time = Date.now()
  return list.find(item => new Date(item.startTime).getTime() <= time && time <= new Date(item.endTime).getTime())
}

function examStatus(exam: ExamVO) {
  const start = new Date(exam.startTime).getTime()
  const end = new Date(exam.endTime).getTime()
  if (exam.submissionStatus === 'graded') return { label: '已批改', type: 'success' as const }
  if (exam.submissionStatus === 'absent') return { label: '缺考', type: 'error' as const }
  if (exam.submissionStatus === 'returned') return { label: '已退回', type: 'error' as const }
  if (exam.submissionStatus === 'submitted') return { label: '已提交', type: 'info' as const }
  if (exam.submissionStatus === 'in_progress') return { label: '答题中', type: 'warning' as const }
  if (now.value < start) return { label: '未开始', type: 'default' as const }
  if (now.value > end) return { label: '已结束', type: 'default' as const }
  return { label: '可进入', type: 'warning' as const }
}

function actionLabel(exam: ExamVO) {
  const status = exam.submissionStatus
  if (status === 'graded' || status === 'absent') return '查看结果'
  if (status === 'submitted') return '修改提交'
  if (status === 'in_progress' || status === 'returned') return '继续答题'
  return '进入考试'
}

function canOpen(exam: ExamVO) {
  const locked = ['graded', 'absent'].includes(exam.submissionStatus || '')
  if (locked) return true
  return now.value >= new Date(exam.startTime).getTime() && now.value <= new Date(exam.endTime).getTime()
}

async function openExam(exam: ExamVO) {
  try {
    activeExam.value = exam
    const locked = ['graded', 'absent'].includes(exam.submissionStatus || '')
    activeSubmission.value = locked ? await getMyExamSubmission(exam.id) : await startExamApi(exam.id)
    restoreAnswers(activeSubmission.value?.answers)
    dirty.value = false
    saveState.value = 'saved'
    showExam.value = true
  } catch (error) { message.error(getErrorMessage(error, '无法进入考试')) }
}

function restoreAnswers(content?: string | null) {
  answers.value = content || ''
  worksheetAnswer.value = {}
  if (schema.value.questions?.length && content) {
    try { worksheetAnswer.value = JSON.parse(content) as WorksheetAnswerMap } catch { worksheetAnswer.value = {} }
  }
}

function currentContent() {
  return schema.value.questions?.length ? JSON.stringify(worksheetAnswer.value) : answers.value
}

function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveState.value = 'idle'
  saveTimer = setTimeout(saveDraft, 1000)
}

async function saveDraft() {
  if (!activeExam.value || readOnly.value || !showExam.value) return
  saveState.value = 'saving'
  try {
    activeSubmission.value = await saveExamDraft(activeExam.value.id, { answers: currentContent() })
    dirty.value = false
    saveState.value = 'saved'
  } catch (error) {
    saveState.value = 'error'
    message.error(getErrorMessage(error, '草稿保存失败'))
  }
}

async function submitExam() {
  if (!activeExam.value || readOnly.value) return
  if (!window.confirm('提交后教师即可开始批改，确认提交吗？')) return
  try {
    if (saveTimer) clearTimeout(saveTimer)
    await submitExamApi(activeExam.value.id, { answers: currentContent() })
    message.success('考试已提交')
    dirty.value = false
    showExam.value = false
    if (activeSemesterId.value) await loadExams(activeSemesterId.value)
  } catch (error) { message.error(getErrorMessage(error, '提交失败')) }
}

function closeExam() {
  if ((dirty.value || saveState.value === 'saving') && !window.confirm('草稿尚未保存完成，确认离开吗？')) return
  showExam.value = false
}

onUnmounted(() => {
  window.clearInterval(clock)
  if (saveTimer) clearTimeout(saveTimer)
})
</script>

<template>
  <div class="page">
    <PageHeader title="考试" subtitle="查看考试状态并参加学期考试" />
    <NSpin :show="ctxLoading || loading">
      <div class="filters">
        <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="filter-select" />
        <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="filter-select" />
      </div>
      <div v-if="exams.length" class="exam-list">
        <article v-for="exam in exams" :key="exam.id" class="exam-row">
          <div class="exam-info">
            <div class="exam-title"><strong>{{ exam.name }}</strong><NTag size="small" :bordered="false" :type="examStatus(exam).type">{{ examStatus(exam).label }}</NTag></div>
            <span>{{ formatDate(exam.startTime, 'datetime') }} - {{ formatDate(exam.endTime, 'datetime') }}</span>
            <NAlert v-if="exam.submissionStatus === 'returned'" type="warning" :bordered="false">退回原因：{{ exam.returnReason }}</NAlert>
            <span v-if="exam.score != null" class="result">成绩 {{ exam.score }} 分</span>
          </div>
          <NButton :disabled="!canOpen(exam)" @click="openExam(exam)">{{ actionLabel(exam) }}</NButton>
        </article>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    </NSpin>

    <NModal :show="showExam" :title="activeExam?.name" preset="card" class="exam-modal" :mask-closable="false" @update:show="value => { if (!value) closeExam() }">
      <NAlert v-if="readOnly" :type="activeSubmission?.status === 'graded' ? 'success' : 'warning'" :bordered="false" class="result-summary">
        {{ activeSubmission?.status === 'graded' ? `已批改${activeSubmission.score != null ? ` · ${activeSubmission.score} 分` : ''}` : activeSubmission?.status === 'absent' ? '缺考' : '特殊处理' }}
      </NAlert>
      <div v-else class="exam-toolbar"><NTag :bordered="false" :type="remainingSeconds < 300 ? 'error' : 'info'">剩余 {{ countdown }}</NTag><span class="save-state">{{ saveState === 'saving' ? '正在保存...' : saveState === 'saved' ? '草稿已保存' : saveState === 'error' ? '保存失败' : '有未保存修改' }}</span></div>
      <NAlert v-if="activeSubmission?.status === 'returned'" type="warning" :bordered="false">教师退回：{{ activeSubmission.returnReason }}</NAlert>
      <WorksheetRenderer v-if="schema.questions?.length" :schema="activeExam?.paperContent || ''" v-model="worksheetAnswer" :readonly="readOnly" />
      <NInput v-else v-model:value="answers" type="textarea" placeholder="请输入答案" :readonly="readOnly" :autosize="{ minRows: 8, maxRows: 20 }" />
      <template #footer><NSpace justify="space-between"><NButton @click="closeExam">关闭</NButton><NButton v-if="!readOnly" type="primary" :disabled="remainingSeconds <= 0" @click="submitExam">正式提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 820px; margin: 0 auto; }
.filters { display: flex; gap: 12px; margin: 16px 0 24px; flex-wrap: wrap; }
.filter-select { width: 220px; }
.exam-list { display: grid; gap: 12px; }
.exam-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.exam-info { display: grid; gap: 6px; min-width: 0; }
.exam-info > span { color: var(--n-text-color-3); font-size: 12px; }
.exam-title { display: flex; align-items: center; gap: 8px; }
.result { color: var(--n-primary-color) !important; font-weight: 700; }
.exam-modal { width: min(760px, calc(100vw - 32px)); }
.exam-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.result-summary { margin-bottom: 12px; }
.save-state { color: var(--n-text-color-3); font-size: 12px; }
@media (max-width: 640px) {
  .filter-select { width: 100%; }
  .exam-row { align-items: stretch; flex-direction: column; }
  .exam-row :deep(.n-button) { min-height: 44px; }
  .exam-toolbar { align-items: flex-start; flex-direction: column; gap: 6px; }
}
</style>
