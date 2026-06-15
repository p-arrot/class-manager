<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NEmpty, NIcon, NModal, NPopconfirm, NSpin } from 'naive-ui'
import { CloudDownloadOutline, DocumentOutline, EyeOutline, FolderOutline, TrashOutline } from '@vicons/ionicons5'
import { formatFileSize } from '@/utils/validation'
import type { DriveItemVO } from '@/types/api'

const show = defineModel<boolean>('show', { required: true })
const showPreview = defineModel<boolean>('showPreview', { required: true })

const props = defineProps<{
  loading: boolean
  items: DriveItemVO[]
  breadcrumb: DriveItemVO[]
  previewUrl: string
  previewName: string
}>()

const emit = defineEmits<{
  back: [index: number]
  enterFolder: [item: DriveItemVO]
  download: [item: DriveItemVO]
  preview: [item: DriveItemVO]
  delete: [itemId: number]
}>()

const totalSize = computed(() => formatFileSize(props.items.reduce((sum, item) => sum + (item.fileSize || 0), 0)))

function rowClass(item: DriveItemVO) {
  return ['drive-row', item.type === 'FOLDER' ? 'is-folder' : ''].filter(Boolean).join(' ')
}
</script>

<template>
  <NModal v-model:show="show" preset="card" title="学生网盘管理" class="drive-modal">
    <NSpin :show="loading">
      <div v-if="breadcrumb.length" class="breadcrumb">
        <NButton text size="tiny" @click="emit('back', 0)">根目录</NButton>
        <template v-for="(item, index) in breadcrumb" :key="item.id">
          <span class="breadcrumb-separator">/</span>
          <NButton text size="tiny" @click="emit('back', index + 1)">{{ item.name }}</NButton>
        </template>
      </div>

      <div class="drive-summary">总占用：{{ totalSize }}</div>

      <div v-if="items.length" class="drive-list">
        <div
          v-for="item in items"
          :key="item.id"
          :class="rowClass(item)"
          @click="item.type === 'FOLDER' ? emit('enterFolder', item) : undefined"
        >
          <NIcon :size="16" :color="item.type === 'FOLDER' ? '#F97316' : '#6b6b65'">
            <FolderOutline v-if="item.type === 'FOLDER'" />
            <DocumentOutline v-else />
          </NIcon>
          <span class="drive-name">{{ item.name }}</span>
          <span class="drive-size">{{ item.fileSize ? formatFileSize(item.fileSize) : '' }}</span>
          <NButton
            v-if="item.type === 'FILE'"
            size="tiny"
            quaternary
            title="下载"
            aria-label="下载"
            @click.stop="emit('download', item)"
          >
            <template #icon><NIcon :size="14"><CloudDownloadOutline /></NIcon></template>
          </NButton>
          <NButton
            v-if="item.type === 'FILE'"
            size="tiny"
            quaternary
            title="预览"
            aria-label="预览"
            @click.stop="emit('preview', item)"
          >
            <template #icon><NIcon :size="14"><EyeOutline /></NIcon></template>
          </NButton>
          <NPopconfirm @positive-click="emit('delete', item.id)">
            <template #trigger>
              <NButton size="tiny" quaternary type="error" title="删除" aria-label="删除" @click.stop>
                <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
              </NButton>
            </template>
            确认删除？
          </NPopconfirm>
        </div>
      </div>
      <NEmpty v-else description="网盘为空" />
    </NSpin>
  </NModal>

  <NModal v-model:show="showPreview" :title="previewName" preset="card" class="preview-modal">
    <iframe v-if="previewUrl" :src="previewUrl" class="preview-frame" />
  </NModal>
</template>

<style scoped>
.drive-modal {
  width: min(640px, calc(100vw - 32px));
  max-height: 80vh;
}
.breadcrumb {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  flex-wrap: wrap;
}
.breadcrumb-separator,
.drive-summary,
.drive-size {
  color: var(--n-text-color-3);
}
.drive-summary {
  font-size: 13px;
  margin-bottom: 12px;
}
.drive-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.drive-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid var(--n-border-color);
}
.drive-row.is-folder {
  cursor: pointer;
}
.drive-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.drive-size {
  font-size: 11px;
  flex: 0 0 auto;
}
.preview-modal {
  width: 90vw;
  max-width: 900px;
  height: 80vh;
}
.preview-frame {
  width: 100%;
  height: calc(80vh - 60px);
  border: none;
  border-radius: 0 0 8px 8px;
}
</style>
