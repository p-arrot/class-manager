<script setup lang="ts">
import { computed, ref, onMounted, h, watch } from 'vue'
import { NAlert, NCheckbox, NDatePicker, NEmpty, NButton, NDataTable, NModal, NForm, NFormItem, NInput, NRadio, NRadioGroup, NSelect, NSpace, NIcon, NPopconfirm, NTag, useMessage } from 'naive-ui'
import { AddOutline, CheckmarkDoneOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { createExam, createExamPaper, deleteExam, gradeExamSubmission, listExamPapers, listExams, listExamSubmissions, updateExam } from '@/api/exams'
import PageHeader from '@/components/PageHeader.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { CORE_DIMENSIONS, emptyQuestion, normalizeDimensionScores, parseTaskSchema, questionStem, questionTotalScore, type QuestionType, type TaskQuestion, type WorksheetAnswerMap } from '@/types/taskSchema'
import type { DataTableColumns } from 'naive-ui'
import type { ExamPaperVO, ExamSubmissionVO, ExamVO, QuestionDimensionScoreDTO } from '@/types/api'

interface ExamForm {
  name: string
  paperId: number | null
  timeRange: [number, number] | null
}

const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const exams = ref<ExamVO[]>([])
const papers = ref<ExamPaperVO[]>([])
const loading = ref(false)
const showModal = ref(false)
const showPaperModal = ref(false)
const showSubmissionModal = ref(false)
const editingId = ref<number | null>(null)
const activeExam = ref<ExamVO | null>(null)
const examSubmissions = ref<ExamSubmissionVO[]>([])
const selectedSubmissionId = ref<number | null>(null)
const loadingSubmissions = ref(false)
const savingGrade = ref(false)
const markAbsent = ref(false)
const gradeScores = ref<QuestionDimensionScoreDTO[]>([])
const form = ref<ExamForm>({ name: '', paperId: null, timeRange: null })
const paperTitle = ref('')
const paperQuestions = ref<TaskQuestion[]>([emptyQuestion('single')])
const paperOptions = computed(() => papers.value.map(paper => ({
  label: paper.title,
  value: paper.id,
})))
const questionTypes: Array<{ label: string; value: QuestionType }> = [
  { label: '填空', value: 'blank' },
  { label: '单选', value: 'single' },
  { label: '多选', value: 'multiple' },
  { label: '是非', value: 'true_false' },
  { label: '简答', value: 'short' },
]
const selectedSubmission = computed(() => examSubmissions.value.find(item => item.id === selectedSubmissionId.value) ?? null)
const activeQuestions = computed(() => parseTaskSchema(activeExam.value?.paperContent).questions ?? [])
const answerMap = computed<WorksheetAnswerMap>(() => {
  if (!selectedSubmission.value?.answers) return {}
  try {
    const parsed = JSON.parse(selectedSubmission.value.answers)
    return parsed && typeof parsed === 'object' ? parsed as WorksheetAnswerMap : {}
  } catch {
    return {}
  }
})
const manualTotalScore = computed(() => gradeScores.value.reduce((sum, item) => sum + (Number(item.earnedScore) || 0), 0))

async function loadExams() {
  if (!activeSemesterId.value) {
    exams.value = []
    return
  }
  loading.value = true
  try {
    exams.value = await listExams(activeSemesterId.value)
  } catch (e) {
    exams.value = []
    message.error(getErrorMessage(e, '加载考试列表失败'))
  } finally {
    loading.value = false
  }
}
async function loadPapers() {
  try {
    papers.value = await listExamPapers()
  } catch (e) {
    papers.value = []
    message.error(getErrorMessage(e, '加载试卷列表失败'))
  }
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', paperId: null, timeRange: null }
  showModal.value = true
}
function openPaperCreate() {
  paperTitle.value = ''
  paperQuestions.value = [emptyQuestion('single')]
  showPaperModal.value = true
}
function addPaperQuestion(type: QuestionType) {
  paperQuestions.value.push(emptyQuestion(type))
}

function removePaperQuestion(index: number) {
  paperQuestions.value.splice(index, 1)
}

function addOption(question: TaskQuestion) {
  question.options = [...(question.options ?? []), `选项 ${(question.options?.length ?? 0) + 1}`]
}

function singleAnswer(question: TaskQuestion) {
  return typeof question.answer === 'string' ? question.answer : null
}

function multipleAnswer(question: TaskQuestion) {
  return Array.isArray(question.answer) ? question.answer : []
}

function booleanAnswer(question: TaskQuestion) {
  return typeof question.answer === 'boolean' ? question.answer : null
}

function totalPaperScore() {
  return paperQuestions.value.reduce((sum, question) => sum + questionTotalScore(question), 0)
}

async function handlePaperSubmit() {
  if (!paperTitle.value.trim()) {
    message.warning('请输入试卷名称')
    return
  }
  if (!paperQuestions.value.length) {
    message.warning('请至少添加一道题')
    return
  }
  for (const [index, question] of paperQuestions.value.entries()) {
    if (!question.stem.trim()) {
      message.warning(`第 ${index + 1} 题缺少题干`)
      return
    }
    if (questionTotalScore(question) <= 0) {
      message.warning(`第 ${index + 1} 题至少需要一个维度分值`)
      return
    }
  }
  try {
    await createExamPaper({
      title: paperTitle.value.trim(),
      content: JSON.stringify({ version: 3, questions: paperQuestions.value.map(question => ({ ...question, dimensionScores: normalizeDimensionScores(question.dimensionScores) })) }),
      totalScore: totalPaperScore(),
    })
    message.success('试卷已创建')
    showPaperModal.value = false
    await loadPapers()
  } catch (e) {
    message.error(getErrorMessage(e, '创建试卷失败'))
  }
}

function openEdit(row: ExamVO) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    paperId: row.paperId ?? null,
    timeRange: [new Date(row.startTime).getTime(), new Date(row.endTime).getTime()],
  }
  showModal.value = true
}

async function handleSubmit() {
  if (!activeSemesterId.value) {
    message.warning('请先选择学期')
    return
  }
  if (!form.value.name.trim()) {
    message.warning('请输入考试名称')
    return
  }
  if (!form.value.paperId) {
    message.warning('请选择试卷')
    return
  }
  if (!form.value.timeRange) {
    message.warning('请选择开始和结束时间')
    return
  }
  try {
    const dto = {
      name: form.value.name,
      paperId: form.value.paperId,
      startTime: toLocalDateTime(form.value.timeRange[0])!,
      endTime: toLocalDateTime(form.value.timeRange[1])!,
      weight: 1,
    }
    if (editingId.value) {
      await updateExam(editingId.value, dto)
    } else {
      await createExam(activeSemesterId.value, dto)
    }
    message.success(editingId.value ? '已更新' : '已创建')
    showModal.value = false
    await loadExams()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteExam(id)
    message.success('已删除')
    await loadExams()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

async function openSubmissions(row: ExamVO) {
  activeExam.value = row
  selectedSubmissionId.value = null
  gradeScores.value = []
  markAbsent.value = false
  showSubmissionModal.value = true
  await loadSubmissions(row.id)
}

async function loadSubmissions(examId = activeExam.value?.id) {
  if (!examId) return
  loadingSubmissions.value = true
  try {
    examSubmissions.value = await listExamSubmissions(examId)
    selectedSubmissionId.value = examSubmissions.value[0]?.id ?? null
    initGradeScores()
  } catch (e) {
    examSubmissions.value = []
    message.error(getErrorMessage(e, '加载考试提交失败'))
  } finally {
    loadingSubmissions.value = false
  }
}

function selectSubmission(row: ExamSubmissionVO) {
  selectedSubmissionId.value = row.id
  markAbsent.value = row.status === 'absent'
  initGradeScores()
}

function initGradeScores() {
  const questions = activeQuestions.value
  gradeScores.value = questions.flatMap(question =>
    normalizeDimensionScores(question.dimensionScores, question.score)
      .filter(item => item.maxScore > 0)
      .map(item => ({
        questionId: question.id,
        dimension: item.dimension,
        earnedScore: 0,
        maxScore: item.maxScore,
        autoGraded: false,
      })),
  )
}

function answerText(question: TaskQuestion) {
  const value = answerMap.value[question.id]
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'boolean') return value ? '正确' : '错误'
  if (value == null || value === '') return '未作答'
  return String(value)
}

function updateScore(questionId: string, dimension: string, value: string) {
  const target = gradeScores.value.find(item => item.questionId === questionId && item.dimension === dimension)
  if (!target) return
  target.earnedScore = Math.max(0, Math.min(Number(value) || 0, target.maxScore))
}

function dimensionLabel(dimension: string) {
  return CORE_DIMENSIONS.find(item => item.key === dimension)?.label ?? dimension
}

function statusLabel(status: string) {
  if (status === 'graded') return '已批改'
  if (status === 'absent') return '缺考'
  if (status === 'submitted') return '待批改'
  return status || '-'
}

async function saveGrade() {
  if (!selectedSubmission.value) return
  savingGrade.value = true
  try {
    await gradeExamSubmission(selectedSubmission.value.id, {
      score: markAbsent.value ? 0 : Math.round(manualTotalScore.value),
      absent: markAbsent.value,
      dimensionScores: markAbsent.value ? [] : gradeScores.value,
    })
    message.success(markAbsent.value ? '已标记缺考' : '考试批改已保存')
    await loadSubmissions()
  } catch (e) {
    message.error(getErrorMessage(e, '保存考试批改失败'))
  } finally {
    savingGrade.value = false
  }
}

const examColumns: DataTableColumns<ExamVO> = [
  { title: '考试名称', key: 'name' },
  { title: '开始时间', key: 'startTime', render: row => formatDate(row.startTime, 'datetime') },
  { title: '结束时间', key: 'endTime', render: row => formatDate(row.endTime, 'datetime') },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row => h(NSpace, { size: 2 }, () => [
      h(NButton, { size: 'tiny', quaternary: true, title: '编辑考试', 'aria-label': '编辑考试', onClick: () => openEdit(row) }, () => h(NIcon, { size: 14 }, () => h(CreateOutline))),
      h(NButton, { size: 'tiny', quaternary: true, title: '提交/批改', 'aria-label': '提交/批改', onClick: () => openSubmissions(row) }, () => h(NIcon, { size: 14 }, () => h(CheckmarkDoneOutline))),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, title: '删除考试', 'aria-label': '删除考试' }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))),
        default: () => '确认删除？',
      }),
    ]),
  },
]

const submissionColumns: DataTableColumns<ExamSubmissionVO> = [
  { title: '学生', key: 'studentName', width: 92, render: row => row.studentName || '-' },
  { title: '学号', key: 'studentNo', width: 110, render: row => row.studentNo || '-' },
  { title: '状态', key: 'status', width: 80, render: row => h(NTag, { size: 'small', type: row.status === 'graded' ? 'success' : row.status === 'absent' ? 'error' : 'warning', bordered: false }, () => statusLabel(row.status)) },
  { title: '分数', key: 'score', width: 70, render: row => row.score ?? '-' },
]

watch(activeSemesterId, loadExams)

onMounted(async () => {
  try {
    await loadCourses()
  } catch (e) {
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
  await loadPapers()
})
</script>

<template>
  <div class="page">
    <PageHeader title="考试管理" subtitle="创建和管理学期考试">
      <template #actions>
        <NSpace :size="8">
          <NButton size="small" @click="openPaperCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建试卷</NButton>
          <NButton size="small" :disabled="!activeSemesterId" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建考试</NButton>
        </NSpace>
      </template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" :disabled="!activeCourseId" />
    </div>
    <NDataTable v-if="exams.length" :data="exams" :columns="examColumns" size="small" :row-key="(r: ExamVO)=>r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    <NEmpty v-else description="请选择课程和学期后管理考试" />
    <NModal v-model:show="showModal" :title="editingId ? '编辑考试' : '创建考试'" preset="card" class="form-modal">
      <NForm label-placement="left" label-width="72">
        <NFormItem label="名称" required><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="试卷" required><NSelect v-model:value="form.paperId" :options="paperOptions" placeholder="选择试卷" /></NFormItem>
        <NFormItem label="考试时间"><NDatePicker v-model:value="form.timeRange" type="datetimerange" clearable class="date-picker" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal = false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showPaperModal" title="创建试卷" preset="card" class="paper-modal">
      <NForm label-placement="top">
        <NFormItem label="试卷名称" required><NInput v-model:value="paperTitle" /></NFormItem>
        <div class="paper-questions">
          <article v-for="(question, index) in paperQuestions" :key="question.id" class="paper-question">
            <div class="question-head">
              <NTag size="small" :bordered="false">第 {{ index + 1 }} 题</NTag>
              <NSelect v-model:value="question.type" :options="questionTypes" size="small" class="type-select" />
              <NButton size="tiny" quaternary @click="removePaperQuestion(index)"><template #icon><NIcon><TrashOutline /></NIcon></template></NButton>
            </div>
            <MarkdownEditor v-model="question.stem" :min-height="150" />
            <div v-if="question.type === 'single' || question.type === 'multiple'" class="option-list">
              <div v-for="(_option, optionIndex) in question.options" :key="optionIndex" class="option-row">
                <NInput v-model:value="question.options![optionIndex]" size="small" />
              </div>
              <NButton size="tiny" text @click="addOption(question)">添加选项</NButton>
            </div>
            <div class="settings-row">
              <NCheckbox v-model:checked="question.autoGrade">自动批改</NCheckbox>
              <NTag size="small" :bordered="false">总分 {{ questionTotalScore(question) }}</NTag>
            </div>
            <NSelect
              v-if="question.autoGrade && question.type === 'single'"
              :value="singleAnswer(question)"
              :options="(question.options ?? []).filter(Boolean).map(item => ({ label: item, value: item }))"
              placeholder="正确答案"
              @update:value="value => { question.answer = value }"
            />
            <NSelect
              v-else-if="question.autoGrade && question.type === 'multiple'"
              :value="multipleAnswer(question)"
              multiple
              :options="(question.options ?? []).filter(Boolean).map(item => ({ label: item, value: item }))"
              placeholder="正确答案"
              @update:value="value => { question.answer = value }"
            />
            <NRadioGroup v-else-if="question.autoGrade && question.type === 'true_false'" :value="booleanAnswer(question)" @update:value="value => { question.answer = value }">
              <NRadio :value="true">正确</NRadio>
              <NRadio :value="false">错误</NRadio>
            </NRadioGroup>
            <NInput v-else-if="question.autoGrade" :value="String(question.answer ?? '')" placeholder="正确答案" @update:value="value => { question.answer = value }" />
            <div class="dimension-grid">
              <label v-for="dim in CORE_DIMENSIONS" :key="dim.key" class="dimension-cell">
                <span>{{ dim.label }}</span>
                <NInput
                  :value="String(question.dimensionScores.find(item => item.dimension === dim.key)?.maxScore ?? 0)"
                  size="small"
                  @update:value="value => {
                    question.dimensionScores = normalizeDimensionScores(question.dimensionScores)
                    const target = question.dimensionScores.find(item => item.dimension === dim.key)
                    if (target) target.maxScore = Math.max(0, Number(value) || 0)
                  }"
                >
                  <template #suffix>分</template>
                </NInput>
              </label>
            </div>
          </article>
        </div>
        <NSpace :size="8" class="add-row">
          <NButton v-for="type in questionTypes" :key="type.value" size="small" @click="addPaperQuestion(type.value)">{{ type.label }}</NButton>
        </NSpace>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showPaperModal = false">取消</NButton><NButton type="primary" @click="handlePaperSubmit">创建试卷</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showSubmissionModal" title="考试提交批改" preset="card" class="submission-modal">
      <div class="submission-workbench">
        <aside class="submission-list">
          <div class="panel-title">提交列表</div>
          <NDataTable
            v-if="examSubmissions.length"
            :data="examSubmissions"
            :columns="submissionColumns"
            :row-key="(row: ExamSubmissionVO) => row.id"
            size="small"
            :loading="loadingSubmissions"
            :row-props="row => ({ class: row.id === selectedSubmissionId ? 'selected-row' : '', onClick: () => selectSubmission(row) })"
          />
          <NEmpty v-else description="暂无提交" class="empty-state" />
        </aside>

        <section class="grading-panel">
          <template v-if="selectedSubmission">
            <div class="grading-head">
              <div>
                <div class="student-title">{{ selectedSubmission.studentName || '未命名学生' }}</div>
                <div class="student-meta">{{ selectedSubmission.studentNo || '-' }} · {{ statusLabel(selectedSubmission.status) }}</div>
              </div>
              <div class="score-total">{{ markAbsent ? 0 : Math.round(manualTotalScore) }} 分</div>
            </div>

            <NAlert type="info" :bordered="false" class="grading-note">
              自动题和人工题都可以在这里做最终确认；保存后写入考试结果评价分。
            </NAlert>

            <NCheckbox v-model:checked="markAbsent" class="absent-check">标记为缺考，本次考试记 0 分</NCheckbox>

            <div class="question-review-list" :class="{ disabled: markAbsent }">
              <article v-for="(question, index) in activeQuestions" :key="question.id" class="review-question">
                <div class="review-question-head">
                  <NTag size="small" :bordered="false">第 {{ index + 1 }} 题</NTag>
                  <span>{{ question.type }}</span>
                  <strong>{{ questionTotalScore(question) }} 分</strong>
                </div>
                <p class="question-stem">{{ questionStem(question) }}</p>
                <div class="answer-box">
                  <span>学生答案</span>
                  <strong>{{ answerText(question) }}</strong>
                </div>
                <div class="score-grid">
                  <label
                    v-for="score in gradeScores.filter(item => item.questionId === question.id)"
                    :key="`${score.questionId}-${score.dimension}`"
                    class="score-cell"
                  >
                    <span>{{ dimensionLabel(score.dimension) }}</span>
                    <NInput
                      :value="String(score.earnedScore)"
                      size="small"
                      :disabled="markAbsent"
                      @update:value="value => updateScore(score.questionId, score.dimension, value)"
                    >
                      <template #suffix>/ {{ score.maxScore }}</template>
                    </NInput>
                  </label>
                </div>
              </article>
            </div>
          </template>
          <NEmpty v-else description="请选择一名学生查看答题详情" class="empty-state" />
        </section>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showSubmissionModal = false">关闭</NButton>
          <NButton type="primary" :loading="savingGrade" :disabled="!selectedSubmission" @click="saveGrade">保存批改</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.toolbar { margin: 16px 0; display: flex; gap: 12px; flex-wrap: wrap; }
.toolbar-select { width: 220px; }
.form-modal { width: min(480px, calc(100vw - 32px)); }
.date-picker { width: 100%; }
.paper-modal { width: min(920px, calc(100vw - 32px)); }
.paper-questions { display: flex; flex-direction: column; gap: 14px; max-height: 62vh; overflow: auto; padding-right: 4px; }
.paper-question { display: flex; flex-direction: column; gap: 10px; padding: 12px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.question-head { display: flex; align-items: center; gap: 8px; }
.type-select { width: 120px; margin-left: auto; }
.option-list { display: flex; flex-direction: column; gap: 6px; }
.settings-row { display: flex; align-items: center; gap: 10px; }
.dimension-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; padding: 10px; border-radius: 8px; background: var(--n-color-embedded); }
.dimension-cell { display: grid; grid-template-columns: minmax(120px, 1fr) 100px; gap: 8px; align-items: center; font-size: 13px; color: var(--n-text-color-2); }
.add-row { margin-top: 12px; }
.submission-modal { width: min(1180px, calc(100vw - 32px)); }
.submission-workbench { display: grid; grid-template-columns: 360px minmax(0, 1fr); gap: 16px; min-height: 560px; }
.submission-list { border-right: 1px solid var(--n-border-color); padding-right: 16px; }
.panel-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: var(--n-text-color-2); }
.grading-panel { min-width: 0; }
.grading-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.student-title { font-size: 18px; font-weight: 700; }
.student-meta { font-size: 12px; color: var(--n-text-color-3); margin-top: 4px; }
.score-total { font-size: 24px; font-weight: 800; color: var(--n-text-color-1); }
.grading-note { margin-bottom: 12px; }
.absent-check { margin-bottom: 12px; }
.question-review-list { display: flex; flex-direction: column; gap: 12px; max-height: 450px; overflow: auto; padding-right: 4px; }
.question-review-list.disabled { opacity: 0.58; }
.review-question { border: 1px solid var(--n-border-color); border-radius: 8px; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.review-question-head { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--n-text-color-3); }
.review-question-head strong { margin-left: auto; color: var(--n-text-color-2); }
.question-stem { margin: 0; white-space: pre-wrap; line-height: 1.6; }
.answer-box { display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: 8px; padding: 8px 10px; background: var(--n-color-embedded); border-radius: 6px; font-size: 13px; }
.answer-box span { color: var(--n-text-color-3); }
.answer-box strong { white-space: pre-wrap; word-break: break-word; }
.score-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.score-cell { display: grid; grid-template-columns: minmax(110px, 1fr) 120px; align-items: center; gap: 8px; font-size: 13px; color: var(--n-text-color-2); }
:deep(.selected-row td) { background: var(--n-merged-th-color); }
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
  .dimension-grid { grid-template-columns: 1fr; }
  .dimension-cell { grid-template-columns: 1fr; }
  .submission-workbench { grid-template-columns: 1fr; }
  .submission-list { border-right: 0; padding-right: 0; }
  .score-grid { grid-template-columns: 1fr; }
  .score-cell { grid-template-columns: 1fr; }
}
</style>
