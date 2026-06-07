<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { init, use, type ECharts, type ComposeOption } from 'echarts/core'
import { BarChart, PieChart, type BarSeriesOption, type PieSeriesOption } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, type GridComponentOption, type LegendComponentOption, type TooltipComponentOption } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { NButton, NDataTable, NEmpty, NIcon, NModal, NProgress, NSpin, NTag, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { ArrowBackOutline, RefreshOutline } from '@vicons/ionicons5'
import { getTaskAnalytics } from '@/api/tasks'
import MarkdownView from '@/components/MarkdownView.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useRealtime } from '@/composables/useRealtime'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { QuestionAnalyticsVO, StudentAnswerVO, StudentTaskAnswerVO, TaskAnalyticsVO } from '@/types/api'

use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type DashboardChartOption = ComposeOption<GridComponentOption | LegendComponentOption | TooltipComponentOption | BarSeriesOption | PieSeriesOption>

const route = useRoute()
const router = useRouter()
const message = useMessage()
const taskId = Number(route.params.taskId)
const loading = ref(false)
const analytics = ref<TaskAnalyticsVO | null>(null)
const activeQuestion = ref<QuestionAnalyticsVO | null>(null)
const submissionChartRef = ref<HTMLDivElement | null>(null)
const accuracyChartRef = ref<HTMLDivElement | null>(null)
let submissionChart: ECharts | null = null
let accuracyChart: ECharts | null = null
const realtime = useRealtime()

const isWorksheet = computed(() => analytics.value?.type === 'worksheet')
const questionRows = computed(() => analytics.value?.questions ?? [])

const submissionColumns: DataTableColumns<StudentTaskAnswerVO> = [
  { title: '学号', key: 'studentNo', width: 120, render: row => row.studentNo || '-' },
  { title: '姓名', key: 'studentName', width: 120, render: row => row.studentName || '-' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h(NTag, { size: 'small', bordered: false, type: row.status === 'graded' ? 'success' : 'warning' }, () => statusLabel(row.status)),
  },
  { title: '提交时间', key: 'submittedAt', width: 170, render: row => row.submittedAt ? formatDate(row.submittedAt, 'datetime') : '-' },
]

const answerColumns: DataTableColumns<StudentAnswerVO> = [
  { title: '学号', key: 'studentNo', width: 120, render: row => row.studentNo || '-' },
  { title: '姓名', key: 'studentName', width: 120, render: row => row.studentName || '-' },
  { title: '作答', key: 'answer', render: row => answerText(row.answer) },
  {
    title: '结果',
    key: 'correct',
    width: 90,
    render: row => row.correct === null
      ? h(NTag, { size: 'small', bordered: false }, () => '未自动判定')
      : h(NTag, { size: 'small', bordered: false, type: row.correct ? 'success' : 'error' }, () => row.correct ? '正确' : '错误'),
  },
]

async function loadAnalytics(showToast = false) {
  loading.value = true
  try {
    analytics.value = await getTaskAnalytics(taskId)
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
    { name: '已提交', value: analytics.value.submittedCount },
    { name: '未提交', value: analytics.value.notSubmittedCount },
    { name: '特殊处理', value: analytics.value.specialCount },
  ]
  const option: DashboardChartOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['52%', '72%'],
      center: ['50%', '44%'],
      data,
      label: { formatter: '{b}: {c}' },
    }],
  }
  submissionChart.setOption(option, true)
}

function renderAccuracyChart() {
  if (!accuracyChartRef.value || !analytics.value) return
  if (!accuracyChart) accuracyChart = init(accuracyChartRef.value)
  const rows = analytics.value.questions.filter(item => item.autoGradable)
  const option: DashboardChartOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 24, bottom: 36 },
    xAxis: { type: 'category', data: rows.map(item => `第${item.index}题`) },
    yAxis: { type: 'value', max: 100 },
    series: [{
      type: 'bar',
      data: rows.map(item => item.accuracyRate),
      barMaxWidth: 28,
      itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] },
    }],
  }
  accuracyChart.setOption(option, true)
}

function statusLabel(status: string) {
  if (status === 'graded') return '已评分'
  if (status === 'special') return '特殊'
  return '已提交'
}

function answerText(value: unknown) {
  if (value == null || value === '') return '未作答'
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'boolean') return value ? '正确' : '错误'
  return String(value)
}

function optionRows(question: QuestionAnalyticsVO) {
  return Object.entries(question.optionDistribution)
}

function handleResize() {
  submissionChart?.resize()
  accuracyChart?.resize()
}

watch(analytics, renderCharts)

onMounted(async () => {
  await loadAnalytics()
  realtime.connect()
  setTimeout(() => realtime.subscribeTask(taskId, () => loadAnalytics()), 300)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  submissionChart?.dispose()
  accuracyChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="page analytics-page">
    <PageHeader title="任务数据看板" :hint="analytics?.title || '实时查看学生完成情况'">
      <template #actions>
        <NButton size="small" @click="router.back()">
          <template #icon><NIcon :size="14"><ArrowBackOutline /></NIcon></template>
          返回
        </NButton>
        <NButton size="small" type="primary" :loading="loading" @click="loadAnalytics(true)">
          <template #icon><NIcon :size="14"><RefreshOutline /></NIcon></template>
          刷新
        </NButton>
      </template>
    </PageHeader>

    <NSpin :show="loading && !analytics">
      <template v-if="analytics">
        <section class="metric-grid">
          <div class="metric">
            <span>提交率</span>
            <strong>{{ analytics.submissionRate }}%</strong>
            <NProgress type="line" :percentage="analytics.submissionRate" :show-indicator="false" />
          </div>
          <div class="metric">
            <span>已提交</span>
            <strong>{{ analytics.submittedCount }}/{{ analytics.totalStudents }}</strong>
            <p>{{ analytics.notSubmittedCount }} 人未提交</p>
          </div>
          <div class="metric">
            <span>自动题准确率</span>
            <strong>{{ isWorksheet ? `${analytics.accuracyRate}%` : '-' }}</strong>
            <p>{{ isWorksheet ? '按可自动判定题目统计' : '课堂作品仅统计提交率' }}</p>
          </div>
          <div class="metric">
            <span>实时连接</span>
            <strong>{{ realtime.connected.value ? '在线' : '连接中' }}</strong>
            <p>学生提交后自动刷新</p>
          </div>
        </section>

        <section class="chart-grid">
          <div class="panel">
            <div class="panel-head">
              <h3>提交构成</h3>
              <span>已提交 / 未提交 / 特殊</span>
            </div>
            <div ref="submissionChartRef" class="chart" />
          </div>
          <div class="panel">
            <div class="panel-head">
              <h3>{{ isWorksheet ? '单题准确率' : '作品提交概览' }}</h3>
              <span>{{ isWorksheet ? '自动批改题目' : '课堂作品无题目准确率' }}</span>
            </div>
            <div v-if="isWorksheet && questionRows.some(item => item.autoGradable)" ref="accuracyChartRef" class="chart" />
            <NEmpty v-else description="暂无可展示的自动题准确率" class="empty-chart" />
          </div>
        </section>

        <section v-if="isWorksheet" class="panel">
          <div class="panel-head">
            <h3>题目分析</h3>
            <span>点击单题查看学生答案</span>
          </div>
          <div class="question-list">
            <button v-for="question in questionRows" :key="question.questionId" class="question-row" type="button" @click="activeQuestion = question">
              <span class="question-index">第 {{ question.index }} 题</span>
              <span class="question-title">{{ question.stem || '未命名题目' }}</span>
              <NTag size="small" :bordered="false" :type="question.autoGradable ? 'success' : 'default'">
                {{ question.autoGradable ? `${question.accuracyRate}% 正确` : '手动批改' }}
              </NTag>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>{{ isWorksheet ? '提交明细' : '作品提交明细' }}</h3>
            <span>{{ analytics.submissions.length }} 条提交记录</span>
          </div>
          <NDataTable :data="analytics.submissions" :columns="submissionColumns" size="small" :row-key="row => row.submissionId" />
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
        <div v-if="optionRows(activeQuestion).length" class="option-bars">
          <div v-for="[option, count] in optionRows(activeQuestion)" :key="option" class="option-bar">
            <span>{{ option }}</span>
            <NProgress type="line" :percentage="analytics?.submittedCount ? Math.round(count * 1000 / analytics.submittedCount) / 10 : 0" :indicator-placement="'inside'" />
            <b>{{ count }}</b>
          </div>
        </div>
        <NDataTable :data="activeQuestion.answers" :columns="answerColumns" size="small" :row-key="row => row.submissionId" />
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.analytics-page { max-width: 1120px; margin: 0 auto; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.metric { min-height: 118px; padding: 16px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-color); }
.metric span { display: block; margin-bottom: 8px; font-size: 13px; color: var(--n-text-color-3); }
.metric strong { display: block; margin-bottom: 8px; font-size: 28px; line-height: 1.1; font-weight: 700; }
.metric p { margin: 0; font-size: 12px; color: var(--n-text-color-3); }
.chart-grid { display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr); gap: 16px; margin-bottom: 16px; }
.panel { padding: 16px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-color); margin-bottom: 16px; }
.panel-head { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; margin-bottom: 12px; }
.panel-head h3 { margin: 0; font-size: 16px; }
.panel-head span { font-size: 12px; color: var(--n-text-color-3); }
.chart { width: 100%; height: 280px; }
.empty-chart { height: 280px; display: flex; align-items: center; justify-content: center; }
.question-list { display: flex; flex-direction: column; gap: 8px; }
.question-row { width: 100%; min-height: 48px; display: grid; grid-template-columns: 86px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-color); color: var(--n-text-color); text-align: left; cursor: pointer; }
.question-row:hover { background: var(--n-color-embedded); }
.question-index { font-size: 13px; font-weight: 600; }
.question-title { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.question-modal { width: min(920px, calc(100vw - 32px)); }
.option-bars { display: grid; gap: 8px; margin: 14px 0; }
.option-bar { display: grid; grid-template-columns: minmax(80px, 160px) minmax(0, 1fr) 32px; align-items: center; gap: 10px; font-size: 13px; }
.option-bar b { text-align: right; }
@media (max-width: 900px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .metric-grid { grid-template-columns: 1fr; }
  .question-row { grid-template-columns: 1fr; }
  .option-bar { grid-template-columns: 1fr; }
}
</style>
