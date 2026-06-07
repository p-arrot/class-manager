<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NButton, NIcon, NModal, NForm, NFormItem, NInput, NSpace, NTag, NPopconfirm, NPopover, useMessage } from 'naive-ui'
import { DownloadOutline, EyeOutline, TrashOutline, CreateOutline, ArrowForwardOutline } from '@vicons/ionicons5'
import FileTree from '@/components/FileTree.vue'
import FileUpload from '@/components/FileUpload.vue'
import FilePreview from '@/components/FilePreview.vue'
import { listCourseResources, createResourceFolder, renameResource, deleteResource } from '@/api/courses'
import http from '@/api/request'
import { formatFileSize, getFileExtension } from '@/utils/validation'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { CourseResourceVO } from '@/types/api'
import type { FormInst } from 'naive-ui'

const props = defineProps<{
  courseId: number
  readonly?: boolean
}>()

const message = useMessage()

const tree = ref<CourseResourceVO[]>([])
const selectedFolderId = ref<number | null>(null)
const folderContents = ref<CourseResourceVO[]>([])
const loading = ref(false)

// New folder
const showNewFolder = ref(false)
const newFolderName = ref('')
const folderFormRef = ref<FormInst | null>(null)

// Rename
const showRename = ref(false)
const renamingResource = ref<CourseResourceVO | null>(null)
const renameValue = ref('')

// Preview
const previewId = ref<number | null>(null)
const previewName = ref('')

// Upload refresh trigger
const uploadKey = ref(0)

const PREVIEWABLE_EXTENSIONS = new Set([
  'pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx',
  'txt', 'html', 'htm', 'md', 'jpg', 'jpeg', 'png', 'gif', 'bmp',
])

function isPreviewable(resource: CourseResourceVO): boolean {
  if (resource.type !== 'FILE') return false
  const ext = getFileExtension(resource.name).toLowerCase()
  return PREVIEWABLE_EXTENSIONS.has(ext)
}

async function loadTree() {
  try {
    tree.value = await listCourseResources(props.courseId)
  } catch (e) {
    tree.value = []
    message.error(getErrorMessage(e, '加载资源目录失败'))
  }
}

async function loadContents() {
  loading.value = true
  try {
    if (selectedFolderId.value === null) {
      folderContents.value = tree.value.filter(n => n.parentId == null)
    } else {
      folderContents.value = await listCourseResources(props.courseId, selectedFolderId.value)
    }
  } catch (e) {
    folderContents.value = []
    message.error(getErrorMessage(e, '加载资源列表失败'))
  } finally {
    loading.value = false
  }
}

function handleFolderSelect(id: number | null) {
  selectedFolderId.value = id
  loadContents()
}

// New folder
async function handleCreateFolder() {
  try {
    await folderFormRef.value?.validate()
  } catch {
    return
  }
  try {
    await createResourceFolder(props.courseId, {
      name: newFolderName.value, parentId: selectedFolderId.value,
    })
    message.success('文件夹已创建')
    showNewFolder.value = false
    newFolderName.value = ''
    await loadTree()
    await loadContents()
  } catch (e) {
    message.error(getErrorMessage(e, '创建失败'))
  }
}

// Rename
function openRename(resource: CourseResourceVO) {
  renamingResource.value = resource
  renameValue.value = resource.name
  showRename.value = true
}
async function handleRename() {
  if (!renamingResource.value) return
  try {
    await renameResource(renamingResource.value.id, { name: renameValue.value })
    message.success('已重命名')
    showRename.value = false
    await loadTree()
    await loadContents()
  } catch (e) {
    message.error(getErrorMessage(e, '重命名失败'))
  }
}

// Delete
async function handleDelete(resource: CourseResourceVO) {
  try {
    await deleteResource(resource.id)
    message.success('已删除')
    await loadTree()
    await loadContents()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

// Download
async function handleDownload(resource: CourseResourceVO) {
  try {
    const response = await http.get<Blob>(`/files/${resource.id}/raw`, {
      responseType: 'blob',
    })
    const blobUrl = URL.createObjectURL(response)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = resource.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
}

// Preview
function handlePreview(resource: CourseResourceVO) {
  previewName.value = resource.name
  previewId.value = resource.id
}

// Upload complete callback
async function handleUploaded() {
  message.success('文件上传成功')
  uploadKey.value++
  await loadTree()
  await loadContents()
}

// Open folder (navigate into folder from file list)
function openFolder(resource: CourseResourceVO) {
  selectedFolderId.value = resource.id
  loadContents()
}

function isFolder(r: CourseResourceVO) {
  return r.type === 'FOLDER'
}

function isFile(r: CourseResourceVO) {
  return r.type === 'FILE'
}

onMounted(async () => {
  await loadTree()
  await loadContents()
})
</script>

<template>
  <div class="resource-panel">
    <div v-if="!readonly" class="panel-toolbar">
      <NButton size="small" quaternary @click="showNewFolder = true">
        <template #icon><NIcon :size="15"><CreateOutline /></NIcon></template>
        新建文件夹
      </NButton>
    </div>

    <div class="panel-layout">
      <FileTree :tree="tree" :selected-id="selectedFolderId" @select="handleFolderSelect" />

      <div class="panel-content">
        <FileUpload
          v-if="!readonly"
          :key="uploadKey"
          :course-id="courseId"
          :parent-id="selectedFolderId"
          :existing-names="folderContents.map(c => c.name)"
          @uploaded="handleUploaded"
          @folder-uploaded="handleUploaded"
        />

        <div class="resource-list" v-if="!loading && folderContents.length">
          <div v-for="item in folderContents" :key="item.id" class="resource-row">
            <div class="res-info">
              <NTag :type="isFolder(item) ? 'info' : 'default'" size="tiny" :bordered="false">
                {{ isFolder(item) ? '文件夹' : '文件' }}
              </NTag>
              <span class="res-name">{{ item.name }}</span>
              <span v-if="isFile(item) && item.fileSize" class="res-meta">
                {{ formatFileSize(item.fileSize) }}
              </span>
              <span class="res-time">{{ formatDate(item.createdAt, 'date') }}</span>
            </div>
            <NSpace :size="2">
              <!-- Preview button (file only, previewable types) -->
              <NPopover v-if="isFile(item) && !isPreviewable(item)" trigger="hover" placement="top">
                <template #trigger>
                  <NButton size="tiny" quaternary disabled title="无法预览" aria-label="无法预览">
                    <template #icon><NIcon :size="14"><EyeOutline /></NIcon></template>
                  </NButton>
                </template>
                该文件类型无法预览
              </NPopover>
              <NButton v-else-if="isFile(item)" size="tiny" quaternary title="预览文件" aria-label="预览文件" @click="handlePreview(item)">
                <template #icon><NIcon :size="14"><EyeOutline /></NIcon></template>
              </NButton>

              <!-- Download (file only) -->
              <NButton v-if="isFile(item)" size="tiny" quaternary title="下载文件" aria-label="下载文件" @click="handleDownload(item)">
                <template #icon><NIcon :size="14"><DownloadOutline /></NIcon></template>
              </NButton>

              <!-- Open folder (folder only) -->
              <NButton v-if="isFolder(item)" size="tiny" quaternary title="打开文件夹" aria-label="打开文件夹" @click="openFolder(item)">
                <template #icon><NIcon :size="14"><ArrowForwardOutline /></NIcon></template>
              </NButton>

              <!-- Edit (not readonly) -->
              <template v-if="!readonly">
                <NButton size="tiny" quaternary title="重命名" aria-label="重命名" @click="openRename(item)">
                  <template #icon>
                    <NIcon :size="14"><CreateOutline /></NIcon>
                  </template>
                </NButton>
                <NPopconfirm @positive-click="() => handleDelete(item)">
                  <template #trigger>
                    <NButton size="tiny" quaternary title="删除" aria-label="删除">
                      <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
                    </NButton>
                  </template>
                  确定删除「{{ item.name }}」？
                </NPopconfirm>
              </template>
            </NSpace>
          </div>
        </div>
        <div v-else-if="!loading" class="empty-hint">
          {{ readonly ? '此文件夹为空' : '此文件夹为空，上传文件或创建子文件夹' }}
        </div>
      </div>
    </div>

    <!-- New folder modal -->
    <NModal v-model:show="showNewFolder" title="新建文件夹" preset="card" class="resource-modal">
      <NForm ref="folderFormRef" :model="{ name: newFolderName }" :rules="{ name: { required: true, message: '请输入文件夹名称', trigger: 'blur' } }" label-placement="left" label-width="56">
        <NFormItem label="名称" path="name">
          <NInput v-model:value="newFolderName" placeholder="如：课件资料" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showNewFolder = false">取消</NButton>
          <NButton type="primary" @click="handleCreateFolder">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- Rename modal -->
    <NModal v-model:show="showRename" title="重命名" preset="card" class="resource-modal">
      <NFormItem label="名称" label-placement="left" label-width="56">
        <NInput v-model:value="renameValue" placeholder="新名称" />
      </NFormItem>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showRename = false">取消</NButton>
          <NButton type="primary" @click="handleRename">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- File preview -->
    <FilePreview :resource-id="previewId" :file-name="previewName" @close="previewId = null" />
  </div>
</template>

<style scoped>
.resource-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.panel-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.panel-layout {
  display: flex;
  gap: 0;
  border: 1px solid var(--n-border-color);
  border-radius: 10px;
  overflow: hidden;
  min-height: 350px;
}
.panel-content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}
.resource-list { display: flex; flex-direction: column; gap: 2px; }
.resource-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; border-radius: 6px; transition: background 150ms ease;
}
.resource-row:hover { background: var(--n-color-embedded); }
.res-info { display: flex; align-items: center; gap: 10px; min-width: 0; }
.res-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.res-meta { font-size: 12px; color: var(--n-text-color-3); flex-shrink: 0; }
.res-time { font-size: 12px; color: var(--n-text-color-3); flex-shrink: 0; margin-left: 6px; }
.empty-hint { text-align: center; padding: 40px 0; font-size: 13px; color: var(--n-text-color-3); }
.resource-modal { width: 360px; }
</style>
