<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { NAlert, NButton, NDataTable, NEmpty, NIcon, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import RadarChart from '@/components/RadarChart.vue'
import { listAllClasses } from '@/api/classes'
import { getSemesterStatsPreview } from '@/api/stats'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { useClassFilterStore } from '@/stores/classFilter'
import { getErrorMessage } from '@/utils/error'
import type { ClassVO, SemesterStatsPreviewRow } from '@/types/api'

interface ClassAggregate {
  className: string
  studentCount: number
  completeCount: number
  missingCount: number
  awareness: number | null
  computing: number | null
  digitalLearn: number | null
  responsibility: number | null
  processScore: number | null
  examScore: number | null
  projectScore: number | null
  resultScore: number | null
  totalScore: number | null
  excellentRate: number | null
  passRate: number | null
}

const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const classFilter = useClassFilterStore()
const preview = ref<SemesterStatsPreviewRow[]>([])
const classes = ref<ClassVO[]>([])
const loading = ref(false)
const selectedClass = ref<string | null>(null)
let syncingClassFromStore = false

const classOptions = computed(() => classAggregates.value.map(item => ({
  label: item.className,
  value: item.className,
})))

const filteredRows = computed(() => selectedClass.value
  ? preview.value.filter(row => normalizedClassName(row) === selectedClass.value)
  : preview.value)

const completeRows = computed(() => filteredRows.value.filter(row => row.totalScore != null && !row.remark))
const missingRows = computed(() => filteredRows.value.filter(row => row.remark))
const averageTotal = computed(() => average(completeRows.value, row => row.totalScore))
const excellentRate = computed(() => rate(completeRows.value.filter(row => (row.totalScore ?? 0) >= 75).length, filteredRows.value.length))
const passRate = computed(() => rate(completeRows.value.filter(row => (row.totalScore ?? 0) >= 60).length, filteredRows.value.length))

const classAggregates = computed<ClassAggregate[]>(() => {
  const groups = new Map<string, SemesterStatsPreviewRow[]>()
  for (const row of preview.value) {
    const key = normalizedClassName(row)
    groups.set(key, [...(groups.get(key) ?? []), row])
  }
  return Array.from(groups.entries())
    .map(([className, rows]) => buildClassAggregate(className, rows))
    .sort((a, b) => a.className.localeCompare(b.className, 'zh-CN'))
})

const radarPoints = computed(() => [
  { dimension: 'AWARENESS', label: '信息意识', avgScore: average(filteredRows.value, row => row.awareness) ?? 0 },
  { dimension: 'COMPUTING', label: '计算思维', avgScore: average(filteredRows.value, row => row.computing) ?? 0 },
  { dimension: 'DIGITAL_LEARNING', label: '数字化学习与创新', avgScore: average(filteredRows.value, row => row.digitalLearn) ?? 0 },
  { dimension: 'RESPONSIBILITY', label: '信息社会责任', avgScore: average(filteredRows.value, row => row.responsibility) ?? 0 },
])

const aggregateColumns: DataTableColumns<ClassAggregate> = [
  { title: '班级', key: 'className', width: 120, fixed: 'left' },
  { title: '学生数', key: 'studentCount', width: 80, align: 'right' },
  { title: '完整总评', key: 'completeCount', width: 90, align: 'right' },
  { title: '缺失', key: 'missingCount', width: 80, align: 'right' },
  { title: '意识', key: 'awareness', width: 82, align: 'right', render: row => scoreText(row.awareness) },
  { title: '计算', key: 'computing', width: 82, align: 'right', render: row => scoreText(row.computing) },
  { title: '学习', key: 'digitalLearn', width: 82, align: 'right', render: row => scoreText(row.digitalLearn) },
  { title: '责任', key: 'responsibility', width: 82, align: 'right', render: row => scoreText(row.responsibility) },
  { title: '过程分', key: 'processScore', width: 90, align: 'right', render: row => scoreText(row.processScore) },
  { title: '考试分', key: 'examScore', width: 90, align: 'right', render: row => scoreText(row.examScore) },
  { title: '项目分', key: 'projectScore', width: 90, align: 'right', render: row => scoreText(row.projectScore) },
  { title: '结果分', key: 'resultScore', width: 90, align: 'right', render: row => scoreText(row.resultScore) },
  { title: '平均总评', key: 'totalScore', width: 100, align: 'right', sorter: (a, b) => (a.totalScore ?? -1) - (b.totalScore ?? -1), render: row => scoreText(row.totalScore) },
  { title: '优秀率', key: 'excellentRate', width: 90, align: 'right', render: row => percentText(row.excellentRate) },
  { title: '合格率', key: 'passRate', width: 90, align: 'right', render: row => percentText(row.passRate) },
]

const studentColumns: DataTableColumns<SemesterStatsPreviewRow> = [
  { title: '班级', key: 'className', width: 110, fixed: 'left', render: row => row.className || '-' },
  { title: '学号', key: 'studentNo', width: 120, fixed: 'left' },
  { title: '姓名', key: 'studentName', width: 100, fixed: 'left' },
  { title: '意识', key: 'awareness', width: 82, align: 'right', render: row => scoreText(row.awareness) },
  { title: '计算', key: 'computing', width: 82, align: 'right', render: row => scoreText(row.computing) },
  { title: '学习', key: 'digitalLearn', width: 82, align: 'right', render: row => scoreText(row.digitalLearn) },
  { title: '责任', key: 'responsibility', width: 82, align: 'right', render: row => scoreText(row.responsibility) },
  { title: '总评', key: 'totalScore', width: 90, align: 'right', sorter: (a, b) => (a.totalScore ?? -1) - (b.totalScore ?? -1), render: row => scoreText(row.totalScore) },
  {
    title: '等级',
    key: 'totalGrade',
    width: 82,
    render: row => row.totalGrade && row.totalGrade !== '暂无数据'
      ? h(NTag, { size: 'small', type: gradeTagType(row.totalScore), bordered: false }, () => row.totalGrade)
      : '-',
  },
  {
    title: '备注',
    key: 'remark',
    minWidth: 150,
    render: row => row.remark
      ? h(NTag, { size: 'small', type: 'warning', bordered: false }, () => row.remark)
      : h('span', { class: 'muted-text' }, '完整'),
  },
]

async function loadPreview() {
  if (!activeSemesterId.value) {
    preview.value = []
    selectedClass.value = null
    return
  }
  loading.value = true
  try {
    preview.value = await getSemesterStatsPreview(activeSemesterId.value)
    if (selectedClass.value && !classOptions.value.some(item => item.value === selectedClass.value)) {
      selectedClass.value = null
    }
  } catch (e) {
    preview.value = []
    selectedClass.value = null
    message.error(getErrorMessage(e, '加载班级分析数据失败'))
  } finally {
    loading.value = false
  }
}

async function loadTeacherClasses() {
  classes.value = await listAllClasses()
  applyGlobalClassFilter()
}

function buildClassAggregate(className: string, rows: SemesterStatsPreviewRow[]): ClassAggregate {
  const complete = rows.filter(row => row.totalScore != null && !row.remark)
  return {
    className,
    studentCount: rows.length,
    completeCount: complete.length,
    missingCount: rows.filter(row => row.remark).length,
    awareness: average(rows, row => row.awareness),
    computing: average(rows, row => row.computing),
    digitalLearn: average(rows, row => row.digitalLearn),
    responsibility: average(rows, row => row.responsibility),
    processScore: average(rows, row => row.processScore),
    examScore: average(rows, row => row.examScore),
    projectScore: average(rows, row => row.projectScore),
    resultScore: average(rows, row => row.resultScore),
    totalScore: average(complete, row => row.totalScore),
    excellentRate: rate(complete.filter(row => (row.totalScore ?? 0) >= 75).length, rows.length),
    passRate: rate(complete.filter(row => (row.totalScore ?? 0) >= 60).length, rows.length),
  }
}

function normalizedClassName(row: SemesterStatsPreviewRow) {
  return row.className || '未分班'
}

function classLabel(row: ClassVO) {
  return `${row.grade}级${row.name}`
}

function applyGlobalClassFilter() {
  syncingClassFromStore = true
  const globalClassId = classFilter.selectedClassId
  const className = globalClassId
    ? classes.value.find(item => item.id === globalClassId)
    : null
  selectedClass.value = className ? classLabel(className) : null
  queueMicrotask(() => {
    syncingClassFromStore = false
  })
}

function syncSelectedClassToStore(className: string | null) {
  if (syncingClassFromStore) return
  if (!className) {
    classFilter.clearFilter()
    return
  }
  const match = classes.value.find(item => classLabel(item) === className)
  if (match) classFilter.setClassId(match.id)
}

function average<T>(rows: T[], getter: (row: T) => number | null | undefined) {
  const values = rows.map(getter).filter((value): value is number => value != null)
  if (!values.length) return null
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function rate(count: number, total: number) {
  if (!total) return null
  return count / total
}

function scoreText(score: number | null | undefined): string {
  return score == null ? '-' : score.toFixed(1)
}

function percentText(value: number | null | undefined) {
  return value == null ? '-' : `${(value * 100).toFixed(0)}%`
}

function gradeTagType(score: number | null | undefined) {
  if (score == null) return 'default'
  if (score >= 75) return 'success'
  if (score >= 60) return 'info'
  if (score >= 40) return 'warning'
  return 'error'
}

onMounted(async () => {
  try {
    await Promise.all([loadCourses(), loadTeacherClasses()])
  } catch (e) {
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
})

watch(activeSemesterId, loadPreview)
watch(() => classFilter.selectedClassId, applyGlobalClassFilter)
watch(selectedClass, syncSelectedClassToStore)
</script>

<template>
  <div class="page">
    <PageHeader title="班级数据分析" subtitle="按班级复盘学期表现、缺失数据和核心素养维度" />

    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect
        v-model:value="activeSemesterId"
        :options="semesterOptions"
        placeholder="选择学期"
        class="toolbar-select"
        :disabled="!activeCourseId"
      />
      <NSelect
        v-model:value="selectedClass"
        :options="classOptions"
        placeholder="全部班级"
        class="toolbar-select"
        :disabled="!preview.length"
        clearable
      />
      <NButton size="small" secondary :disabled="!activeSemesterId || loading" @click="loadPreview">
        <template #icon>
          <NIcon><RefreshOutline /></NIcon>
        </template>
        刷新
      </NButton>
    </div>

    <NSpin :show="loading">
      <NEmpty v-if="!activeSemesterId" description="请选择课程和学期" class="empty-state" />
      <template v-else>
        <div class="summary-grid">
          <div class="summary-item">
            <span>学生数</span>
            <strong>{{ filteredRows.length }}</strong>
          </div>
          <div class="summary-item">
            <span>完整总评</span>
            <strong>{{ completeRows.length }}</strong>
          </div>
          <div class="summary-item">
            <span>缺失数据</span>
            <strong>{{ missingRows.length }}</strong>
          </div>
          <div class="summary-item">
            <span>平均总评</span>
            <strong>{{ averageTotal == null ? '-' : averageTotal.toFixed(1) }}</strong>
          </div>
          <div class="summary-item">
            <span>优秀率</span>
            <strong>{{ percentText(excellentRate) }}</strong>
          </div>
          <div class="summary-item">
            <span>合格率</span>
            <strong>{{ percentText(passRate) }}</strong>
          </div>
        </div>

        <NAlert v-if="missingRows.length" type="warning" :bordered="false" class="section-gap">
          当前筛选范围内有 {{ missingRows.length }} 名学生缺少总评数据，班级均分和通过率只统计已完成总评的学生。
        </NAlert>

        <div v-if="preview.length" class="analysis-grid">
          <section class="chart-panel">
            <div class="section-head">
              <h2>{{ selectedClass || '全部班级' }}维度雷达</h2>
              <span>四项核心素养平均分</span>
            </div>
            <RadarChart :current="radarPoints" :has-previous="false" />
          </section>

          <section class="table-panel">
            <div class="section-head">
              <h2>班级对比</h2>
              <span>{{ classAggregates.length }} 个班级</span>
            </div>
            <NDataTable
              :columns="aggregateColumns"
              :data="classAggregates"
              :row-key="row => row.className"
              size="small"
              :bordered="false"
              :single-line="false"
              :scroll-x="1320"
            />
          </section>
        </div>

        <section v-if="preview.length" class="student-panel">
          <div class="section-head">
            <h2>学生明细</h2>
            <span>{{ filteredRows.length }} 名学生</span>
          </div>
          <NDataTable
            :columns="studentColumns"
            :data="filteredRows"
            :row-key="row => row.studentId"
            size="small"
            :bordered="false"
            :single-line="false"
            :scroll-x="980"
          />
        </section>

        <NEmpty v-else description="暂无评价数据" class="empty-state" />
      </template>
    </NSpin>
  </div>
</template>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 220px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-item {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-card-color);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.summary-item span,
.section-head span,
.muted-text {
  color: var(--n-text-color-3);
}

.summary-item span {
  font-size: 12px;
}

.summary-item strong {
  font-size: 22px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--n-text-color-1);
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.4fr);
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}

.chart-panel,
.table-panel,
.student-panel {
  min-width: 0;
}

.student-panel {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
  font-weight: 700;
}

.section-head span {
  font-size: 13px;
}

.section-gap {
  margin-bottom: 12px;
}

.empty-state {
  padding: 48px 0;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .analysis-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .toolbar {
    align-items: stretch;
  }

  .toolbar-select {
    width: 100%;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
