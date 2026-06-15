<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NEmpty, NButton, NIcon, NSpace, NModal, NInput, useMessage, NPopconfirm, NSpin } from 'naive-ui'
import { TrashOutline, FolderOutline, DocumentOutline, CloudDownloadOutline, EyeOutline, SearchOutline } from '@vicons/ionicons5'
import { createDriveFolder, deleteDriveItem, getDrivePreview, getDriveRaw, listDriveItems, uploadDriveFile } from '@/api/drive'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'
import { formatFileSize } from '@/utils/validation'
import { getErrorMessage } from '@/utils/error'
import type { DriveItemVO } from '@/types/api'

const message = useMessage()
const items = ref<DriveItemVO[]>([])
const parentId = ref<number | null>(null)
const breadcrumb = ref<DriveItemVO[]>([])
const showNewFolder = ref(false)
const folderName = ref('')
const searchQuery = ref('')
const loading = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const showPreview = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)

const filteredItems = computed(() => {
  if (!searchQuery.value.trim()) return items.value
  const q = searchQuery.value.toLowerCase()
  return items.value.filter(i => i.name.toLowerCase().includes(q))
})

async function loadItems() {
  loading.value = true
  try {
    items.value = await listDriveItems({ parentId: parentId.value })
  } catch (e) {
    items.value = []
    message.error(getErrorMessage(e, '加载网盘列表失败'))
  } finally {
    loading.value = false
  }
}

async function enterFolder(item: DriveItemVO) {
  breadcrumb.value.push(item)
  parentId.value = item.id
  await loadItems()
}

async function goBack(idx: number) {
  breadcrumb.value = breadcrumb.value.slice(0, idx)
  parentId.value = idx > 0 ? breadcrumb.value[idx - 1].id : null
  await loadItems()
}

async function createFolder() {
  try {
    await createDriveFolder({ name: folderName.value, parentId: parentId.value })
    message.success('文件夹已创建')
    showNewFolder.value = false
    folderName.value = ''
    await loadItems()
  } catch (e) {
    message.error(getErrorMessage(e, '创建失败'))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteDriveItem(id)
    message.success('已删除')
    await loadItems()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

const uploading = ref(false)
const uploadQueue = ref(0)

async function uploadFile(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  if (parentId.value) fd.append('parentId', String(parentId.value))
  await uploadDriveFile(fd)
}

async function processFiles(files: FileList | File[]) {
  uploading.value = true
  uploadQueue.value = files.length
  let success = 0
  let fail = 0
  let lastError = ''
  for (const file of Array.from(files)) {
    try {
      await uploadFile(file)
      success++
    } catch (e) {
      fail++
      lastError = getErrorMessage(e, '上传失败')
    }
    uploadQueue.value--
  }
  if (fail) {
    message.warning(`${success} 个成功，${fail} 个失败。${lastError}`)
  } else {
    message.success(`${success} 个文件上传成功`)
  }
  uploading.value = false
  await loadItems()
}

async function handleUpload(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files?.length) return
  await processFiles(files)
}

function triggerUpload() {
  fileInput.value?.click()
}

function triggerFolder() {
  folderInput.value?.click()
}

async function handleDownload(item: DriveItemVO) {
  try {
    const blob = await getDriveRaw(item.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = item.name
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
}

async function handlePreview(item: DriveItemVO) {
  try {
    const r = await getDrivePreview(item.id)
    if (r?.url) {
      previewUrl.value = r.url
      previewName.value = item.name
      showPreview.value = true
    }
  } catch (e) {
    message.error(getErrorMessage(e, '预览失败'))
  }
}

onMounted(loadItems)
</script>

<template>
  <div class="page">
    <PageHeader title="我的网盘" subtitle="管理个人文件">
      <template #actions>
        <NButton size="small" @click="showNewFolder = true">
          <template #icon><NIcon :size="14"><FolderOutline /></NIcon></template>新建文件夹
        </NButton>
        <NButton size="small" class="header-action" @click="triggerUpload">
          <template #icon><NIcon :size="14"><DocumentOutline /></NIcon></template>上传文件
        </NButton>
        <NButton size="small" class="header-action" @click="triggerFolder">
          <template #icon><NIcon :size="14"><FolderOutline /></NIcon></template>上传文件夹
        </NButton>
        <input ref="fileInput" type="file" multiple class="hidden-input" @change="handleUpload" />
        <input ref="folderInput" type="file" webkitdirectory multiple class="hidden-input" @change="handleUpload" />
      </template>
    </PageHeader>

    <div class="breadcrumb" v-if="breadcrumb.length">
      <NButton text size="tiny" @click="goBack(0)">根目录</NButton>
      <span v-for="(b, i) in breadcrumb" :key="b.id">
        <span class="crumb-sep">/</span>
        <NButton text size="tiny" @click="goBack(i + 1)">{{ b.name }}</NButton>
      </span>
    </div>

    <div class="drive-toolbar">
      <NInput v-model:value="searchQuery" placeholder="搜索文件..." clearable class="search-input">
        <template #prefix><NIcon :size="14"><SearchOutline /></NIcon></template>
      </NInput>
    </div>

    <div v-if="uploading" class="upload-status">
      <span>{{ uploadQueue }} 个文件待上传...</span>
    </div>

    <NSpin :show="loading">
    <div v-if="filteredItems.length" class="file-grid">
      <div v-for="item in filteredItems" :key="item.id" class="file-card" :class="{ clickable: item.type === 'FOLDER' }" @click="item.type === 'FOLDER' ? enterFolder(item) : undefined">
        <div class="file-icon" :class="{ folder: item.type === 'FOLDER' }">
          <NIcon :size="22" :color="item.type === 'FOLDER' ? '#F97316' : '#6b6b65'">
            <FolderOutline v-if="item.type === 'FOLDER'" /><DocumentOutline v-else />
          </NIcon>
        </div>
        <div class="file-body">
          <span class="file-name">{{ item.name }}</span>
          <span class="file-meta">{{ item.fileSize ? formatFileSize(item.fileSize) : '--' }} · {{ formatDate(item.createdAt, 'date') }}</span>
        </div>
        <div class="file-actions" @click.stop>
          <NButton v-if="item.type === 'FILE'" size="tiny" quaternary title="下载" aria-label="下载" @click="handleDownload(item)"><template #icon><NIcon :size="15"><CloudDownloadOutline /></NIcon></template></NButton>
          <NButton v-if="item.type === 'FILE'" size="tiny" quaternary title="预览" aria-label="预览" @click="handlePreview(item)"><template #icon><NIcon :size="15"><EyeOutline /></NIcon></template></NButton>
          <NPopconfirm @positive-click="() => handleDelete(item.id)"><template #trigger><NButton size="tiny" quaternary title="删除" aria-label="删除"><template #icon><NIcon :size="15"><TrashOutline /></NIcon></template></NButton></template>确定删除？</NPopconfirm>
        </div>
      </div>
    </div>
    <NEmpty v-else-if="!uploading" description="此文件夹为空。拖拽文件到此处或点击上传按钮" />
    </NSpin>

    <NModal v-model:show="showNewFolder" title="新建文件夹" preset="card" class="folder-modal">
      <NInput v-model:value="folderName" placeholder="文件夹名称" />
      <template #footer><NSpace justify="end"><NButton @click="showNewFolder = false">取消</NButton><NButton type="primary" @click="createFolder">确定</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showPreview" :title="previewName" preset="card" class="preview-modal">
      <iframe v-if="previewUrl" :src="previewUrl" class="preview-frame" />
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; }
.header-action { margin-left: 8px; }
.hidden-input { display: none; }
.breadcrumb { font-size: 13px; margin: 16px 0; display: flex; align-items: center; gap: 2px; }
.crumb-sep { color: var(--n-text-color-3); margin: 0 2px; }
.drive-toolbar { margin-bottom: 12px; }
.search-input { width: 240px; }
.upload-status { padding: 8px 12px; font-size: 13px; color: var(--n-text-color-2); background: var(--n-color-embedded); border-radius: 6px; margin-bottom: 12px; }
.file-grid { display: flex; flex-direction: column; gap: 2px; margin-top: 12px; }
.file-card { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-radius: 8px; transition: background 0.15s; }
.file-card:hover { background: var(--n-color-embedded); }
.file-card.clickable { cursor: pointer; }
.file-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: var(--n-color-embedded); }
.file-icon.folder { background: rgba(249,115,22,0.1); }
.file-body { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.file-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-meta { font-size: 11px; color: var(--n-text-color-3); }
.file-actions { display: flex; gap: 0; flex-shrink: 0; }
.folder-modal { width: 360px; }
.preview-modal { width: 90vw; max-width: 900px; height: 80vh; }
.preview-frame { width: 100%; height: calc(80vh - 60px); border: none; border-radius: 0 0 8px 8px; }
</style>
