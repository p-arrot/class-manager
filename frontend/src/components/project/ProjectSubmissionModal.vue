<script setup lang="ts">
import { NButton, NEmpty, NIcon, NInput, NModal, NSpace } from 'naive-ui'
import { CloudDownloadOutline, EyeOutline } from '@vicons/ionicons5'
import { CORE_DIMENSIONS } from '@/types/taskSchema'
import { formatDate } from '@/utils/date'
import type { ArtifactFile } from '@/types/grading'
import type { DimensionScoreConfig } from '@/types/taskSchema'
import type { ProjectSubmissionRow } from '@/types/project'

const show = defineModel<boolean>('show', { required: true })

defineProps<{
  title: string
  rows: ProjectSubmissionRow[]
  rubric: DimensionScoreConfig[]
  getScore: (submissionId: number, dimension: string) => number
}>()

const emit = defineEmits<{
  preview: [file: ArtifactFile]
  download: [file: ArtifactFile]
  scoreChange: [submissionId: number, dimension: string, value: number | null]
  saveScore: [row: ProjectSubmissionRow]
}>()

function dimensionLabel(key: string) {
  return CORE_DIMENSIONS.find(item => item.key === key)?.label ?? key
}
</script>

<template>
  <NModal v-model:show="show" :title="title" preset="card" class="submissions-modal">
    <div v-if="rows.length" class="submission-list">
      <div v-for="row in rows" :key="row.id" class="submission-row">
        <div class="sub-head">
          <span class="sub-name">{{ row.studentName || '学生' }}</span>
          <span class="sub-no">{{ row.studentNo }}</span>
          <span class="sub-time">{{ row.submittedAt ? formatDate(row.submittedAt, 'datetime') : '-' }}</span>
        </div>
        <div v-if="row.parsed.note" class="sub-note">{{ row.parsed.note }}</div>
        <div v-if="row.parsed.files.length" class="sub-files">
          <div v-for="file in row.parsed.files" :key="file.id" class="sub-file">
            <span>{{ file.name }}</span>
            <NSpace :size="4">
              <NButton size="tiny" quaternary title="预览文件" aria-label="预览文件" @click="emit('preview', file)">
                <template #icon><NIcon><EyeOutline /></NIcon></template>
                预览
              </NButton>
              <NButton size="tiny" quaternary title="下载文件" aria-label="下载文件" @click="emit('download', file)">
                <template #icon><NIcon><CloudDownloadOutline /></NIcon></template>
                下载
              </NButton>
            </NSpace>
          </div>
        </div>
        <div class="submission-score-grid">
          <label v-for="dim in rubric.filter(item => item.maxScore > 0)" :key="dim.dimension" class="submission-score-cell">
            <span>{{ dimensionLabel(dim.dimension) }}</span>
            <NInput
              :value="String(getScore(row.id, dim.dimension))"
              size="small"
              @update:value="value => emit('scoreChange', row.id, dim.dimension, Number(value) || 0)"
            >
              <template #suffix>/ {{ dim.maxScore }}</template>
            </NInput>
          </label>
          <NButton size="small" type="primary" @click="emit('saveScore', row)">保存评分</NButton>
        </div>
      </div>
    </div>
    <NEmpty v-else description="暂无提交" />
  </NModal>
</template>

<style scoped>
.submissions-modal {
  width: min(720px, calc(100vw - 32px));
}
.submission-list { display: flex; flex-direction: column; gap: 10px; }
.submission-row { padding: 12px 14px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.sub-head { display: flex; align-items: center; gap: 8px; }
.sub-name { font-weight: 600; }
.sub-no, .sub-time { color: var(--n-text-color-3); font-size: 12px; }
.sub-note { margin-top: 8px; white-space: pre-wrap; color: var(--n-text-color-2); font-size: 13px; }
.sub-files { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.sub-file { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 10px; border-radius: 6px; background: var(--n-color-embedded); font-size: 13px; }
.submission-score-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)) auto; gap: 8px; align-items: end; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--n-border-color); }
.submission-score-cell { display: grid; gap: 4px; font-size: 12px; color: var(--n-text-color-2); }
@media (max-width: 720px) {
  .submission-score-grid { grid-template-columns: 1fr; }
}
</style>
