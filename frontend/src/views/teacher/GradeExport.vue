<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { NAlert, NButton, NDataTable, NEmpty, NIcon, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { DownloadOutline, RefreshOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import { exportSemesterStats, getSemesterStatsPreview } from '@/api/stats'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { getErrorMessage } from '@/utils/error'
import type { SemesterStatsPreviewRow } from '@/types/api'

const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const preview = ref<SemesterStatsPreviewRow[]>([])
const loading = ref(false)
const exporting = ref(false)

const completeRows = computed(() => preview.value.filter(row => row.totalScore != null && !row.remark))
const missingRows = computed(() => preview.value.filter(row => row.remark))
const averageTotal = computed(() => {
  const scoredRows = completeRows.value.filter(row => row.totalScore != null)
  if (!scoredRows.length) return null
  const sum = scoredRows.reduce((total, row) => total + (row.totalScore ?? 0), 0)
  return sum / scoredRows.length
})
const missingSummary = computed(() => {
  const counts = new Map<string, number>()
  for (const row of missingRows.value) {
    const key = row.remark || '其他缺失'
    counts.set(key, (counts.get(key) ?? 0) + 1)
  }
  return Array.from(counts.entries()).map(([label, count]) => ({ label, count }))
})

const columns: DataTableColumns<SemesterStatsPreviewRow> = [
  { title: '班级', key: 'className', width: 110, fixed: 'left', render: row => row.className || '-' },
  { title: '学号', key: 'studentNo', width: 120, fixed: 'left' },
  { title: '姓名', key: 'studentName', width: 100, fixed: 'left' },
  { title: '意识', key: 'awareness', width: 82, align: 'right', render: row => scoreText(row.awareness) },
  { title: '计算', key: 'computing', width: 82, align: 'right', render: row => scoreText(row.computing) },
  { title: '学习', key: 'digitalLearn', width: 82, align: 'right', render: row => scoreText(row.digitalLearn) },
  { title: '责任', key: 'responsibility', width: 82, align: 'right', render: row => scoreText(row.responsibility) },
  { title: '过程分', key: 'processScore', width: 90, align: 'right', render: row => scoreText(row.processScore) },
  { title: '考试分', key: 'examScore', width: 90, align: 'right', render: row => scoreText(row.examScore) },
  { title: '项目分', key: 'projectScore', width: 90, align: 'right', render: row => scoreText(row.projectScore) },
  { title: '结果分', key: 'resultScore', width: 90, align: 'right', render: row => scoreText(row.resultScore) },
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
      : h('span', { class: 'muted-text' }, '可导出'),
  },
]

async function loadPreview() {
  if (!activeSemesterId.value) {
    preview.value = []
    return
  }
  loading.value = true
  try {
    preview.value = await getSemesterStatsPreview(activeSemesterId.value)
  } catch (e) {
    preview.value = []
    message.error(getErrorMessage(e, '加载总评预览失败'))
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  if (!activeSemesterId.value || exporting.value) return
  exporting.value = true
  try {
    const blob = await exportSemesterStats(activeSemesterId.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学期总评.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error(getErrorMessage(e, '导出失败'))
  } finally {
    exporting.value = false
  }
}

function scoreText(score: number | null | undefined): string {
  return score == null ? '-' : score.toFixed(1)
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
    await loadCourses()
  } catch (e) {
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
})

watch(activeSemesterId, loadPreview)
</script>

<template>
  <div class="page">
    <PageHeader title="成绩导出" subtitle="预览并导出学期总评">
      <template #actions>
        <NButton
          size="small"
          type="primary"
          :loading="exporting"
          :disabled="!activeSemesterId || loading"
          @click="handleExport"
        >
          <template #icon>
            <NIcon><DownloadOutline /></NIcon>
          </template>
          导出 Excel
        </NButton>
      </template>
    </PageHeader>

    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect
        v-model:value="activeSemesterId"
        :options="semesterOptions"
        placeholder="选择学期"
        class="toolbar-select"
        :disabled="!activeCourseId"
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
            <span class="summary-label">学生数</span>
            <strong>{{ preview.length }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">可生成总评</span>
            <strong>{{ completeRows.length }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">缺失数据</span>
            <strong>{{ missingRows.length }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">平均总评</span>
            <strong>{{ averageTotal == null ? '-' : averageTotal.toFixed(1) }}</strong>
          </div>
        </div>

        <NAlert v-if="missingSummary.length" type="warning" :bordered="false" class="missing-alert">
          <div class="missing-list">
            <span v-for="item in missingSummary" :key="item.label">{{ item.label }}：{{ item.count }} 人</span>
          </div>
        </NAlert>

        <NDataTable
          v-if="preview.length"
          :columns="columns"
          :data="preview"
          :row-key="row => row.studentId"
          size="small"
          :bordered="false"
          :single-line="false"
          :scroll-x="1320"
          class="grade-table"
        />
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
  width: 240px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-item {
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-card-color);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.summary-label {
  font-size: 12px;
  color: var(--n-text-color-3);
}

.summary-item strong {
  font-size: 22px;
  line-height: 1.2;
  font-weight: 700;
  color: var(--n-text-color-1);
}

.missing-alert {
  margin-bottom: 12px;
}

.missing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
}

.grade-table {
  margin-top: 4px;
}

.muted-text {
  color: var(--n-text-color-3);
}

.empty-state {
  padding: 48px 0;
}

@media (max-width: 720px) {
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
