<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { init, use, type ComposeOption, type ECharts } from 'echarts/core'
import { BarChart, PieChart, type BarSeriesOption, type PieSeriesOption } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, type GridComponentOption, type LegendComponentOption, type TooltipComponentOption } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { NButton, NDataTable, NEmpty, NIcon, NModal, NProgress, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import { ArrowBackOutline, CreateOutline, RefreshOutline } from '@vicons/ionicons5'
import { listAllClasses } from '@/api/classes'
import { getCourse } from '@/api/courses'
import { getLesson } from '@/api/lessons'
import { getSemester } from '@/api/semesters'
import { getTask, getTaskAnalytics } from '@/api/tasks'
import MarkdownView from '@/components/MarkdownView.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useRealtime } from '@/composables/useRealtime'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { useClassFilterStore } from '@/stores/classFilter'
import type { ClassVO, QuestionAnalyticsVO, StudentAnswerVO, StudentTaskAnswerVO, TaskAnalyticsVO, TaskDetailVO } from '@/types/api'

use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type DashboardChartOption = ComposeOption<GridComponentOption | LegendComponentOption | TooltipComponentOption | BarSeriesOption | PieSeriesOption>

const route = useRoute()
const router = useRouter()
const message = useMessage()
const classFilter = useClassFilterStore()
const taskId = Number(route.params.taskId)
const loading = ref(false)
const metaLoading = ref(false)
const analytics = ref<TaskAnalyticsVO | null>(null)
const task = ref<TaskDetailVO | null>(null)
const classes = ref<ClassVO[]>([])
const courseClassIds = ref<number[]>([])
const selectedClassId = ref(0)
const activeQuestion = ref<QuestionAnalyticsVO | null>(null)
const submissionChartRef = ref<HTMLDivElement | null>(null)
const accuracyChartRef = ref<HTMLDivElement | null>(null)
let submissionChart: ECharts | null = null
let accuracyChart: ECharts | null = null
const realtime = useRealtime()
let syncingClassFromStore = false

const isWorksheet = computed(() => analytics.value?.type === 'worksheet')
const questionRows = computed(() => analytics.value?.questions ?? [])
const autoQuestions = computed(() => questionRows.value.filter(item => item.autoGradable))
const manualQuestions = computed(() => questionRows.value.filter(item => !item.autoGradable))
const completedCount = computed(() => {
  const data = analytics.value
  if (!data) return 0
  return data.submittedCount + data.gradedCount + data.specialCount
})
const classOptions = computed<SelectOption[]>(() => {
  const classMap = new Map(classes.value.map(item => [item.id, item]))
  return [
    { label: '全部授课班级', value: 0 },
    ...courseClassIds.value.map(id => {
      const item = classMap.get(id)
      return {
        label: item ? `${item.grade}级 ${item.name}` : `班级 ${id}`,
        value: id,
      }
    }),
  ]
})
const scopeLabel = computed(() => classOptions.value.find(item => item.value === selectedClassId.value)?.label ?? '全部授课班级')
const questionCount = computed(() => analytics.value?.questionCount ?? questionRows.value.length)
const autoQuestionCount = computed(() => analytics.value?.autoQuestionCount ?? autoQuestions.value.length)
const manualQuestionCount = computed(() => analytics.value?.manualQuestionCount ?? manualQuestions.value.length)

const submissionColumns: DataTableColumns<StudentTaskAnswerVO> = [
  { title: '班级', key: 'className', width: 130, render: row => row.className || '-' },
  { title: '学号', key: 'studentNo', width: 130, render: row => row.studentNo || '-' },
  { title: '姓名', key: 'studentName', width: 120, render: row => row.studentName || '-' },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: row => h(NTag, { size: 'small', bordered: false, type: statusType(row.status) }, () => statusLabel(row.status)),
  },
  { title: '提交时间', key: 'submittedAt', width: 180, render: row => row.submittedAt ? formatDate(row.submittedAt, 'datetime') : '-' },
  { title: '内容摘要', key: 'content', minWidth: 180, render: row => submissionSummary(row) },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: row => h(
      NButton,
      {
        size: 'small',
        type: row.status === 'submitted' ? 'primary' : 'default',
        disabled: !row.submissionId,
        onClick: () => openSubmission(row),
      },
      () => row.status === 'graded' || row.status === 'special' ? '查看/复核' : row.submissionId ? '去批改' : '未提交',
    ),
  },
]

const answerColumns: DataTableColumns<StudentAnswerVO> = [
  { title: '学号', key: 'studentNo', width: 150, render: row => row.studentNo || '-' },
  { title: '姓名', key: 'studentName', width: 140, render: row => row.studentName || '-' },
  { title: '作答', key: 'answer', render: row => answerText(row.answer) },
  {
    title: '结果',
    key: 'correct',
    width: 120,
    render: row => row.correct === null
      ? h(NTag, { size: 'small', bordered: false }, () => '待人工评分')
      : h(NTag, { size: 'small', bordered: false, type: row.correct ? 'success' : 'error' }, () => row.correct ? '正确' : '错误'),
  },
]

async function loadMeta() {
  metaLoading.value = true
  try {
    task.value = await getTask(taskId)
    if (!task.value?.lessonId) return
    const lesson = await getLesson(task.value.lessonId)
    const semester = await getSemester(lesson.semesterId)
    const [course, allClasses] = await Promise.all([
      getCourse(semester.courseId),
      listAllClasses(),
    ])
    courseClassIds.value = course.classIds ?? []
    classes.value = allClasses
    applyGlobalClassFilter()
  } catch (e) {
    message.error(getErrorMessage(e, '加载任务范围失败'))
  } finally {
    metaLoading.value = false
  }
}

function applyGlobalClassFilter() {
  syncingClassFromStore = true
  const globalClassId = classFilter.selectedClassId
  selectedClassId.value = globalClassId && courseClassIds.value.includes(globalClassId)
    ? globalClassId
    : 0
  queueMicrotask(() => {
    syncingClassFromStore = false
  })
}

async function loadAnalytics(showToast = false) {
  loading.value = true
  try {
    analytics.value = await getTaskAnalytics(taskId, selectedClassId.value > 0 ? selectedClassId.value : undefined)
    renderCharts()
    if (showToast) message.success('数据已刷新')
  } catch (e) {
    analytics.value = null
    message.error(getErrorMessage(e, '加载任务数据看板失败'))
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  requestAnimationFrame(() => {
    renderSubmissionChart()
    renderAccuracyChart()
  })
}

function renderSubmissionChart() {
  if (!submissionChartRef.value || !analytics.value) return
  if (!submissionChart) submissionChart = init(submissionChartRef.value)
  const data = [
    { name: '待批改', value: analytics.value.submittedCount, itemStyle: { color: '#2563eb' } },
    { name: '已批改', value: analytics.value.gradedCount, itemStyle: { color: '#16a34a' } },
    { name: '未提交', value: analytics.value.notSubmittedCount, itemStyle: { color: '#d6d3cc' } },
    { name: '特殊处理', value: analytics.value.specialCount, itemStyle: { color: '#a16207' } },
  ]
  const option: DashboardChartOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#57534e' } },
    series: [{
      type: 'pie',
      radius: ['58%', '74%'],
      center: ['50%', '43%'],
      data,
      label: { formatter: '{b} {c}', color: '#44403c' },
    }],
  }
  submissionChart.setOption(option, true)
}

function renderAccuracyChart() {
  if (!accuracyChartRef.value || !analytics.value) return
  if (!accuracyChart) accuracyChart = init(accuracyChartRef.value)
  const rows = autoQuestions.value
  const option: DashboardChartOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 38, right: 16, top: 24, bottom: 34 },
    xAxis: {
      type: 'category',
      data: rows.map(item => `第 ${item.index} 题`),
      axisLine: { lineStyle: { color: '#d6d3cc' } },
      axisLabel: { color: '#57534e' },
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: '#57534e', formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#e7e5e0' } },
    },
    series: [{
      type: 'bar',
      data: rows.map(item => item.accuracyRate),
      barMaxWidth: 30,
      itemStyle: { color: '#2563eb', borderRadius: [5, 5, 0, 0] },
    }],
  }
  accuracyChart.setOption(option, true)
}

function statusType(status: string) {
  if (status === 'graded') return 'success'
  if (status === 'submitted') return 'info'
  if (status === 'special') return 'warning'
  if (status === 'not_submitted') return 'default'
  return 'default'
}

function statusLabel(status: string) {
  if (status === 'graded') return '已批改'
  if (status === 'submitted') return '待批改'
  if (status === 'special') return '特殊处理'
  if (status === 'not_submitted') return '未提交'
  return status || '未知'
}

function answerText(value: unknown) {
  if (value == null || value === '') return '未作答'
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'boolean') return value ? '正确' : '错误'
  return String(value)
}

function questionSummary(question: QuestionAnalyticsVO) {
  if (question.autoGradable) {
    return `${question.correctCount}/${question.answerCount} 正确`
  }
  return `${question.answerCount} 份作答，批改页逐题评分`
}

function optionRows(question: QuestionAnalyticsVO) {
  return Object.entries(question.optionDistribution)
}

function openGrading() {
  router.push(`/teacher/grading/${taskId}`)
}

function openSubmission(row: StudentTaskAnswerVO) {
  if (!row.submissionId) return
  router.push(`/teacher/grading/${taskId}?submissionId=${row.submissionId}`)
}

function submissionSummary(row: StudentTaskAnswerVO) {
  if (!row.content) return row.status === 'not_submitted' ? '尚未提交' : '-'
  try {
    const parsed = JSON.parse(row.content) as Record<string, unknown>
    if (typeof parsed.note === 'string' && parsed.note.trim()) return parsed.note.trim()
    const answered = Object.values(parsed).filter(value => value !== null && value !== '' && !(Array.isArray(value) && !value.length)).length
    return answered ? `${answered} 项作答` : '已提交'
  } catch {
    return row.content.length > 32 ? `${row.content.slice(0, 32)}...` : row.content
  }
}

function classOptionLabel(option: SelectOption) {
  return String(option.label ?? '')
}

function handleResize() {
  submissionChart?.resize()
  accuracyChart?.resize()
}

watch(analytics, renderCharts)
watch(selectedClassId, (value) => {
  loadAnalytics()
  if (syncingClassFromStore) return
  if (value > 0) {
    classFilter.setClassId(value)
  } else {
    classFilter.clearFilter()
  }
})
watch(() => classFilter.selectedClassId, applyGlobalClassFilter)

onMounted(async () => {
  await Promise.all([loadMeta(), loadAnalytics()])
  realtime.connect()
  setTimeout(() => realtime.subscribeTask(taskId, () => loadAnalytics()), 300)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  submissionChart?.dispose()
  accuracyChart?.dispose()
  realtime.disconnect()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="page analytics-page">
    <PageHeader title="任务数据看板" :hint="analytics?.title || task?.title || '实时查看学生完成情况'">
      <template #actions>
        <div class="header-scope">
          <span>统计范围</span>
          <NSelect
            v-model:value="selectedClassId"
            :options="classOptions"
            :loading="metaLoading"
            :render-label="classOptionLabel"
            class="class-select"
            placeholder="选择班级"
          />
        </div>
        <NButton size="small" @click="router.back()">
          <template #icon><NIcon :size="14"><ArrowBackOutline /></NIcon></template>
          返回
        </NButton>
        <NButton size="small" :loading="loading" @click="loadAnalytics(true)">
          <template #icon><NIcon :size="14"><RefreshOutline /></NIcon></template>
          刷新
        </NButton>
        <NButton size="small" type="primary" @click="openGrading">
          <template #icon><NIcon :size="14"><CreateOutline /></NIcon></template>
          去批改
        </NButton>
      </template>
    </PageHeader>

    <NSpin :show="loading && !analytics">
      <template v-if="analytics">
        <section class="context-strip">
          <span>{{ scopeLabel }}</span>
          <b>{{ analytics.totalStudents }}</b>
          <span>名学生</span>
          <i />
          <b>{{ questionCount }}</b>
          <span>道题，其中 {{ autoQuestionCount }} 道自动题、{{ manualQuestionCount }} 道人工题</span>
        </section>

        <section class="metric-grid">
          <div class="metric primary">
            <span>提交率</span>
            <strong>{{ analytics.submissionRate }}%</strong>
            <NProgress type="line" :percentage="analytics.submissionRate" color="#2563eb" rail-color="#dbeafe" :show-indicator="false" />
          </div>
          <div class="metric">
            <span>待批改</span>
            <strong>{{ analytics.submittedCount }}</strong>
            <p>{{ completedCount }}/{{ analytics.totalStudents }} 人已提交，{{ analytics.notSubmittedCount }} 人未提交</p>
          </div>
          <div class="metric">
            <span>已批改</span>
            <strong>{{ analytics.gradedCount }}</strong>
            <p>{{ analytics.specialCount }} 人特殊处理</p>
          </div>
          <div class="metric">
            <span>自动题准确率</span>
            <strong>{{ isWorksheet ? `${analytics.accuracyRate}%` : '-' }}</strong>
            <p>只统计可自动判定的 {{ autoQuestionCount }} 道题</p>
          </div>
        </section>

        <section class="chart-grid">
          <div class="panel">
            <div class="panel-head">
              <h3>提交构成</h3>
              <span>待批改 / 已批改 / 未提交 / 特殊处理</span>
            </div>
            <div ref="submissionChartRef" class="chart" />
          </div>
          <div class="panel">
            <div class="panel-head">
              <h3>{{ isWorksheet ? '自动题准确率' : '作品提交概览' }}</h3>
              <span>{{ isWorksheet ? `覆盖 ${autoQuestionCount}/${questionCount} 道题` : '课堂作品无单题准确率' }}</span>
            </div>
            <div v-if="isWorksheet && autoQuestions.length" ref="accuracyChartRef" class="chart" />
            <NEmpty v-else description="暂无可自动判定的题目" class="empty-chart" />
          </div>
        </section>

        <section v-if="isWorksheet" class="panel question-panel">
          <div class="panel-head">
            <h3>题目分析</h3>
            <span>自动题看正确率，人工题进入批改页逐题评分</span>
          </div>
          <div class="question-list">
            <button v-for="question in questionRows" :key="question.questionId" class="question-row" type="button" @click="activeQuestion = question">
              <span class="question-index">第 {{ question.index }} 题</span>
              <span class="question-title">{{ question.stem || '未填写题干' }}</span>
              <span class="question-meta">{{ questionSummary(question) }}</span>
              <NTag size="small" :bordered="false" :type="question.autoGradable ? 'success' : 'default'">
                {{ question.autoGradable ? `${question.accuracyRate}% 正确` : '人工评分题' }}
              </NTag>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>批改收件箱</h3>
            <span>{{ analytics.submissions.length }} 名应完成学生</span>
          </div>
          <NDataTable :data="analytics.submissions" :columns="submissionColumns" size="small" :row-key="row => row.studentId" :scroll-x="860" />
        </section>
      </template>
      <NEmpty v-else description="暂无任务数据" />
    </NSpin>

    <NModal
      :show="!!activeQuestion"
      preset="card"
      class="question-modal"
      :title="activeQuestion ? `第 ${activeQuestion.index} 题作答明细` : ''"
      @update:show="value => { if (!value) activeQuestion = null }"
    >
      <template v-if="activeQuestion">
        <MarkdownView :content="activeQuestion.stem" />
        <div class="modal-hint">
          {{ activeQuestion.autoGradable ? '此题已按参考答案自动判定；仍可进入批改页复核并按维度调整分数。' : '此题为人工评分题，分数会在批改页逐份录入，并同步到核心素养维度评分。' }}
        </div>
        <div v-if="optionRows(activeQuestion).length" class="option-bars">
          <div v-for="[option, count] in optionRows(activeQuestion)" :key="option" class="option-bar">
            <span>{{ option }}</span>
            <NProgress type="line" color="#2563eb" rail-color="#e7e5e0" :percentage="completedCount ? Math.round(count * 1000 / completedCount) / 10 : 0" :indicator-placement="'inside'" />
            <b>{{ count }}</b>
          </div>
        </div>
        <NDataTable :data="activeQuestion.answers" :columns="answerColumns" size="small" :row-key="row => row.submissionId" />
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.analytics-page {
  --surface: #ffffff;
  --surface-muted: #f5f4f1;
  --surface-soft: #fafaf9;
  --border: #e7e5e0;
  --text: #1c1917;
  --text-muted: #78716c;
  --accent: #2563eb;
  max-width: 1160px;
  margin: 0 auto;
}
.header-scope {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-scope span {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}
.class-select {
  width: 220px;
}
.context-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0 0 14px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-muted);
  font-size: 12px;
}
.context-strip b {
  color: var(--text);
  font-weight: 600;
}
.context-strip i {
  width: 1px;
  height: 12px;
  margin: 0 4px;
  background: var(--border);
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.metric {
  min-height: 120px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}
.metric.primary {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
}
.metric span {
  display: block;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 13px;
}
.metric strong {
  display: block;
  margin-bottom: 8px;
  color: var(--text);
  font-size: 28px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: 0;
}
.metric p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
}
.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.panel {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  margin-bottom: 16px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}
.panel-head h3 {
  margin: 0;
  color: var(--text);
  font-size: 16px;
}
.panel-head span {
  color: var(--text-muted);
  font-size: 12px;
}
.chart {
  width: 100%;
  height: 280px;
}
.empty-chart {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.question-row {
  width: 100%;
  min-height: 58px;
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) minmax(140px, auto) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition: background-color 150ms ease, border-color 150ms ease;
}
.question-row:hover {
  border-color: #d6d3cc;
  background: var(--surface-muted);
}
.question-index {
  color: var(--text);
  font-size: 13px;
  font-weight: 600;
}
.question-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.question-meta {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}
.question-modal {
  width: min(920px, calc(100vw - 32px));
}
.modal-hint {
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--surface-muted);
  color: var(--text-muted);
  font-size: 13px;
}
.option-bars {
  display: grid;
  gap: 8px;
  margin: 14px 0;
}
.option-bar {
  display: grid;
  grid-template-columns: minmax(80px, 160px) minmax(0, 1fr) 32px;
  align-items: center;
  gap: 10px;
  color: var(--text);
  font-size: 13px;
}
.option-bar b {
  text-align: right;
}
@media (max-width: 960px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-grid { grid-template-columns: 1fr; }
  .question-row { grid-template-columns: 82px minmax(0, 1fr) auto; }
  .question-meta { grid-column: 2 / 4; }
}
@media (max-width: 620px) {
  .header-scope { order: 3; width: 100%; }
  .class-select { width: 100%; }
  .metric-grid { grid-template-columns: 1fr; }
  .question-row { grid-template-columns: 1fr; }
  .question-meta { grid-column: auto; white-space: normal; }
  .option-bar { grid-template-columns: 1fr; }
}
</style>
