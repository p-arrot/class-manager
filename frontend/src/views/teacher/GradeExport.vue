<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { NButton, NSelect, NEmpty, NTag, NCard, useMessage } from 'naive-ui'
import PageHeader from '@/components/PageHeader.vue'
import { exportSemesterStats, getSemesterStatsPreview } from '@/api/stats'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { getErrorMessage } from '@/utils/error'
import type { SemesterStatsPreviewRow } from '@/types/api'

const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const preview = ref<SemesterStatsPreviewRow[]>([])
const loading = ref(false)

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
  if (!activeSemesterId.value) return
  try {
    const r = await exportSemesterStats(activeSemesterId.value)
    const url = URL.createObjectURL(r)
    const a = document.createElement('a')
    a.href = url
    a.download = '学期总评.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error(getErrorMessage(e, '导出失败'))
  }
}

function gradeLabel(s: number | null): string {
  if (s == null) return '-'
  if (s >= 90) return 'A'
  if (s >= 75) return 'B'
  if (s >= 60) return 'C'
  if (s >= 40) return 'D'
  return 'E'
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
    <PageHeader title="成绩导出" subtitle="导出学期总评 Excel">
      <template #actions>
        <NButton size="small" type="primary" :disabled="!activeSemesterId" @click="handleExport">导出 Excel</NButton>
      </template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" :disabled="!activeCourseId" />
    </div>
    <NEmpty v-if="!activeSemesterId" description="请选择课程和学期" />
    <div v-else-if="preview.length" class="preview-list">
      <NCard v-for="row in preview" :key="row.studentId" size="small" class="preview-card">
        <div class="row-info">
          <span class="row-name">{{ row.studentName }}</span>
          <span class="row-no">{{ row.studentNo }}</span>
          <NTag size="tiny" :type="(row.totalScore ?? 0) >= 60 ? 'success' : 'error'" :bordered="false">{{ gradeLabel(row.totalScore) }}</NTag>
          <span class="row-score">{{ row.totalScore?.toFixed(1) ?? '-' }} 分</span>
        </div>
      </NCard>
    </div>
    <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" />
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.toolbar { margin: 16px 0 24px; display: flex; gap: 12px; flex-wrap: wrap; }
.toolbar-select { width: 220px; }
.preview-list { display: flex; flex-direction: column; gap: 8px; }
.preview-card { padding: 12px 16px; }
.row-info { display: flex; align-items: center; gap: 12px; }
.row-name { font-size: 14px; font-weight: 600; }
.row-no { font-size: 12px; color: var(--n-text-color-3); }
.row-score { font-size: 14px; font-weight: 600; margin-left: auto; }
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
}
</style>
