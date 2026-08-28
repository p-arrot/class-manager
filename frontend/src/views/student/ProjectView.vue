<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NAlert, NButton, NEmpty, NIcon, NInput, NModal, NSelect, NSpace, NSpin, NTag, useMessage } from 'naive-ui'
import { CloudUploadOutline, FolderOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { uploadDriveFile } from '@/api/drive'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { getMyProjectSubmission, listProjects, submitProject } from '@/api/projects'
import { defaultProjectArtifact, parseProjectDescription, parseProjectSubmissionContent } from '@/types/project'
import { CORE_DIMENSIONS } from '@/types/taskSchema'
import type { DriveItemVO, ProjectSubmissionVO, ProjectVO, SemesterVO } from '@/types/api'

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const projects = ref<ProjectVO[]>([])
const showSubmit = ref(false)
const activeProject = ref<ProjectVO | null>(null)
const activeSubmission = ref<ProjectSubmissionVO | null>(null)
const submitNote = ref('')
const uploadedItems = ref<DriveItemVO[]>([])
const uploadLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)

const activeConfig = computed(() => activeProject.value ? parseProjectDescription(activeProject.value).artifact : defaultProjectArtifact)
const extensionLabel = computed(() => activeConfig.value.allowedExtensions.length ? activeConfig.value.allowedExtensions.map((ext: string) => `.${ext}`).join('、') : '不限格式')
const submissionLocked = computed(() => Boolean(activeSubmission.value && !activeSubmission.value.canResubmit))
const courseOptions = computed(() => courses.value.map(course => ({
  label: course.name,
  value: course.id,
})))
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

watch(activeCourseId, async (cid) => {
  if (!cid) return
  await loadSemesters(cid)
  activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
})
watch(activeSemesterId, async (sid) => {
  if (sid) {
    try {
      projects.value = await listProjects(sid)
    } catch (e) {
      projects.value = []
      message.error(getErrorMessage(e, '加载项目列表失败'))
    }
  } else {
    projects.value = []
  }
})

function pickCurrentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(s => new Date(s.startTime).getTime() <= now && now <= new Date(s.endTime).getTime())
}

function projectDescription(project: ProjectVO) {
  return parseProjectDescription(project).text
}

async function openSubmit(project: ProjectVO) {
  activeProject.value = project
  submitNote.value = ''
  uploadedItems.value = []
  activeSubmission.value = null
  try {
    const existing = await getMyProjectSubmission(project.id)
    activeSubmission.value = existing
    if (existing?.content) {
      const parsed = parseProjectSubmissionContent(existing.content)
      submitNote.value = parsed.note
      uploadedItems.value = parsed.files.map(file => ({ ...file, fileSize: file.fileSize ?? null, type: 'FILE', contentType: null, parentId: null, objectName: null, createdAt: existing.createdAt || '' }))
    }
    showSubmit.value = true
  } catch (error) { message.error(getErrorMessage(error, '加载已有提交失败')) }
}

function dimensionLabel(value: string) {
  return CORE_DIMENSIONS.find(item => item.key === value)?.label ?? value
}

function fileAllowed(file: File) {
  const allowed = activeConfig.value.allowedExtensions
  if (!allowed.length || activeConfig.value.submitMode === 'folder') return true
  const ext = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : ''
  return allowed.includes(ext)
}

async function uploadFiles(files: FileList | File[]) {
  const list = Array.from(files)
  if (!list.length) return
  const invalid = list.find(file => !fileAllowed(file))
  if (invalid) {
    message.warning(`文件格式不符合要求：${invalid.name}`)
    return
  }
  uploadLoading.value = true
  try {
    for (const file of list) {
      const fd = new FormData()
      fd.append('file', file)
      uploadedItems.value.push(await uploadDriveFile(fd))
    }
    message.success(`${list.length} 个文件已上传`)
  } catch (e) {
    message.error(getErrorMessage(e, '上传失败'))
  } finally {
    uploadLoading.value = false
  }
}

async function handleFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (files?.length) await uploadFiles(files)
  input.value = ''
}

async function handleSubmit() {
  if (!activeProject.value) return
  if (!uploadedItems.value.length && !submitNote.value.trim()) {
    message.warning('请上传作品或填写作品说明')
    return
  }
  try {
    await submitProject(activeProject.value.id, {
      content: JSON.stringify({
        note: submitNote.value,
        submitMode: activeConfig.value.submitMode,
        files: uploadedItems.value.map(item => ({ id: item.id, name: item.name, fileSize: item.fileSize, type: item.type })),
      }),
    })
    message.success('提交成功')
    showSubmit.value = false
    if (activeSemesterId.value) projects.value = await listProjects(activeSemesterId.value)
  } catch (e) {
    message.error(getErrorMessage(e, '提交失败'))
  }
}

watch(courses, list => {
  if (!activeCourseId.value && list.length) activeCourseId.value = list[0].id
}, { immediate: true })

</script>

<template>
  <div class="page">
    <PageHeader title="项目化学习" subtitle="按个人提交作品；如有协作成员，请在备注中写清" />
    <NSpin :show="ctxLoading">
      <div class="filters">
        <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="filter-select" />
        <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="filter-select" />
      </div>
      <div v-if="projects.length" class="list">
        <div v-for="p in projects" :key="p.id" class="card">
          <div class="info">
            <span class="name">{{ p.name }}</span>
            <span v-if="projectDescription(p)" class="desc">{{ projectDescription(p) }}</span>
            <span class="meta">个人提交 · 截止 {{ p.deadline ? formatDate(p.deadline, 'datetime') : '未设置' }}</span>
            <NAlert v-if="p.submissionStatus === 'returned'" type="warning" :bordered="false">退回原因：{{ p.returnReason }}</NAlert>
            <NTag v-else-if="p.submissionStatus && p.submissionStatus !== 'not_submitted'" size="small" :bordered="false" :type="p.submissionStatus === 'graded' ? 'success' : 'warning'">{{ p.submissionStatus === 'graded' ? '已批改' : '已提交待批改' }}</NTag>
          </div>
          <NSpace :size="8" class="card-actions">
            <NButton size="small" type="primary" @click="openSubmit(p)">{{ p.submissionStatus === 'submitted' || p.submissionStatus === 'returned' ? '查看或修改' : p.submissionStatus === 'graded' ? '查看批改详情' : '提交作品' }}</NButton>
          </NSpace>
        </div>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无项目" />
    </NSpin>

    <NModal v-model:show="showSubmit" :title="activeProject ? `提交作品 · ${activeProject.name}` : '提交作品'" preset="card" class="project-modal">
      <NAlert type="info" :bordered="false" class="submit-hint">
        提交方式：{{ activeConfig.submitMode === 'folder' ? '文件夹' : '文件' }}；文件格式：{{ extensionLabel }}。如有组员，请在备注中写清姓名或学号。
      </NAlert>
      <NAlert v-if="activeSubmission?.status === 'graded'" type="success" :bordered="false" class="grading-summary">
        <strong>批改完成<span v-if="activeSubmission.score != null"> · {{ activeSubmission.score }} 分</span></strong>
        <div v-if="activeSubmission.dimensionScores?.length" class="dimension-results">
          <span v-for="item in activeSubmission.dimensionScores" :key="`${item.questionId}-${item.dimension}`">
            {{ dimensionLabel(item.dimension) }} {{ item.earnedScore }}/{{ item.maxScore }}
          </span>
        </div>
      </NAlert>
      <div v-if="!submissionLocked" class="upload-actions">
        <NButton :loading="uploadLoading" @click="fileInput?.click()">
          <template #icon><NIcon><CloudUploadOutline /></NIcon></template>
          上传文件
        </NButton>
        <NButton v-if="activeConfig.submitMode === 'folder'" :loading="uploadLoading" @click="folderInput?.click()">
          <template #icon><NIcon><FolderOutline /></NIcon></template>
          上传文件夹
        </NButton>
        <input ref="fileInput" type="file" :multiple="activeConfig.submitMode === 'folder'" class="hidden-input" @change="handleFileInput" />
        <input ref="folderInput" type="file" webkitdirectory multiple class="hidden-input" @change="handleFileInput" />
      </div>
      <div v-if="uploadedItems.length" class="uploaded-list">
        <div v-for="item in uploadedItems" :key="item.id" class="uploaded-item">
          <span>{{ item.name }}</span>
          <NTag size="tiny" :bordered="false">已上传</NTag>
        </div>
      </div>
      <label class="note-field">
        <span>备注 / 组员说明</span>
        <NInput
          v-model:value="submitNote"
          type="textarea"
          :readonly="submissionLocked"
          placeholder="例如：组员张三 20240102、李四 20240103；作品链接或补充说明也可以写在这里"
          :autosize="{ minRows: 3, maxRows: 8 }"
        />
        <small>教师会根据这里的信息手动找到对应学生并分别评分。</small>
      </label>
      <template #footer><NSpace justify="end"><NButton @click="showSubmit = false">关闭</NButton><NButton v-if="!submissionLocked" type="primary" @click="handleSubmit">提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 760px; margin: 0 auto; }
.filters { display: flex; gap: 12px; margin: 16px 0 24px; flex-wrap: wrap; }
.filter-select { width: 220px; }
.list { display: flex; flex-direction: column; gap: 12px; }
.card { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.info { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.name { font-size: 15px; font-weight: 600; }
.desc { font-size: 13px; color: var(--n-text-color-2); line-height: 1.5; }
.meta { font-size: 12px; color: var(--n-text-color-3); }
.project-modal { width: min(520px, calc(100vw - 32px)); }
.submit-hint { margin-bottom: 14px; }
.grading-summary { margin-bottom: 14px; }
.grading-summary strong { display: block; margin-bottom: 8px; }
.dimension-results { display: flex; flex-wrap: wrap; gap: 8px 16px; font-size: 13px; }
.upload-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.hidden-input { display: none; }
.uploaded-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.uploaded-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 10px; border-radius: 6px; background: var(--n-color-embedded); font-size: 13px; }
.note-field { display: grid; gap: 6px; font-size: 13px; color: var(--n-text-color-2); }
.note-field > span { font-weight: 600; color: var(--n-text-color-1); }
.note-field small { color: var(--n-text-color-3); line-height: 1.5; }
@media (max-width: 640px) {
  .filter-select { width: 100%; }
  .card { align-items: stretch; flex-direction: column; }
  .card-actions :deep(.n-button) { min-height: 44px; }
}
</style>
