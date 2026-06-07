<script setup lang="ts">
import { NButton, NIcon, NSpace } from 'naive-ui'
import { CloudDownloadOutline, EyeOutline } from '@vicons/ionicons5'
import { formatFileSize } from '@/utils/validation'
import type { ArtifactFile } from '@/types/grading'

defineProps<{
  note?: string
  files: ArtifactFile[]
  fallbackContent?: string | null
}>()

const emit = defineEmits<{
  preview: [file: ArtifactFile]
  download: [file: ArtifactFile]
}>()
</script>

<template>
  <div class="artifact-result">
    <div v-if="note" class="content-text">{{ note }}</div>
    <div v-if="files.length" class="file-list">
      <div v-for="file in files" :key="file.id" class="file-row">
        <div>
          <div class="file-name">{{ file.name }}</div>
          <div class="file-meta">{{ file.type || 'FILE' }} · {{ formatFileSize(file.fileSize) }}</div>
        </div>
        <NSpace :size="6">
          <NButton size="tiny" quaternary aria-label="预览文件" @click="emit('preview', file)">
            <template #icon><NIcon><EyeOutline /></NIcon></template>
            预览
          </NButton>
          <NButton size="tiny" quaternary aria-label="下载文件" @click="emit('download', file)">
            <template #icon><NIcon><CloudDownloadOutline /></NIcon></template>
            下载
          </NButton>
        </NSpace>
      </div>
    </div>
    <div v-if="!note && !files.length" class="content-text">{{ fallbackContent || '无内容' }}</div>
  </div>
</template>

<style scoped>
.artifact-result { display: flex; flex-direction: column; gap: 12px; }
.content-text { padding: 12px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; font-size: 14px; min-height: 60px; white-space: pre-wrap; background: var(--n-color-embedded); }
.file-list { display: flex; flex-direction: column; gap: 8px; }
.file-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.file-name { font-size: 14px; font-weight: 600; }
.file-meta { margin-top: 2px; color: var(--n-text-color-3); font-size: 12px; }
@media (max-width: 720px) {
  .file-row { align-items: flex-start; flex-direction: column; }
}
</style>
