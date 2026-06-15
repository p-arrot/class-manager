<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NEmpty, NButton, NSelect, NModal, NSpace, NInput, NSpin, useMessage } from 'naive-ui'
import PageHeader from '@/components/PageHeader.vue'
import WorksheetRenderer from '@/components/WorksheetRenderer.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { listExams, submitExam as submitExamApi } from '@/api/exams'
import { parseTaskSchema } from '@/types/taskSchema'
import type { ExamVO, SemesterVO } from '@/types/api'
import type { WorksheetAnswerMap } from '@/types/taskSchema'

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const exams = ref<ExamVO[]>([])
const loading = ref(false)
const showExam = ref(false)
const activeExam = ref<ExamVO | null>(null)
const answers = ref('')
const worksheetAnswer = ref<WorksheetAnswerMap>({})
const courseOptions = computed(() => courses.value.map(course => ({
  label: course.name,
  value: course.id,
})))
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

watch(activeCourseId, async (cid) => {
  if (cid) {
    await loadSemesters(cid)
    activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
  }
})
watch(activeSemesterId, async (sid) => { if (sid) { await loadExams(sid) } })

async function loadExams(semesterId: number) {
  loading.value = true
  try {
    exams.value = await listExams(semesterId)
  } catch (e) {
    exams.value = []
    message.error(getErrorMessage(e, '加载考试列表失败'))
  } finally {
    loading.value = false
  }
}

function startExam(exam: ExamVO) {
  activeExam.value = exam
  answers.value = ''
  worksheetAnswer.value = {}
  showExam.value = true
}

function pickCurrentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(s => new Date(s.startTime).getTime() <= now && now <= new Date(s.endTime).getTime())
}

async function submitExam() {
  if (!activeExam.value) return
  try {
    const schema = parseTaskSchema(activeExam.value.paperContent)
    const content = schema.questions?.length ? JSON.stringify(worksheetAnswer.value) : answers.value
    await submitExamApi(activeExam.value.id, { answers: content })
    message.success('提交成功')
    showExam.value = false
    if (activeSemesterId.value) await loadExams(activeSemesterId.value)
  } catch (e) {
    message.error(getErrorMessage(e, '提交失败'))
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="考试" subtitle="参加学期考试" />
    <NSpin :show="ctxLoading">
      <div class="filters">
        <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="filter-select" />
        <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="filter-select" />
      </div>
      <div v-if="exams.length" class="exam-list">
        <div v-for="e in exams" :key="e.id" class="exam-row">
          <div class="exam-info">
            <span class="exam-name">{{ e.name }}</span>
            <span class="exam-time">{{ formatDate(e.startTime, 'datetime') }} - {{ formatDate(e.endTime, 'datetime') }}</span>
          </div>
          <NButton size="small" @click="startExam(e)">进入考试</NButton>
        </div>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    </NSpin>
    <NModal v-model:show="showExam" :title="activeExam?.name" preset="card" class="exam-modal">
      <WorksheetRenderer v-if="parseTaskSchema(activeExam?.paperContent).questions?.length" :schema="activeExam?.paperContent || ''" v-model="worksheetAnswer" />
      <NInput v-else v-model:value="answers" type="textarea" placeholder="请输入答案" :autosize="{ minRows: 8, maxRows: 20 }" />
      <template #footer><NSpace justify="end"><NButton @click="showExam = false">取消</NButton><NButton type="primary" @click="submitExam">提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.filters { display: flex; gap: 12px; margin: 16px 0 24px; flex-wrap: wrap; }
.filter-select { width: 200px; }
.exam-list { display: flex; flex-direction: column; gap: 12px; }
.exam-row { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 10px; }
.exam-info { display: flex; flex-direction: column; gap: 4px; }
.exam-name { font-size: 15px; font-weight: 600; }
.exam-time { font-size: 12px; color: var(--n-text-color-3); }
.exam-modal { width: 640px; }
@media (max-width: 640px) {
  .filter-select { width: 100%; }
  .exam-row { align-items: stretch; flex-direction: column; gap: 12px; }
}
</style>
