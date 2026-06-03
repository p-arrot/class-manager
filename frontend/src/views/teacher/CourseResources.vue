<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NModal, NForm, NFormItem, NInput, NSpace, NTag, NPopconfirm, useMessage, useDialog } from 'naive-ui'
import { ArrowBackOutline, AddOutline, DownloadOutline, EyeOutline, TrashOutline, CreateOutline } from '@vicons/ionicons5'
import FileTree from '@/components/FileTree.vue'
import FileUpload from '@/components/FileUpload.vue'
import FilePreview from '@/components/FilePreview.vue'
import { getCourse } from '@/api/courses'
import { listCourseResources, createResourceFolder, renameResource, deleteResource } from '@/api/courses'
import { getDownloadUrl } from '@/api/files'
import { formatFileSize } from '@/utils/validation'
import type { CourseDetailVO, CourseResourceVO } from '@/types/api'
import type { FormInst, FormRules } from 'naive-ui'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)
const tree = ref<CourseResourceVO[]>([])
const selectedFolderId = ref<number | null>(null)
const folderContents = ref<CourseResourceVO[]>([])
const loading = ref(false)

// New folder modal
const showNewFolder = ref(false)
const newFolderName = ref('')
const folderFormRef = ref<FormInst | null>(null)
const folderRules: FormRules = { name: { required: true, message: '请输入文件夹名称', trigger: 'blur' } }

// Rename modal
const showRename = ref(false)
const renamingResource = ref<CourseResourceVO | null>(null)
const renameValue = ref('')

// Preview
const previewId = ref<number | null>(null)
const previewName = ref('')

// Upload refresh counter
const uploadKey = ref(0)

async function loadCourse() {
  try {
    course.value = await getCourse(courseId)
  } catch (e: any) {
    message.error(e.message || '加载课程失败')
    router.push('/teacher/courses')
  }
}

async function loadTree() {
  try {
    tree.value = await listCourseResources(courseId)
  } catch (e) { console.error("CourseResources.vue failed", e) }
}

async function loadContents() {
  loading.value = true
  try {
    const children = selectedFolderId.value === null
      ? tree.value.filter(n => (n as any).parentId == null)
      : await listCourseResources(courseId, selectedFolderId.value)
    folderContents.value = children
  } catch (e) { console.error("CourseResources.vue failed", e) }
  finally { loading.value = false }
}

function handleFolderSelect(id: number | null) {
  selectedFolderId.value = id
  loadContents()
}

function goBack() { router.push(`/teacher/courses/${courseId}`) }

// New folder
async function handleCreateFolder() {
  try { await folderFormRef.value?.validate() } catch { return }
  try {
    await createResourceFolder(courseId, {
      name: newFolderName.value,
      parentId: selectedFolderId.value,
    })
    message.success('文件夹已创建')
    showNewFolder.value = false
    newFolderName.value = ''
    await loadTree()
    await loadContents()
  } catch (e: any) { message.error(e.message || '创建失败') }
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
  } catch (e: any) { message.error(e.message || '重命名失败') }
}

// Delete
async function handleDelete(resource: CourseResourceVO) {
  try {
    await deleteResource(resource.id)
    message.success('已删除')
    await loadTree()
    await loadContents()
  } catch (e: any) { message.error(e.message || '删除失败') }
}

// Download
async function handleDownload(resource: CourseResourceVO) {
  try {
    const { url } = await getDownloadUrl(resource.id)
    window.open(url, '_blank')
  } catch (e: any) { message.error(e.message || '下载失败') }
}

// Preview
function handlePreview(resource: CourseResourceVO) {
  previewName.value = resource.name
  previewId.value = resource.id
}

// Upload complete
function handleUploaded() {
  uploadKey.value++
  loadTree()
  loadContents()
}

function isFolder(r: CourseResourceVO) { return r.type === 'FOLDER' }
function isFile(r: CourseResourceVO) { return r.type === 'FILE' }

onMounted(async () => {
  await loadCourse()
  await loadTree()
  await loadContents()
})
</script>

<template>
  <div class="page">
    <!-- Breadcrumb -->
    <div class="back-bar">
      <NButton text size="small" @click="goBack">
        <template #icon><NIcon :size="16"><ArrowBackOutline /></NIcon></template>
        返回课程
      </NButton>
      <span class="sep">/</span>
      <span class="current">{{ course?.name || '课程资源' }}</span>
    </div>

    <!-- Title -->
    <div class="page-head">
      <h2 class="page-title">课程资源</h2>
      <NSpace :size="8">
        <NButton size="small" quaternary @click="showNewFolder = true">
          <template #icon><NIcon :size="15"><CreateOutline /></NIcon></template>
          新建文件夹
        </NButton>
      </NSpace>
    </div>

    <!-- Main: Tree + Content -->
    <div class="resource-layout">
      <FileTree
        :tree="tree"
        :selected-id="selectedFolderId"
        @select="handleFolderSelect"
      />

      <div class="resource-main">
        <!-- Upload zone -->
        <FileUpload
          :key="uploadKey"
          :course-id="courseId"
          :parent-id="selectedFolderId"
          @uploaded="handleUploaded"
        />

        <!-- File & folder list -->
        <div class="resource-list" v-if="!loading">
          <div
            v-for="item in folderContents"
            :key="item.id"
            class="resource-row"
          >
            <div class="res-info">
              <NTag :type="isFolder(item) ? 'info' : 'default'" size="tiny" :bordered="false">
                {{ isFolder(item) ? '文件夹' : '文件' }}
              </NTag>
              <span class="res-name">{{ item.name }}</span>
              <span v-if="isFile(item) && item.fileSize" class="res-meta">
                {{ formatFileSize(item.fileSize) }}
              </span>
            </div>
            <NSpace :size="2">
              <template v-if="isFile(item)">
                <NButton size="tiny" quaternary @click="handlePreview(item)">
                  <template #icon><NIcon :size="14"><EyeOutline /></NIcon></template>
                </NButton>
                <NButton size="tiny" quaternary @click="handleDownload(item)">
                  <template #icon><NIcon :size="14"><DownloadOutline /></NIcon></template>
                </NButton>
              </template>
              <NButton size="tiny" quaternary @click="openRename(item)">
                <template #icon>
                  <NIcon :size="14">
                    <svg viewBox="0 0 512 512" width="14" height="14"><path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M364.13 125.25L87 403l-23 45 44.99-23 277.76-277.13-22.62-22.62z"/><path d="M420.55 150.68l-22.62-22.62 35.69-35.69a32 32 0 0145.25 0l.01.01a32 32 0 010 45.25l-35.68 35.68z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="32"/></svg>
                  </NIcon>
                </template>
              </NButton>
              <NPopconfirm @positive-click="() => handleDelete(item)">
                <template #trigger>
                  <NButton size="tiny" quaternary>
                    <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
                  </NButton>
                </template>
                确定删除「{{ item.name }}」？
              </NPopconfirm>
            </NSpace>
          </div>
          <div v-if="folderContents.length === 0" class="empty-hint">
            此文件夹为空，上传文件或创建子文件夹
          </div>
        </div>
      </div>
    </div>

    <!-- New folder modal -->
    <NModal v-model:show="showNewFolder" title="新建文件夹" preset="card" style="width:360px">
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
    <NModal v-model:show="showRename" title="重命名" preset="card" style="width:360px">
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
    <FilePreview
      :resource-id="previewId"
      :file-name="previewName"
      @close="previewId = null"
    />
  </div>
</template>

<style scoped>
.page { max-width: 1100px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.back-bar { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; font-size: 13px; }
.sep { color: var(--n-text-color-3); }
.current { color: var(--n-text-color-2); font-weight: 500; }
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; margin: 0; }
.resource-layout { display: flex; gap: 0; border: 1px solid var(--n-border-color); border-radius: 10px; overflow: hidden; min-height: 400px; }
.resource-main { flex: 1; padding: 20px; display: flex; flex-direction: column; gap: 16px; overflow: auto; }
.resource-list { display: flex; flex-direction: column; gap: 2px; }
.resource-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-radius: 6px; transition: background 150ms ease; }
.resource-row:hover { background: var(--n-color-embedded); }
.res-info { display: flex; align-items: center; gap: 10px; min-width: 0; }
.res-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.res-meta { font-size: 12px; color: var(--n-text-color-3); flex-shrink: 0; }
.empty-hint { text-align: center; padding: 40px 0; font-size: 13px; color: var(--n-text-color-3); }
</style>
