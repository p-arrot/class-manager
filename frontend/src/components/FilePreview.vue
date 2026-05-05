<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal, NSpin } from 'naive-ui'
import { getPreviewUrl } from '@/api/files'

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
  } catch (e: any) {
    error.value = e.message || '加载预览失败'
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
    style="width: 90vw; max-width: 1100px; height: 85vh; padding: 0;"
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

<style scoped>
.preview-body {
  width: 100%;
  height: calc(85vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 0 0 8px 8px;
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
