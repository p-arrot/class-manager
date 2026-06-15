<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal, NSpin } from 'naive-ui'
import { getPreviewUrl } from '@/api/files'
import { getErrorMessage } from '@/utils/error'

const props = defineProps<{
  resourceId: number | null
  fileName?: string
}>()

const emit = defineEmits<{
  close: []
}>()

const loading = ref(false)
const previewUrl = ref('')
const error = ref('')

watch(() => props.resourceId, async (id) => {
  if (id === null) {
    previewUrl.value = ''
    error.value = ''
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getPreviewUrl(id)
    previewUrl.value = data.url
  } catch (e) {
    error.value = getErrorMessage(e, '加载预览失败')
  } finally {
    loading.value = false
  }
})

function handleClose() {
  previewUrl.value = ''
  error.value = ''
  emit('close')
}
</script>

<template>
  <NModal
    :show="resourceId !== null"
    preset="card"
    :title="fileName || '文件预览'"
    class="file-preview-modal"
    :on-close="handleClose"
    :bordered="false"
  >
    <div class="preview-body">
      <NSpin v-if="loading" class="preview-loading" />
      <div v-else-if="error" class="preview-error">{{ error }}</div>
      <iframe
        v-else-if="previewUrl"
        :src="previewUrl"
        class="preview-iframe"
        frameborder="0"
      />
      <div v-else class="preview-empty">无法预览</div>
    </div>
  </NModal>
</template>

<style>
.file-preview-modal.n-card {
  display: flex !important;
  flex-direction: column !important;
  width: min(1100px, 90vw) !important;
  height: min(85vh, 900px) !important;
  max-height: calc(100vh - 48px) !important;
  overflow: hidden !important;
}
.file-preview-modal .n-card__content {
  padding: 0 !important;
  flex: 1 !important;
  min-height: 0 !important;
  overflow: hidden !important;
}
</style>

<style scoped>
.preview-body {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.preview-error {
  color: var(--n-text-color-3);
  font-size: 14px;
}
.preview-empty {
  color: var(--n-text-color-3);
  font-size: 14px;
}
</style>
