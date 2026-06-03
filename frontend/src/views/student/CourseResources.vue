<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NSpace, NEmpty, useMessage } from 'naive-ui'
import { ArrowBackOutline, DownloadOutline, EyeOutline } from '@vicons/ionicons5'
import FileTree from '@/components/FileTree.vue'
import FilePreview from '@/components/FilePreview.vue'
import { getCourse } from '@/api/courses'
import { listCourseResources } from '@/api/courses'
import { getDownloadUrl } from '@/api/files'
import { formatFileSize } from '@/utils/validation'
import type { CourseDetailVO, CourseResourceVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)
const tree = ref<CourseResourceVO[]>([])
const selectedFolderId = ref<number | null>(null)
const folderContents = ref<CourseResourceVO[]>([])
const loading = ref(false)

const previewId = ref<number | null>(null)
const previewName = ref('')

async function loadCourse() {
  try { course.value = await getCourse(courseId) }
  catch (e: any) { message.error(e.message || '加载失败'); router.push('/student/home') }
}

async function loadTree() {
  try { tree.value = await listCourseResources(courseId) } catch (e) { console.error("CourseResources.vue failed", e) }
}

async function loadContents() {
  loading.value = true
  try {
    if (selectedFolderId.value === null) {
      folderContents.value = tree.value.filter(n => (n as any).parentId == null)
    } else {
      folderContents.value = await listCourseResources(courseId, selectedFolderId.value)
    }
  } catch (e) { console.error("CourseResources.vue failed", e) }
  finally { loading.value = false }
}

function handleFolderSelect(id: number | null) {
  selectedFolderId.value = id
  loadContents()
}

function goBack() { router.push(`/student/courses/${courseId}`) }

async function handleDownload(resource: CourseResourceVO) {
  try {
    const { url } = await getDownloadUrl(resource.id)
    window.open(url, '_blank')
  } catch (e: any) { message.error(e.message || '下载失败') }
}

function handlePreview(resource: CourseResourceVO) {
  previewName.value = resource.name
  previewId.value = resource.id
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
    <div class="back-bar">
      <NButton text size="small" @click="goBack">
        <template #icon><NIcon :size="16"><ArrowBackOutline /></NIcon></template>
        返回课程
      </NButton>
      <span class="sep">/</span>
      <span class="current">{{ course?.name || '课程资源' }}</span>
    </div>

    <div class="page-head">
      <h2 class="page-title">课程资源</h2>
    </div>

    <div class="resource-layout">
      <FileTree
        :tree="tree"
        :selected-id="selectedFolderId"
        @select="handleFolderSelect"
      />

      <div class="resource-main">
        <div class="resource-list" v-if="!loading && folderContents.length">
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
            <NSpace v-if="isFile(item)" :size="2">
              <NButton size="tiny" quaternary @click="handlePreview(item)">
                <template #icon><NIcon :size="14"><EyeOutline /></NIcon></template>
              </NButton>
              <NButton size="tiny" quaternary @click="handleDownload(item)">
                <template #icon><NIcon :size="14"><DownloadOutline /></NIcon></template>
              </NButton>
            </NSpace>
          </div>
        </div>
        <NEmpty v-else-if="!loading" description="此文件夹为空" class="empty-hint" />
      </div>
    </div>

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
.empty-hint { padding: 40px 0; }
</style>
