<script setup lang="ts">
import { NButton, NModal, NSpace, NUpload } from 'naive-ui'
import type { StudentImportResultVO } from '@/types/api'
import type { UploadFileInfo } from 'naive-ui'

const show = defineModel<boolean>('show', { required: true })

defineProps<{
  uploading: boolean
  result: StudentImportResultVO | null
}>()

const emit = defineEmits<{
  upload: [payload: { file: UploadFileInfo; fileList: UploadFileInfo[] }]
  close: []
}>()
</script>

<template>
  <NModal v-model:show="show" title="导入学生" preset="card" class="form-modal form-modal-wide">
    <NUpload accept=".xlsx,.xls" :max="1" :show-file-list="true" :default-upload="false" @change="payload => emit('upload', payload)">
      <NButton :loading="uploading">选择 Excel 文件</NButton>
    </NUpload>
    <p class="import-hint">表头须含：年级、班级、学号、姓名。每行独立处理。</p>
    <div v-if="result" class="import-result">
      <p>
        成功 <strong>{{ result.successCount }}</strong> 条，失败
        <strong class="error-text">{{ result.failCount }}</strong> 条
      </p>
      <div v-if="result.errors.length" class="error-list">
        <div v-for="(item, index) in result.errors" :key="index" class="error-item">
          <span>第 {{ item.rowNum }} 行</span>
          <span>{{ item.studentNo }} {{ item.name }}</span>
          <span class="error-text">{{ item.errorMsg }}</span>
        </div>
      </div>
    </div>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="emit('close')">关闭</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.form-modal {
  width: min(420px, calc(100vw - 32px));
}
.form-modal-wide {
  width: min(480px, calc(100vw - 32px));
}
.import-hint {
  font-size: 12px;
  color: var(--n-text-color-3);
  margin-top: 8px;
}
.import-result {
  margin-top: 16px;
  padding: 12px;
  background: var(--n-color-embedded);
  border-radius: 8px;
  font-size: 13px;
}
.error-list {
  max-height: 160px;
  overflow-y: auto;
  margin-top: 8px;
}
.error-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--n-border-color);
}
.error-text {
  color: var(--n-error-color);
}
</style>
