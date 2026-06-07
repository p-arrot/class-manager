<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NEmpty, NIcon, NModal, NSpace, NSpin, NTag, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  PersonOutline,
} from '@vicons/ionicons5'
import { getLesson } from '@/api/lessons'
import { getDrivePreview, getDriveRaw } from '@/api/drive'
import { evaluateSubmission, getTask, listSubmissions } from '@/api/tasks'
import PageHeader from '@/components/PageHeader.vue'
import ArtifactSubmissionPanel from '@/components/grading/ArtifactSubmissionPanel.vue'
import WorksheetSubmissionPanel from '@/components/grading/WorksheetSubmissionPanel.vue'
import StudentProfileModal from '@/components/StudentProfileModal.vue'
import { getErrorMessage } from '@/utils/error'
import { normalizeDimensionScores, parseTaskSchema } from '@/types/taskSchema'
import { isArtifactFile, isRecord } from '@/types/grading'
import type { SubmissionVO, TaskDetailVO } from '@/types/api'
import type { ArtifactFile, ParsedSubmissionContent } from '@/types/grading'
import type { TaskQuestion } from '@/types/taskSchema'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const taskId = Number(route.params.taskId)

const loading = ref(false)
const submissions = ref<SubmissionVO[]>([])
const task = ref<TaskDetailVO | null>(null)
const currentIdx = ref(0)
const questionScores = ref<Record<number, Record<string, Record<string, number>>>>({})
const semesterId = ref<number | null>(null)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')
const previewUrl = ref('')
const previewTitle = ref('')
const previewLoading = ref(false)

const current = computed<SubmissionVO | null>(() => submissions.value[currentIdx.value] || null)
const schema = computed(() => parseTaskSchema(task.value?.formSchema))
const parsedContent = computed<ParsedSubmissionContent>(() => {
  if (!current.value?.content) return {}
  try {
    const parsed = JSON.parse(current.value.content) as unknown
    return isRecord(parsed) ? parsed as ParsedSubmissionContent : { raw: current.value.content }
  } catch {
    return { raw: current.value.content }
  }
})
const artifactFiles = computed(() => {
  const files = parsedContent.value.files
  return Array.isArray(files) ? files.filter(isArtifactFile) : []
})

async function loadSubmissions() {
  loading.value = true
  try {
    submissions.value = await listSubmissions(taskId) || []
    task.value = await getTask(taskId)
    if (task.value?.lessonId) {
      const lesson = await getLesson(task.value.lessonId)
      if (lesson?.semesterId) semesterId.value = lesson.semesterId
    }
  } catch (e) {
    message.error(getErrorMessage(e, '加载提交失败'))
  } finally {
    loading.value = false
  }
}

function openProfile(studentId: number, name: string) {
  profileStudentId.value = studentId
  profileStudentName.value = name
}

async function submitGrade() {
  const sub = current.value
  if (!sub) return
  const scoreRows = buildQuestionScoreRows(sub.id)
  if (!scoreRows.length) {
    message.warning('请至少填写一个题目维度得分')
    return
  }
  try {
    await evaluateSubmission(sub.id, { dimensions: [], questionScores: scoreRows })
    message.success('评分成功')
    sub.status = 'graded'
    if (currentIdx.value < submissions.value.length - 1) currentIdx.value++
  } catch (e) {
    message.error(getErrorMessage(e, '评分失败'))
  }
}

async function markSpecial() {
  const sub = current.value
  if (!sub) return
  try {
    await evaluateSubmission(sub.id, { isSpecial: true, dimensions: [] })
    sub.status = 'special'
    message.success('已标记特殊情况')
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

async function unmarkSpecial() {
  const sub = current.value
  if (!sub) return
  try {
    await evaluateSubmission(sub.id, { isSpecial: false, dimensions: [] })
    sub.status = 'submitted'
    message.success('已取消标记')
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

function getQuestionScore(subId: number, questionId: string, dimension: string) {
  const existing = questionScores.value[subId]?.[questionId]?.[dimension]
  if (typeof existing === 'number') return existing
  const question = schema.value.questions?.find(item => item.id === questionId)
  return question?.autoGrade && isCorrect(question) ? normalizeDimensionScores(question.dimensionScores).find(item => item.dimension === dimension)?.maxScore ?? 0 : 0
}

function setQuestionScore(subId: number, questionId: string, dimension: string, value: number | null) {
  if (!questionScores.value[subId]) questionScores.value[subId] = {}
  if (!questionScores.value[subId][questionId]) questionScores.value[subId][questionId] = {}
  questionScores.value[subId][questionId][dimension] = Math.max(0, Number(value ?? 0))
}

function buildQuestionScoreRows(subId: number) {
  return (schema.value.questions ?? []).flatMap(question => {
    const dims = normalizeDimensionScores(question.dimensionScores).filter(item => item.maxScore > 0)
    return dims.map(dim => ({
      questionId: question.id,
      dimension: dim.dimension,
      earnedScore: getQuestionScore(subId, question.id, dim.dimension),
      maxScore: dim.maxScore,
      autoGraded: Boolean(question.autoGrade),
    }))
  })
}

function isCorrect(question: TaskQuestion) {
  const expected = question.answer
  const actual = parsedContent.value[question.id]
  if (Array.isArray(expected)) {
    if (!Array.isArray(actual)) return false
    return [...expected].map(String).sort().join('|') === [...actual].map(String).sort().join('|')
  }
  return String(expected) === String(actual)
}

async function previewFile(file: ArtifactFile) {
  previewLoading.value = true
  previewTitle.value = file.name
  previewUrl.value = ''
  try {
    const data = await getDrivePreview(file.id)
    previewUrl.value = data.url
  } catch (e) {
    message.error(getErrorMessage(e, '预览失败'))
  } finally {
    previewLoading.value = false
  }
}

async function downloadFile(file: ArtifactFile) {
  try {
    const blob = await getDriveRaw(file.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.name
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
}

onMounted(loadSubmissions)
</script>

<template>
  <div class="page">
    <NButton text @click="router.back()">
      <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
      返回
    </NButton>
    <PageHeader title="批量评分" :subtitle="`${task?.title || '任务'} · ${submissions.length} 份提交 · ${currentIdx + 1}/${submissions.length || 1}`" />

    <NSpin :show="loading">
      <div v-if="current" class="grading-area">
        <div class="student-bar">
          <span class="student-name">{{ current.studentName || '学生' }}</span>
          <span class="student-no">{{ current.studentNo }}</span>
          <NTag size="small" :type="current.status === 'submitted' ? 'warning' : current.status === 'graded' ? 'success' : 'default'" :bordered="false">
            {{ current.status === 'submitted' ? '待评分' : current.status === 'graded' ? '已评分' : current.status }}
          </NTag>
          <NButton size="tiny" quaternary class="profile-btn" aria-label="查看学生档案" @click="openProfile(current.studentId, current.studentName || '学生')">
            <template #icon><NIcon :size="14"><PersonOutline /></NIcon></template>
            查看档案
          </NButton>
        </div>

        <div class="content-preview">
          <div class="content-label">提交内容</div>

          <WorksheetSubmissionPanel
            v-if="task?.type === 'worksheet'"
            :questions="schema.questions ?? []"
            :content="parsedContent"
            :submission-id="current.id"
            :fallback-content="current.content"
            :get-score="getQuestionScore"
            @score-change="setQuestionScore"
          />

          <ArtifactSubmissionPanel
            v-else
            :note="parsedContent.note"
            :files="artifactFiles"
            :fallback-content="current.content"
            @preview="previewFile"
            @download="downloadFile"
          />
        </div>

        <NSpace justify="center" :size="12" class="actions">
          <NButton :disabled="currentIdx === 0" @click="currentIdx--">
            <template #icon><NIcon><ChevronBackOutline /></NIcon></template>
            上一个
          </NButton>
          <NButton type="primary" @click="submitGrade">提交评分</NButton>
          <NButton v-if="current.status !== 'special'" type="warning" @click="markSpecial">特殊标记</NButton>
          <NButton v-else @click="unmarkSpecial">取消标记</NButton>
          <NButton :disabled="currentIdx >= submissions.length - 1" @click="currentIdx++">
            下一个
            <template #icon><NIcon><ChevronForwardOutline /></NIcon></template>
          </NButton>
        </NSpace>
      </div>
      <NEmpty v-else description="暂无提交需要评分">
        <template #extra><NButton size="small" @click="router.back()">返回</NButton></template>
      </NEmpty>
    </NSpin>

    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="semesterId" @close="profileStudentId = null" />
    <NModal
      :show="!!previewTitle"
      preset="card"
      :title="previewTitle"
      class="preview-modal"
      :bordered="false"
      @update:show="v => { if (!v) { previewTitle = ''; previewUrl = '' } }"
    >
      <div class="preview-body">
        <NSpin v-if="previewLoading" />
        <iframe v-else-if="previewUrl" :src="previewUrl" class="preview-frame" />
        <NEmpty v-else description="暂无预览" />
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 920px; margin: 0 auto; }
.grading-area { margin-top: 16px; }
.student-bar { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; margin-bottom: 16px; }
.student-name { font-size: 16px; font-weight: 600; }
.student-no { font-size: 13px; color: var(--n-text-color-3); }
.profile-btn { margin-left: auto; }
.content-preview { margin-bottom: 20px; }
.content-label { font-size: 13px; color: var(--n-text-color-3); margin-bottom: 6px; }
.actions { margin-top: 24px; }
.preview-modal { width: 90vw; max-width: 1100px; height: 85vh; }
.preview-body { height: calc(85vh - 90px); display: flex; align-items: stretch; justify-content: stretch; min-height: 0; }
.preview-frame { display: block; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: 0; flex: 1 1 auto; }
@media (max-width: 720px) {
  .student-bar { align-items: flex-start; flex-direction: column; }
  .profile-btn { margin-left: 0; }
}
</style>
