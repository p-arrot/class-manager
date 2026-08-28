<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NCheckbox, NDataTable, NDatePicker, NEmpty, NForm, NFormItem, NIcon, NInput, NModal, NPopconfirm, NRadio, NRadioGroup, NSelect, NSpace, NTag, useMessage } from 'naive-ui'
import { AddOutline, CheckmarkDoneOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { createExam, createExamPaper, deleteExam, listExamPapers, listExams, updateExam } from '@/api/exams'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { CORE_DIMENSIONS, emptyQuestion, normalizeDimensionScores, questionTotalScore } from '@/types/taskSchema'
import type { DataTableColumns } from 'naive-ui'
import type { ExamPaperVO, ExamVO } from '@/types/api'
import type { QuestionType, TaskQuestion } from '@/types/taskSchema'

interface ExamForm { name: string; paperId: number | null; timeRange: [number, number] | null }

const router = useRouter()
const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const exams = ref<ExamVO[]>([])
const papers = ref<ExamPaperVO[]>([])
const showExamForm = ref(false)
const showPaperForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref<ExamForm>({ name: '', paperId: null, timeRange: null })
const paperTitle = ref('')
const paperQuestions = ref<TaskQuestion[]>([emptyQuestion('single')])

const paperOptions = computed(() => papers.value.map(item => ({ label: item.title, value: item.id })))
const questionTypes: Array<{ label: string; value: QuestionType }> = [
  { label: '填空', value: 'blank' }, { label: '单选', value: 'single' }, { label: '多选', value: 'multiple' },
  { label: '是非', value: 'true_false' }, { label: '简答', value: 'short' },
]

async function loadExams() {
  if (!activeSemesterId.value) { exams.value = []; return }
  try { exams.value = await listExams(activeSemesterId.value) }
  catch (error) { exams.value = []; message.error(getErrorMessage(error, '加载考试列表失败')) }
}

async function loadPapers() {
  try { papers.value = await listExamPapers() }
  catch (error) { papers.value = []; message.error(getErrorMessage(error, '加载试卷列表失败')) }
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', paperId: null, timeRange: null }
  showExamForm.value = true
}

function openEdit(row: ExamVO) {
  editingId.value = row.id
  form.value = { name: row.name, paperId: row.paperId ?? null, timeRange: [new Date(row.startTime).getTime(), new Date(row.endTime).getTime()] }
  showExamForm.value = true
}

async function saveExam() {
  if (!activeSemesterId.value) return message.warning('请先选择学期')
  if (!form.value.name.trim()) return message.warning('请输入考试名称')
  if (!form.value.paperId) return message.warning('请选择试卷')
  if (!form.value.timeRange) return message.warning('请选择开始和结束时间')
  if (form.value.timeRange[1] <= form.value.timeRange[0]) return message.warning('结束时间必须晚于开始时间')
  const data = { name: form.value.name.trim(), paperId: form.value.paperId, startTime: toLocalDateTime(form.value.timeRange[0])!, endTime: toLocalDateTime(form.value.timeRange[1])!, weight: 1 }
  try {
    if (editingId.value) await updateExam(editingId.value, data)
    else await createExam(activeSemesterId.value, data)
    message.success(editingId.value ? '考试已更新' : '考试已创建')
    showExamForm.value = false
    await loadExams()
  } catch (error) { message.error(getErrorMessage(error, '保存考试失败')) }
}

function openPaperCreate() {
  paperTitle.value = ''
  paperQuestions.value = [emptyQuestion('single')]
  showPaperForm.value = true
}

function closePaperForm() {
  const hasContent = paperTitle.value.trim() || paperQuestions.value.some(item => item.stem.trim())
  if (hasContent && !window.confirm('试卷尚未创建，确认放弃当前编辑吗？')) return
  showPaperForm.value = false
}

function addQuestion(type: QuestionType) { paperQuestions.value.push(emptyQuestion(type)) }
function removeQuestion(index: number) { paperQuestions.value.splice(index, 1) }
function addOption(question: TaskQuestion) { question.options = [...(question.options ?? []), `选项 ${(question.options?.length ?? 0) + 1}`] }
function singleAnswer(question: TaskQuestion) { return typeof question.answer === 'string' ? question.answer : null }
function multipleAnswer(question: TaskQuestion) { return Array.isArray(question.answer) ? question.answer : [] }
function booleanAnswer(question: TaskQuestion) { return typeof question.answer === 'boolean' ? question.answer : null }

async function savePaper() {
  if (!paperTitle.value.trim()) return message.warning('请输入试卷名称')
  if (!paperQuestions.value.length) return message.warning('请至少添加一道题')
  for (const [index, question] of paperQuestions.value.entries()) {
    if (!question.stem.trim()) return message.warning(`第 ${index + 1} 题缺少题干`)
    if (questionTotalScore(question) <= 0) return message.warning(`第 ${index + 1} 题至少需要一个维度分值`)
  }
  try {
    await createExamPaper({
      title: paperTitle.value.trim(),
      content: JSON.stringify({ version: 3, questions: paperQuestions.value.map(item => ({ ...item, dimensionScores: normalizeDimensionScores(item.dimensionScores) })) }),
      totalScore: paperQuestions.value.reduce((sum, item) => sum + questionTotalScore(item), 0),
    })
    message.success('试卷已创建')
    showPaperForm.value = false
    await loadPapers()
  } catch (error) { message.error(getErrorMessage(error, '创建试卷失败')) }
}

async function removeExam(id: number) {
  try { await deleteExam(id); message.success('考试已删除'); await loadExams() }
  catch (error) { message.error(getErrorMessage(error, '删除考试失败')) }
}

const columns: DataTableColumns<ExamVO> = [
  { title: '考试名称', key: 'name', minWidth: 150 },
  { title: '开始时间', key: 'startTime', width: 150, render: row => formatDate(row.startTime, 'datetime') },
  { title: '结束时间', key: 'endTime', width: 150, render: row => formatDate(row.endTime, 'datetime') },
  { title: '操作', key: 'actions', width: 128, render: row => h(NSpace, { size: 2 }, () => [
    h(NButton, { size: 'tiny', quaternary: true, title: '编辑考试', 'aria-label': '编辑考试', onClick: () => openEdit(row) }, () => h(NIcon, { size: 14 }, () => h(CreateOutline))),
    h(NButton, { size: 'tiny', quaternary: true, title: '提交/批改', 'aria-label': '提交/批改', onClick: () => router.push(`/teacher/exams/${row.id}/submissions`) }, () => h(NIcon, { size: 14 }, () => h(CheckmarkDoneOutline))),
    h(NPopconfirm, { onPositiveClick: () => removeExam(row.id) }, { trigger: () => h(NButton, { size: 'tiny', quaternary: true, title: '删除考试', 'aria-label': '删除考试' }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))), default: () => '确认删除？' }),
  ]) },
]

watch(activeSemesterId, loadExams)
onMounted(async () => { await loadCourses().catch(error => message.error(getErrorMessage(error, '加载课程失败'))); await loadPapers() })
</script>

<template>
  <div class="page">
    <PageHeader title="考试管理" subtitle="创建试卷、安排考试并从学生名单进入批改">
      <template #actions><NSpace><NButton size="small" @click="openPaperCreate"><template #icon><NIcon><AddOutline /></NIcon></template>创建试卷</NButton><NButton size="small" :disabled="!activeSemesterId" @click="openCreate"><template #icon><NIcon><AddOutline /></NIcon></template>创建考试</NButton></NSpace></template>
    </PageHeader>
    <div class="toolbar"><NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" /><NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" :disabled="!activeCourseId" /></div>
    <NDataTable v-if="exams.length" :data="exams" :columns="columns" :scroll-x="578" size="small" :row-key="row => row.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    <NEmpty v-else description="请选择课程和学期后管理考试" />

    <NModal v-model:show="showExamForm" :title="editingId ? '编辑考试' : '创建考试'" preset="card" class="exam-form">
      <NForm label-placement="left" label-width="80"><NFormItem label="名称" required><NInput v-model:value="form.name" /></NFormItem><NFormItem label="试卷" required><NSelect v-model:value="form.paperId" :options="paperOptions" placeholder="选择试卷" /></NFormItem><NFormItem label="考试时间" required><NDatePicker v-model:value="form.timeRange" type="datetimerange" clearable class="full" /></NFormItem></NForm>
      <template #footer><NSpace justify="end"><NButton @click="showExamForm = false">取消</NButton><NButton type="primary" @click="saveExam">保存</NButton></NSpace></template>
    </NModal>

    <NModal :show="showPaperForm" title="创建试卷" preset="card" class="paper-form" :mask-closable="false" @update:show="value => { if (!value) closePaperForm() }">
      <NForm label-placement="top"><NFormItem label="试卷名称" required><NInput v-model:value="paperTitle" /></NFormItem>
        <div class="questions"><article v-for="(question, index) in paperQuestions" :key="question.id" class="question">
          <div class="question-head"><NTag size="small" :bordered="false">第 {{ index + 1 }} 题</NTag><NSelect v-model:value="question.type" :options="questionTypes" size="small" /><NButton quaternary title="删除题目" aria-label="删除题目" @click="removeQuestion(index)"><template #icon><NIcon><TrashOutline /></NIcon></template></NButton></div>
          <MarkdownEditor v-model="question.stem" :min-height="140" />
          <div v-if="question.type === 'single' || question.type === 'multiple'" class="options"><NInput v-for="(_option, optionIndex) in question.options" :key="optionIndex" v-model:value="question.options![optionIndex]" size="small" /><NButton text @click="addOption(question)">添加选项</NButton></div>
          <div class="settings"><NCheckbox v-model:checked="question.autoGrade">自动批改</NCheckbox><NTag size="small" :bordered="false">总分 {{ questionTotalScore(question) }}</NTag></div>
          <NSelect v-if="question.autoGrade && question.type === 'single'" :value="singleAnswer(question)" :options="(question.options ?? []).filter(Boolean).map(value => ({ label: value, value }))" placeholder="正确答案" @update:value="value => { question.answer = value }" />
          <NSelect v-else-if="question.autoGrade && question.type === 'multiple'" :value="multipleAnswer(question)" multiple :options="(question.options ?? []).filter(Boolean).map(value => ({ label: value, value }))" placeholder="正确答案" @update:value="value => { question.answer = value }" />
          <NRadioGroup v-else-if="question.autoGrade && question.type === 'true_false'" :value="booleanAnswer(question)" @update:value="value => { question.answer = value }"><NRadio :value="true">正确</NRadio><NRadio :value="false">错误</NRadio></NRadioGroup>
          <NInput v-else-if="question.autoGrade" :value="String(question.answer ?? '')" placeholder="正确答案" @update:value="value => { question.answer = value }" />
          <div class="rubric"><label v-for="dimension in CORE_DIMENSIONS" :key="dimension.key"><span>{{ dimension.label }}</span><NInput :value="String(question.dimensionScores.find(item => item.dimension === dimension.key)?.maxScore ?? 0)" size="small" @update:value="value => { question.dimensionScores = normalizeDimensionScores(question.dimensionScores); const target = question.dimensionScores.find(item => item.dimension === dimension.key); if (target) target.maxScore = Math.max(0, Number(value) || 0) }"><template #suffix>分</template></NInput></label></div>
        </article></div>
        <NSpace class="add-row"><NButton v-for="type in questionTypes" :key="type.value" size="small" @click="addQuestion(type.value)">{{ type.label }}</NButton></NSpace>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="closePaperForm">取消</NButton><NButton type="primary" @click="savePaper">创建试卷</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.toolbar { display: grid; grid-template-columns: repeat(2, 220px); gap: 12px; margin: 16px 0; }
.exam-form { width: min(500px, calc(100vw - 32px)); }
.paper-form { width: min(920px, calc(100vw - 32px)); }
.full { width: 100%; }
.questions { display: grid; gap: 14px; max-height: 62vh; overflow: auto; padding-right: 4px; }
.question { display: grid; gap: 10px; padding: 14px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.question-head { display: grid; grid-template-columns: auto 130px 44px; justify-content: end; align-items: center; gap: 8px; }
.options { display: grid; gap: 6px; }
.settings { display: flex; align-items: center; gap: 10px; }
.rubric { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; padding: 10px; background: var(--n-color-embedded); border-radius: 6px; }
.rubric label { display: grid; grid-template-columns: minmax(120px, 1fr) 100px; align-items: center; gap: 8px; color: var(--n-text-color-2); font-size: 13px; }
.add-row { margin-top: 12px; }
@media (max-width: 640px) { .toolbar, .rubric, .rubric label { grid-template-columns: 1fr; } .question-head { grid-template-columns: auto 1fr 44px; } }
</style>
