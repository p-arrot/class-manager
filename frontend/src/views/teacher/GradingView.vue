<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NEmpty, NIcon, NInput, NModal, NSpin, NTag, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  PersonOutline,
  SaveOutline,
} from '@vicons/ionicons5'
import { getLesson } from '@/api/lessons'
import { getDrivePreview, getDriveRaw } from '@/api/drive'
import { evaluateSubmission, getTask, listSubmissions, returnTaskSubmission } from '@/api/tasks'
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
const scoreTouched = ref<Record<number, Record<string, Record<string, boolean>>>>({})
const questionComments = ref<Record<number, Record<string, string>>>({})
const referenceAnswerVisible = ref<Record<number, Record<string, boolean>>>({})
const teacherComments = ref<Record<number, string>>({})
const validationErrors = ref<Record<string, string>>({})
const submitting = ref(false)
const specialLoading = ref(false)
const returnLoading = ref(false)
const showReturnModal = ref(false)
const returnReason = ref('')
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
const manualQuestions = computed(() => (schema.value.questions ?? []).filter(question => {
  return !question.autoGrade && normalizeDimensionScores(question.dimensionScores).some(item => item.maxScore > 0)
}))
const autoQuestions = computed(() => (schema.value.questions ?? []).filter(question => question.autoGrade))
const manualProgress = computed(() => {
  const sub = current.value
  if (!sub) return { completed: 0, total: 0 }
  const total = manualQuestions.value.length
  const completed = manualQuestions.value.filter(question => isManualQuestionComplete(sub.id, question)).length
  return { completed, total }
})
const autoProgress = computed(() => ({ completed: autoQuestions.value.length, total: autoQuestions.value.length }))
const currentTeacherComment = computed({
  get() {
    const sub = current.value
    return sub ? teacherComments.value[sub.id] ?? '' : ''
  },
  set(value: string) {
    const sub = current.value
    if (sub) teacherComments.value[sub.id] = value
  },
})

async function loadSubmissions() {
  loading.value = true
  try {
    submissions.value = await listSubmissions(taskId) || []
    const targetSubmissionId = Number(route.query.submissionId)
    if (targetSubmissionId) {
      const index = submissions.value.findIndex(item => item.id === targetSubmissionId)
      if (index >= 0) currentIdx.value = index
    }
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
  validationErrors.value = {}
  if (!validateManualScores(sub.id)) {
    message.warning('请先完成所有人工题评分')
    await scrollToFirstInvalidQuestion()
    return
  }
  const scoreRows = buildQuestionScoreRows(sub.id)
  if (!scoreRows.length) {
    message.warning('请至少填写一个题目的维度得分')
    return
  }
  submitting.value = true
  try {
    await evaluateSubmission(sub.id, {
      dimensions: [],
      questionScores: scoreRows,
      teacherComment: currentTeacherComment.value.trim(),
      questionFeedback: buildQuestionFeedbackRows(sub.id),
    })
    message.success('评分成功')
    sub.status = 'graded'
    if (currentIdx.value < submissions.value.length - 1) currentIdx.value++
  } catch (e) {
    message.error(getErrorMessage(e, '评分失败'))
  } finally {
    submitting.value = false
  }
}

async function markSpecial() {
  const sub = current.value
  if (!sub) return
  if (!currentTeacherComment.value.trim()) {
    message.warning('请先填写特殊处理原因')
    return
  }
  specialLoading.value = true
  try {
    await evaluateSubmission(sub.id, {
      isSpecial: true,
      dimensions: [],
      teacherComment: currentTeacherComment.value.trim(),
      questionFeedback: buildQuestionFeedbackRows(sub.id),
    })
    sub.status = 'special'
    message.success('已标记特殊情况')
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  } finally {
    specialLoading.value = false
  }
}

async function unmarkSpecial() {
  const sub = current.value
  if (!sub) return
  specialLoading.value = true
  try {
    await evaluateSubmission(sub.id, { isSpecial: false, dimensions: [] })
    sub.status = 'submitted'
    message.success('已取消标记')
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  } finally {
    specialLoading.value = false
  }
}

async function confirmReturn() {
  const sub = current.value
  if (!sub || !returnReason.value.trim()) return
  returnLoading.value = true
  try {
    await returnTaskSubmission(sub.id, returnReason.value.trim())
    sub.status = 'returned'
    sub.returnReason = returnReason.value.trim()
    message.success('已退回学生修改，原评分已清除')
    showReturnModal.value = false
    returnReason.value = ''
  } catch (error) {
    message.error(getErrorMessage(error, '退回失败'))
  } finally {
    returnLoading.value = false
  }
}

function getQuestionScore(subId: number, questionId: string, dimension: string) {
  const existing = questionScores.value[subId]?.[questionId]?.[dimension]
  if (typeof existing === 'number') return existing
  const question = schema.value.questions?.find(item => item.id === questionId)
  if (question?.autoGrade) {
    return isCorrect(question)
      ? normalizeDimensionScores(question.dimensionScores).find(item => item.dimension === dimension)?.maxScore ?? 0
      : 0
  }
  return null
}

function setQuestionScore(subId: number, questionId: string, dimension: string, value: number | null) {
  if (!questionScores.value[subId]) questionScores.value[subId] = {}
  if (!questionScores.value[subId][questionId]) questionScores.value[subId][questionId] = {}
  if (!scoreTouched.value[subId]) scoreTouched.value[subId] = {}
  if (!scoreTouched.value[subId][questionId]) scoreTouched.value[subId][questionId] = {}
  questionScores.value[subId][questionId][dimension] = Math.max(0, Number(value ?? 0))
  scoreTouched.value[subId][questionId][dimension] = true
  if (validationErrors.value[questionId]) delete validationErrors.value[questionId]
}

function getQuestionComment(subId: number, questionId: string) {
  return questionComments.value[subId]?.[questionId] ?? ''
}

function setQuestionComment(subId: number, questionId: string, value: string) {
  if (!questionComments.value[subId]) questionComments.value[subId] = {}
  questionComments.value[subId][questionId] = value
}

function getReferenceVisible(subId: number, questionId: string) {
  return referenceAnswerVisible.value[subId]?.[questionId] ?? false
}

function setReferenceVisible(subId: number, questionId: string, value: boolean) {
  if (!referenceAnswerVisible.value[subId]) referenceAnswerVisible.value[subId] = {}
  referenceAnswerVisible.value[subId][questionId] = value
}

function buildQuestionScoreRows(subId: number) {
  return (schema.value.questions ?? []).flatMap(question => {
    const dims = normalizeDimensionScores(question.dimensionScores).filter(item => item.maxScore > 0)
    return dims.map(dim => ({
      questionId: question.id,
      dimension: dim.dimension,
      earnedScore: getQuestionScore(subId, question.id, dim.dimension) ?? 0,
      maxScore: dim.maxScore,
      autoGraded: Boolean(question.autoGrade),
    }))
  })
}

function buildQuestionFeedbackRows(subId: number) {
  return (schema.value.questions ?? []).map(question => ({
    questionId: question.id,
    comment: getQuestionComment(subId, question.id).trim(),
    referenceAnswerVisible: getReferenceVisible(subId, question.id),
  }))
}

function isManualQuestionComplete(subId: number, question: TaskQuestion) {
  const dims = normalizeDimensionScores(question.dimensionScores).filter(item => item.maxScore > 0)
  return dims.every(dim => scoreTouched.value[subId]?.[question.id]?.[dim.dimension])
}

function validateManualScores(subId: number) {
  const errors: Record<string, string> = {}
  for (const question of manualQuestions.value) {
    if (!isManualQuestionComplete(subId, question)) {
      errors[question.id] = '人工题需要填写每个评分维度，0 分也请明确输入。'
    }
  }
  validationErrors.value = errors
  return !Object.keys(errors).length
}

async function scrollToFirstInvalidQuestion() {
  await nextTick()
  const firstId = Object.keys(validationErrors.value)[0]
  if (!firstId) return
  document.querySelector(`[data-question-id="${CSS.escape(firstId)}"]`)?.scrollIntoView({ block: 'center', behavior: 'smooth' })
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

function statusType(status: string) {
  if (status === 'graded') return 'success'
  if (status === 'special') return 'warning'
  return 'info'
}

function statusLabel(status: string) {
  if (status === 'submitted') return '待评分'
  if (status === 'graded') return '已评分'
  if (status === 'special') return '特殊处理'
  if (status === 'returned') return '已退回'
  return status
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
  <div class="page grading-page">
    <NButton text class="back-button" @click="router.back()">
      <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
      返回
    </NButton>
    <PageHeader title="学习单批改" :subtitle="`${task?.title || '任务'} · ${submissions.length} 份提交 · ${currentIdx + 1}/${submissions.length || 1}`" />

    <NSpin :show="loading">
      <div v-if="current" class="grading-area">
        <div class="student-bar">
          <div class="student-main">
            <span class="student-name">{{ current.studentName || '学生' }}</span>
            <span class="student-no">{{ current.studentNo }}</span>
          </div>
          <div class="progress-pill">
            自动题 {{ autoProgress.completed }}/{{ autoProgress.total }} · 人工题 {{ manualProgress.completed }}/{{ manualProgress.total }}
          </div>
          <NTag size="small" :type="statusType(current.status)" :bordered="false">
            {{ statusLabel(current.status) }}
          </NTag>
          <NButton size="small" quaternary class="profile-btn" aria-label="查看学生档案" @click="openProfile(current.studentId, current.studentName || '学生')">
            <template #icon><NIcon :size="14"><PersonOutline /></NIcon></template>
            学生档案
          </NButton>
        </div>

        <div class="content-preview">
          <div class="content-label">
            <span>提交内容</span>
            <small>自动题显示预评分，人工题需要逐维度确认后才能保存。</small>
          </div>
          <NAlert v-if="Object.keys(validationErrors).length" type="error" :bordered="false" class="validation-summary">
            还有 {{ Object.keys(validationErrors).length }} 道人工题未完成评分。请补齐后再保存批改。
          </NAlert>

          <WorksheetSubmissionPanel
            v-if="task?.type === 'worksheet'"
            :questions="schema.questions ?? []"
            :content="parsedContent"
            :submission-id="current.id"
            :fallback-content="current.content"
            :get-score="getQuestionScore"
            :get-comment="getQuestionComment"
            :get-reference-visible="getReferenceVisible"
            :validation-errors="validationErrors"
            @score-change="setQuestionScore"
            @feedback-change="setQuestionComment"
            @reference-visible-change="setReferenceVisible"
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

        <section class="teacher-comment-section" aria-label="整份任务总评">
          <label class="teacher-comment-label" for="teacher-comment">总评 / 特殊处理原因</label>
          <NInput
            id="teacher-comment"
            v-model:value="currentTeacherComment"
            type="textarea"
            placeholder="写给学生的整份任务反馈；标记特殊处理时这里作为原因。"
            :autosize="{ minRows: 3, maxRows: 6 }"
          />
        </section>

        <div class="actions">
          <NButton :disabled="currentIdx === 0 || submitting || specialLoading" @click="currentIdx--">
            <template #icon><NIcon><ChevronBackOutline /></NIcon></template>
            上一个
          </NButton>
          <NButton type="primary" :loading="submitting" :disabled="specialLoading" @click="submitGrade">
            <template #icon><NIcon><SaveOutline /></NIcon></template>
            保存批改
          </NButton>
          <NButton v-if="current.status !== 'special'" type="warning" :loading="specialLoading" :disabled="submitting" @click="markSpecial">特殊处理</NButton>
          <NButton v-else :loading="specialLoading" :disabled="submitting" @click="unmarkSpecial">取消特殊处理</NButton>
          <NButton type="error" secondary :disabled="submitting || specialLoading" @click="showReturnModal = true">退回修改</NButton>
          <NButton :disabled="submitting || specialLoading" @click="router.push(`/teacher/tasks/${taskId}/analytics`)">返回数据看板</NButton>
          <NButton :disabled="currentIdx >= submissions.length - 1 || submitting || specialLoading" @click="currentIdx++">
            下一个
            <template #icon><NIcon><ChevronForwardOutline /></NIcon></template>
          </NButton>
        </div>
      </div>
      <NEmpty v-else description="暂无提交需要评分">
        <template #extra><NButton size="small" @click="router.push(`/teacher/tasks/${taskId}/analytics`)">返回批改收件箱</NButton></template>
      </NEmpty>
    </NSpin>

    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="semesterId" @close="profileStudentId = null" />
    <NModal v-model:show="showReturnModal" preset="card" title="退回学生修改" class="return-modal">
      <NInput v-model:value="returnReason" type="textarea" placeholder="请说明需要修改的内容" :maxlength="500" show-count />
      <template #footer><div class="return-actions"><NButton @click="showReturnModal = false">取消</NButton><NButton type="warning" :loading="returnLoading" :disabled="!returnReason.trim()" @click="confirmReturn">确认退回</NButton></div></template>
    </NModal>
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
.grading-page {
  max-width: 980px;
  margin: 0 auto;
  padding-bottom: 104px;
}
.back-button {
  margin-bottom: 8px;
}
.grading-area {
  margin-top: 16px;
}
.student-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  margin-bottom: 16px;
  background: var(--n-card-color);
}
.student-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.student-name {
  color: var(--n-text-color);
  font-size: 16px;
  font-weight: 600;
}
.student-no {
  color: var(--n-text-color-3);
  font-size: 13px;
}
.progress-pill {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--n-color-embedded);
  color: var(--n-text-color-2);
  font-size: 12px;
  white-space: nowrap;
}
.profile-btn {
  margin-left: auto;
}
.content-preview {
  margin-bottom: 20px;
}
.content-label {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
  color: var(--n-text-color-2);
  font-size: 13px;
}
.content-label span {
  font-weight: 600;
}
.content-label small {
  color: var(--n-text-color-3);
  font-size: 12px;
}
.validation-summary {
  margin-bottom: 12px;
}
.teacher-comment-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
  padding: 14px 16px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-card-color);
}
.teacher-comment-label {
  color: var(--n-text-color-2);
  font-size: 13px;
  font-weight: 600;
}
.actions {
  position: sticky;
  bottom: 0;
  z-index: 20;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
  padding: 12px 16px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px 8px 0 0;
  background: var(--n-card-color);
  backdrop-filter: blur(8px);
}
.preview-modal {
  width: 90vw;
  max-width: 1100px;
  height: 85vh;
}
.return-modal { width: min(480px, calc(100vw - 32px)); }
.return-actions { display: flex; justify-content: flex-end; gap: 8px; }
.preview-body {
  height: calc(85vh - 90px);
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  min-height: 0;
}
.preview-frame {
  display: block;
  width: 100%;
  height: 100%;
  min-width: 100%;
  min-height: 100%;
  border: 0;
  flex: 1 1 auto;
}
@media (max-width: 720px) {
  .student-bar {
    align-items: flex-start;
    flex-direction: column;
  }
  .progress-pill {
    white-space: normal;
  }
  .profile-btn {
    margin-left: 0;
  }
  .content-label {
    flex-direction: column;
    gap: 4px;
  }
  .actions {
    justify-content: stretch;
  }
  .actions :deep(.n-button) {
    flex: 1 1 140px;
  }
}
</style>
