<script setup lang="ts">
import { ref, computed } from 'vue'
import { NButton, NIcon, NProgress, NSpace } from 'naive-ui'
import { CloudUploadOutline, DocumentOutline, FolderOpenOutline } from '@vicons/ionicons5'
import { useFileUpload } from '@/composables/useFileUpload'
import { createResourceFolder } from '@/api/courses'

const props = defineProps<{
  courseId: number
  parentId: number | null
  existingNames?: string[]
}>()

const emit = defineEmits<{
  uploaded: [resourceId: number]
  folderUploaded: []
}>()

const { state, uploadFile, retry, reset } = useFileUpload()
const dragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)

const hintText = computed(() => {
  if (state.value.error) return state.value.error
  if (state.value.uploading) return `上传中 ${state.value.progress}%`
  return `拖拽文件到此处，或点击上传`
})

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  dragging.value = true
}
function handleDragLeave() {
  dragging.value = false
}
function handleDrop(e: DragEvent) {
  e.preventDefault()
  dragging.value = false
  const items = e.dataTransfer?.items
  if (!items || items.length === 0) return
  // Process dropped items recursively (files + folders)
  processEntry(items, props.parentId)
}

async function processEntry(items: DataTransferItemList, parentId: number | null) {
  const entries: FileSystemEntry[] = []
  for (let i = 0; i < items.length; i++) {
    const entry = items[i].webkitGetAsEntry()
    if (entry) entries.push(entry)
  }
  let count = 0
  for (const entry of entries) {
    await traverseEntry(entry, parentId)
    count++
  }
  if (count > 0) {
    emit('folderUploaded')
  }
}

async function traverseEntry(entry: FileSystemEntry, parentId: number | null): Promise<void> {
  if (entry.isFile) {
    const file = await new Promise<File>((resolve) => {
      (entry as FileSystemFileEntry).file(resolve)
    })
    await uploadFile(file, props.courseId, parentId)
    emit('uploaded', 0) // trigger refresh
  } else if (entry.isDirectory) {
    // Create folder
    let folderId: number | null = null
    try {
      const folder = await createResourceFolder(props.courseId, {
        name: entry.name,
        parentId: parentId,
      })
      folderId = folder.id
    } catch {
      folderId = null
    }
    // Process children
    const reader = (entry as FileSystemDirectoryEntry).createReader()
    const children = await new Promise<FileSystemEntry[]>((resolve) => {
      reader.readEntries(resolve)
    })
    for (const child of children) {
      await traverseEntry(child, folderId)
    }
  }
}

function handleFileSelect() {
  fileInput.value?.click()
}
function handleFolderSelect() {
  folderInput.value?.click()
}

async function handleInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  // If it's from folder input (webkitdirectory), process all files with relative paths
  if (input === folderInput.value) {
    // Build folder structure from relative paths
    const folderMap = new Map<string, number | null>() // path -> folderId
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const relativePath = (file as any).webkitRelativePath || file.name
      const parts = relativePath.split('/')
      const fileName = parts.pop()!
      // Create parent folders as needed
      let currentParentId = props.parentId
      let currentPath = ''
      for (let j = 0; j < parts.length; j++) {
        currentPath += (currentPath ? '/' : '') + parts[j]
        if (!folderMap.has(currentPath)) {
          try {
            const folder = await createResourceFolder(props.courseId, {
              name: parts[j],
              parentId: currentParentId,
            })
            folderMap.set(currentPath, folder.id)
            currentParentId = folder.id
          } catch {
            currentParentId = null
            break
          }
        } else {
          currentParentId = folderMap.get(currentPath)!
        }
      }
      // Upload file to its parent folder
      if (currentParentId !== null || props.parentId === null) {
        const rid = await uploadFile(file, props.courseId, currentParentId)
        if (rid) emit('uploaded', rid)
      }
    }
    emit('folderUploaded')
  } else {
    // Single file upload
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      // Check name conflict
      if (props.existingNames?.includes(file.name)) {
        const ok = window.confirm(`文件「${file.name}」已存在，是否覆盖？`)
        if (!ok) continue
      }
      const rid = await uploadFile(file, props.courseId, props.parentId)
      if (rid) emit('uploaded', rid)
    }
  }
  input.value = ''
}

async function handleRetry() {
  const resourceId = await retry()
  if (resourceId) {
    emit('uploaded', resourceId)
  }
}
</script>

<template>
  <div
    class="upload-zone"
    :class="{ dragging, uploading: state.uploading, error: !!state.error }"
    @dragover="handleDragOver"
    @dragleave="handleDragLeave"
    @drop="handleDrop"
  >
    <input ref="fileInput" type="file" class="file-input" multiple @change="handleInputChange" />
    <input ref="folderInput" type="file" class="file-input" webkitdirectory @change="handleInputChange" />

    <div class="upload-content">
      <NIcon :size="28" :depth="state.error ? 3 : 2">
        <CloudUploadOutline v-if="!state.uploading && !state.error" />
        <DocumentOutline v-else />
      </NIcon>
      <span class="upload-hint">{{ hintText }}</span>

      <NProgress
        v-if="state.uploading"
        :percentage="state.progress"
        :height="4"
        :border-radius="2"
        style="width: 100%; max-width: 280px;"
        :show-text="false"
      />

      <NSpace v-if="state.error && !state.uploading" :size="4">
        <NButton size="tiny" quaternary @click.stop="handleRetry">重试</NButton>
      </NSpace>

      <NSpace v-if="!state.uploading" :size="6">
        <NButton size="tiny" quaternary @click.stop="handleFileSelect()">
          <template #icon><NIcon :size="13"><DocumentOutline /></NIcon></template>
          文件
        </NButton>
        <NButton size="tiny" quaternary @click.stop="handleFolderSelect()">
          <template #icon><NIcon :size="13"><FolderOpenOutline /></NIcon></template>
          文件夹
        </NButton>
      </NSpace>
    </div>
  </div>

  <div class="upload-ext-hint">拖拽文件/文件夹到此处，或点击选择。单文件最大 200MB</div>
</template>

<style scoped>
.upload-zone {
  border: 1px dashed var(--n-border-color);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 200ms ease, background-color 200ms ease;
  background: transparent;
}
.upload-zone:hover { border-color: var(--n-text-color-3); }
.upload-zone.dragging {
  border-color: var(--n-primary-color);
  border-style: solid;
  background: rgba(var(--n-primary-color-rgb, 24 160 88), 0.04);
}
.upload-zone.uploading { border-style: solid; border-color: var(--n-primary-color); cursor: default; }
.upload-zone.error { border-color: var(--n-error-color); border-style: solid; }
.file-input { display: none; }
.upload-content { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.upload-hint { font-size: 13px; color: var(--n-text-color-2); transition: color 200ms ease; }
.upload-zone.error .upload-hint { color: var(--n-error-color); }
.upload-ext-hint { font-size: 11px; color: var(--n-text-color-3); margin-top: 6px; text-align: center; }
</style>
