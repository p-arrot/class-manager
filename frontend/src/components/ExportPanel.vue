<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NEmpty, NButton, NSelect, NTag, NCard, useMessage } from 'naive-ui'
import { listSemesters } from '@/api/semesters'
import { exportSemesterStats, getSemesterStatsPreview } from '@/api/stats'
import type { SemesterStatsPreviewRow, SemesterVO } from '@/types/api'
import { getErrorMessage } from '@/utils/error'

const props = defineProps<{ courseId: number }>()
const message = useMessage()
const semesters = ref<SemesterVO[]>([])
const activeSemesterId = ref<number | null>(null)
const preview = ref<SemesterStatsPreviewRow[]>([])
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

watch(() => props.courseId, async () => {
  try {
    semesters.value = await listSemesters(props.courseId) || []
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
}, { immediate: true })
watch(activeSemesterId, async () => {
  if (!activeSemesterId.value) {
    preview.value = []
    return
  }
  try {
    preview.value = await getSemesterStatsPreview(activeSemesterId.value) || []
  } catch (e) {
    preview.value = []
    message.error(getErrorMessage(e, '加载总评预览失败'))
  }
})

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

function gradeLabel(s: number): string {
  if (s >= 90) return 'A'
  if (s >= 75) return 'B'
  if (s >= 60) return 'C'
  if (s >= 40) return 'D'
  return 'E'
}
</script>

<template>
  <div>
    <div class="export-toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="semester-select" />
      <NButton v-if="activeSemesterId" size="small" type="primary" @click="handleExport">导出 Excel</NButton>
    </div>
    <div v-if="preview.length" class="preview-list">
      <NCard v-for="row in preview" :key="row.studentId" size="small" class="preview-card">
        <div class="preview-row">
          <span class="student-name">{{ row.studentName }}</span>
          <span class="student-no">{{ row.studentNo }}</span>
          <NTag size="tiny" :type="(row.totalScore ?? 0) >= 60 ? 'success' : 'error'" :bordered="false">{{ row.totalScore == null ? '-' : gradeLabel(row.totalScore) }}</NTag>
          <span class="total-score">{{ row.totalScore?.toFixed(1) || '-' }} 分</span>
        </div>
      </NCard>
    </div>
    <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" class="empty-state" />
  </div>
</template>

<style scoped>
.export-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.semester-select { width: 200px; }
.preview-list { display: flex; flex-direction: column; gap: 8px; }
.preview-card { padding: 12px 16px; }
.preview-row { display: flex; align-items: center; gap: 12px; }
.student-name { font-size: 14px; font-weight: 600; }
.student-no { font-size: 12px; color: var(--n-text-color-3); }
.total-score { font-size: 14px; font-weight: 600; margin-left: auto; }
.empty-state { padding: 40px 0; }
</style>
