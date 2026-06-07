<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NAlert, NButton, NEmpty, NIcon, NInput, NModal, NSelect, NSpace, NSpin, NTag, useMessage } from 'naive-ui'
import { CloudUploadOutline, FolderOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { uploadDriveFile } from '@/api/drive'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { createProjectTeam, listProjects, submitProject } from '@/api/projects'
import { defaultProjectArtifact, parseProjectDescription } from '@/types/project'
import type { DriveItemVO, ProjectVO, SemesterVO } from '@/types/api'

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const projects = ref<ProjectVO[]>([])
const showSubmit = ref(false)
const activeProject = ref<ProjectVO | null>(null)
const submitNote = ref('')
const uploadedItems = ref<DriveItemVO[]>([])
const uploadLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)
const showTeam = ref(false)
const teamName = ref('')

const activeConfig = computed(() => activeProject.value ? parseProjectDescription(activeProject.value).artifact : defaultProjectArtifact)
const extensionLabel = computed(() => activeConfig.value.allowedExtensions.length ? activeConfig.value.allowedExtensions.map((ext: string) => `.${ext}`).join('、') : '不限格式')
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

function openSubmit(project: ProjectVO) {
  activeProject.value = project
  submitNote.value = ''
  uploadedItems.value = []
  showSubmit.value = true
}

function openTeamModal(project: ProjectVO) {
  activeProject.value = project
  teamName.value = ''
  showTeam.value = true
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
  } catch (e) {
    message.error(getErrorMessage(e, '提交失败'))
  }
}

async function handleJoinTeam() {
  if (!activeProject.value) return
  if (!teamName.value.trim()) {
    message.warning('请输入队伍名称')
    return
  }
  try {
    await createProjectTeam(activeProject.value.id, teamName.value.trim())
    message.success('组队成功')
    showTeam.value = false
  } catch (e) {
    message.error(getErrorMessage(e, '组队失败'))
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="项目化学习" subtitle="参与项目、组队并提交作品" />
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
            <span class="meta">组队上限 {{ p.maxTeamSize }} 人 · 截止 {{ p.deadline ? formatDate(p.deadline, 'datetime') : '未设置' }}</span>
          </div>
          <NSpace :size="8" class="card-actions">
            <NButton size="small" @click="openTeamModal(p)">创建队伍</NButton>
            <NButton size="small" type="primary" @click="openSubmit(p)">提交作品</NButton>
          </NSpace>
        </div>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无项目" />
    </NSpin>

    <NModal v-model:show="showTeam" title="创建队伍" preset="card" class="project-modal">
      <NInput v-model:value="teamName" placeholder="输入队伍名称" />
      <template #footer><NSpace justify="end"><NButton @click="showTeam = false">取消</NButton><NButton type="primary" @click="handleJoinTeam">创建</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showSubmit" :title="activeProject ? `提交作品 · ${activeProject.name}` : '提交作品'" preset="card" class="project-modal">
      <NAlert type="info" :bordered="false" class="submit-hint">
        提交方式：{{ activeConfig.submitMode === 'folder' ? '文件夹' : '文件' }}；文件格式：{{ extensionLabel }}
      </NAlert>
      <div class="upload-actions">
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
      <NInput v-model:value="submitNote" type="textarea" placeholder="作品说明、链接或补充信息" :autosize="{ minRows: 3, maxRows: 8 }" />
      <template #footer><NSpace justify="end"><NButton @click="showSubmit = false">取消</NButton><NButton type="primary" @click="handleSubmit">提交</NButton></NSpace></template>
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
.upload-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.hidden-input { display: none; }
.uploaded-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.uploaded-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 10px; border-radius: 6px; background: var(--n-color-embedded); font-size: 13px; }
@media (max-width: 640px) {
  .filter-select { width: 100%; }
  .card { align-items: stretch; flex-direction: column; }
}
</style>
