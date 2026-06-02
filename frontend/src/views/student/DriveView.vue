<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NButton, NIcon, NTag, NSpace, NModal, NInput, useMessage, NPopconfirm } from 'naive-ui'
import { AddOutline, TrashOutline, FolderOutline, DocumentOutline } from '@vicons/ionicons5'
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
const fileInput = ref<HTMLInputElement | null>(null)

async function loadItems() {
  try { items.value = await http.get('/drive/tree', { params: { parentId: parentId.value } }) } catch { /* ignore */ }
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

async function handleUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]; if (!file) return
  try {
    const fd = new FormData(); fd.append('file', file)
    await http.post('/files/upload', fd)
    message.success('上传成功'); await loadItems()
  } catch (e: any) { message.error(e.message || '上传失败') }
}

function triggerUpload() { fileInput.value?.click() }

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
        <input ref="fileInput" type="file" style="display:none" @change="handleUpload" />
      </template>
    </PageHeader>

    <div class="breadcrumb" v-if="breadcrumb.length">
      <NButton text size="tiny" @click="goBack(0)">根目录</NButton>
      <span v-for="(b, i) in breadcrumb" :key="b.id">
        <span class="crumb-sep">/</span>
        <NButton text size="tiny" @click="goBack(i + 1)">{{ b.name }}</NButton>
      </span>
    </div>

    <div v-if="items.length" class="file-list">
      <div v-for="item in items" :key="item.id" class="file-row">
        <div class="file-info" @click="item.type === 'FOLDER' ? enterFolder(item) : undefined" :style="{ cursor: item.type === 'FOLDER' ? 'pointer' : 'default' }">
          <NIcon :size="16" :color="item.type === 'FOLDER' ? '#F97316' : '#6b6b65'">
            <FolderOutline v-if="item.type === 'FOLDER'" /><DocumentOutline v-else />
          </NIcon>
          <span class="file-name">{{ item.name }}</span>
          <span v-if="item.fileSize" class="file-meta">{{ formatFileSize(item.fileSize) }}</span>
          <span class="file-time">{{ formatDate(item.createdAt, 'date') }}</span>
        </div>
        <NPopconfirm @positive-click="() => handleDelete(item.id)">
          <template #trigger>
            <NButton size="tiny" quaternary><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton>
          </template>
          确定删除？
        </NPopconfirm>
      </div>
    </div>
    <NEmpty v-else description="此文件夹为空" />

    <NModal v-model:show="showNewFolder" title="新建文件夹" preset="card" style="width:360px">
      <NInput v-model:value="folderName" placeholder="文件夹名称" />
      <template #footer><NSpace justify="end"><NButton @click="showNewFolder = false">取消</NButton><NButton type="primary" @click="createFolder">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.breadcrumb { font-size: 13px; margin: 16px 0; display: flex; align-items: center; gap: 2px; }
.crumb-sep { color: var(--n-text-color-3); margin: 0 2px; }
.file-list { display: flex; flex-direction: column; gap: 2px; margin-top: 12px; }
.file-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-radius: 6px; }
.file-row:hover { background: var(--n-color-embedded); }
.file-info { display: flex; align-items: center; gap: 8px; flex: 1; }
.file-name { font-size: 14px; font-weight: 500; }
.file-meta { font-size: 11px; color: var(--n-text-color-3); }
.file-time { font-size: 11px; color: var(--n-text-color-3); margin-left: auto; }
</style>
