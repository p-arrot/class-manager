<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { NEmpty, NButton, NIcon, NTag, NSpace, NModal, NInput, useMessage, NPopconfirm, NSpin } from 'naive-ui'
import { AddOutline, TrashOutline, FolderOutline, DocumentOutline, CloudDownloadOutline, EyeOutline, SearchOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'
import { formatFileSize } from '@/utils/validation'

interface DriveItem { id: number; name: string; type: string; fileSize: number | null; objectName: string | null; createdAt: string }

const message = useMessage()
const items = ref<DriveItem[]>([])
const parentId = ref<number | null>(null)
const breadcrumb = ref<DriveItem[]>([])
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
  try { items.value = await http.get('/drive/tree', { params: { parentId: parentId.value } }) } catch { /* ignore */ }
  finally { loading.value = false }
}

async function enterFolder(item: DriveItem) {
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
    await http.post('/drive/folders', { name: folderName.value, parentId: parentId.value })
    message.success('文件夹已创建'); showNewFolder.value = false; folderName.value = ''
    await loadItems()
  } catch (e: any) { message.error(e.message || '创建失败') }
}

async function handleDelete(id: number) {
  try { await http.delete(`/drive/${id}`); message.success('已删除'); await loadItems() }
  catch (e: any) { message.error(e.message || '删除失败') }
}

const uploading = ref(false)
const uploadQueue = ref(0)

async function uploadFile(file: File) {
  const fd = new FormData(); fd.append('file', file)
  if (parentId.value) fd.append('parentId', String(parentId.value))
  await http.post('/drive/upload', fd)
}

async function processFiles(files: FileList | File[]) {
  uploading.value = true
  uploadQueue.value = files.length
  let success = 0; let fail = 0
  for (const file of Array.from(files)) {
    try { await uploadFile(file); success++ }
    catch { fail++ }
    uploadQueue.value--
  }
  if (fail) message.warning(`${success} 个成功, ${fail} 个失败`)
  else message.success(`${success} 个文件上传成功`)
  uploading.value = false
  await loadItems()
}

async function handleUpload(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files?.length) return
  await processFiles(files)
}

async function handleDrop(e: DragEvent) {
  e.preventDefault()
  const items = e.dataTransfer?.items; if (!items) return
  const files: File[] = []
  async function scanEntries(entries: FileSystemEntry[]) {
    for (const entry of entries) {
      if (entry.isFile) {
        files.push(await new Promise(resolve => (entry as FileSystemFileEntry).file(resolve)))
      } else if (entry.isDirectory) {
        // Create folder for this directory
        const folderName = entry.name
        try {
          await http.post('/drive/folders', { name: folderName, parentId: parentId.value })
          message.info(`已创建文件夹: ${folderName}`)
        } catch { /* folder may exist */ }
      }
    }
  }
  const entries: FileSystemEntry[] = []
  for (let i = 0; i < items.length; i++) {
    const entry = items[i].webkitGetAsEntry()
    if (entry) entries.push(entry)
  }
  await scanEntries(entries)
  if (files.length) await processFiles(files)
}

function triggerUpload() { fileInput.value?.click() }
function triggerFolder() { folderInput.value?.click() }

async function handleDownload(item: any) {
  try {
    const blob = await http.get(`/drive/${item.id}/raw`, { responseType: 'blob' }) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = item.name; a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) { message.error('下载失败') }
}

async function handlePreview(item: any) {
  try {
    const r: any = await http.get(`/drive/${item.id}/preview`)
    if (r?.url) {
      previewUrl.value = r.url.replace('minio:9000', 'localhost:9000')
      previewName.value = item.name
      showPreview.value = true
    }
  } catch (e: any) { message.error('预览失败') }
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
        <NButton size="small" @click="triggerUpload" style="margin-left:8px">
          <template #icon><NIcon :size="14"><DocumentOutline /></NIcon></template>上传文件
        </NButton>
        <NButton size="small" @click="triggerFolder" style="margin-left:8px">
          <template #icon><NIcon :size="14"><FolderOutline /></NIcon></template>上传文件夹
        </NButton>
        <input ref="fileInput" type="file" multiple style="display:none" @change="handleUpload" />
        <input ref="folderInput" type="file" webkitdirectory multiple style="display:none" @change="handleUpload" />
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
      <NInput v-model:value="searchQuery" placeholder="搜索文件..." clearable style="width:240px">
        <template #prefix><NIcon :size="14"><SearchOutline /></NIcon></template>
      </NInput>
    </div>

    <div v-if="uploading" class="upload-status">
      <span>{{ uploadQueue }} 个文件待上传...</span>
    </div>

    <NSpin :show="loading">
    <div v-if="filteredItems.length" class="file-grid">
      <div v-for="item in filteredItems" :key="item.id" class="file-card" :class="{ clickable: item.type === 'FOLDER' }" @click="item.type === 'FOLDER' ? enterFolder(item) : undefined">
        <div class="file-icon" :style="{ background: item.type === 'FOLDER' ? 'rgba(249,115,22,0.1)' : 'var(--n-color-embedded)' }">
          <NIcon :size="22" :color="item.type === 'FOLDER' ? '#F97316' : '#6b6b65'">
            <FolderOutline v-if="item.type === 'FOLDER'" /><DocumentOutline v-else />
          </NIcon>
        </div>
        <div class="file-body">
          <span class="file-name">{{ item.name }}</span>
          <span class="file-meta">{{ item.fileSize ? formatFileSize(item.fileSize) : '--' }} · {{ formatDate(item.createdAt, 'date') }}</span>
        </div>
        <div class="file-actions" @click.stop>
          <NButton v-if="item.type === 'FILE'" size="tiny" quaternary @click="handleDownload(item)" title="下载"><template #icon><NIcon :size="15"><CloudDownloadOutline /></NIcon></template></NButton>
          <NButton v-if="item.type === 'FILE'" size="tiny" quaternary @click="handlePreview(item)" title="预览"><template #icon><NIcon :size="15"><EyeOutline /></NIcon></template></NButton>
          <NPopconfirm @positive-click="() => handleDelete(item.id)"><template #trigger><NButton size="tiny" quaternary><template #icon><NIcon :size="15"><TrashOutline /></NIcon></template></NButton></template>确定删除？</NPopconfirm>
        </div>
      </div>
    </div>
    <NEmpty v-else-if="!uploading" description="此文件夹为空。拖拽文件到此处或点击上传按钮" />
    </NSpin>

    <NModal v-model:show="showNewFolder" title="新建文件夹" preset="card" style="width:360px">
      <NInput v-model:value="folderName" placeholder="文件夹名称" />
      <template #footer><NSpace justify="end"><NButton @click="showNewFolder = false">取消</NButton><NButton type="primary" @click="createFolder">确定</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showPreview" :title="previewName" preset="card" style="width:90vw;max-width:900px;height:80vh">
      <iframe v-if="previewUrl" :src="previewUrl" style="width:100%;height:calc(80vh - 60px);border:none;border-radius:0 0 8px 8px" />
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; }
.breadcrumb { font-size: 13px; margin: 16px 0; display: flex; align-items: center; gap: 2px; }
.crumb-sep { color: var(--n-text-color-3); margin: 0 2px; }
.drive-toolbar { margin-bottom: 12px; }
.upload-status { padding: 8px 12px; font-size: 13px; color: var(--n-text-color-2); background: var(--n-color-embedded); border-radius: 6px; margin-bottom: 12px; }
.file-grid { display: flex; flex-direction: column; gap: 2px; margin-top: 12px; }
.file-card { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-radius: 8px; transition: background 0.15s; }
.file-card:hover { background: var(--n-color-embedded); }
.file-card.clickable { cursor: pointer; }
.file-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.file-body { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.file-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-meta { font-size: 11px; color: var(--n-text-color-3); }
.file-actions { display: flex; gap: 0; flex-shrink: 0; }
</style>
