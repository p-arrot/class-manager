<script setup lang="ts">
import { ref, onMounted, h, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NEmpty, NButton, NDataTable, NSelect, NSpace, NIcon, NPopconfirm, NTag, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { createProject, deleteProject, listProjects, updateProject } from '@/api/projects'
import PageHeader from '@/components/PageHeader.vue'
import ProjectCreateModal from '@/components/project/ProjectCreateModal.vue'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { buildProjectDescription, createEmptyProjectForm, createProjectFormFromProject, parseProjectDescription } from '@/types/project'
import type { DataTableColumns } from 'naive-ui'
import type { ProjectVO } from '@/types/api'

const message = useMessage()
const router = useRouter()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const projects = ref<ProjectVO[]>([])
const showModal = ref(false)
const editingId = ref<number | null>(null)
const form = ref(createEmptyProjectForm())

async function loadProjects() {
  if (!activeSemesterId.value) {
    projects.value = []
    return
  }
  try {
    projects.value = await listProjects(activeSemesterId.value)
  } catch (e) {
    projects.value = []
    message.error(getErrorMessage(e, '加载项目列表失败'))
  }
}

function openCreate() {
  editingId.value = null
  form.value = createEmptyProjectForm()
  showModal.value = true
}

function openEdit(row: ProjectVO) {
  editingId.value = row.id
  form.value = createProjectFormFromProject(row)
  showModal.value = true
}
async function handleSubmit() {
  if (!activeSemesterId.value) {
    message.warning('请先选择学期')
    return
  }
  if (!form.value.name.trim()) {
    message.warning('请输入项目名称')
    return
  }
  try {
    const body = {
      name: form.value.name,
      description: buildProjectDescription(form.value),
      deadline: toLocalDateTime(form.value.deadline),
      weight: 1,
    }
    if (editingId.value) {
      await updateProject(editingId.value, body)
    } else {
      await createProject(activeSemesterId.value, body)
    }
    message.success(editingId.value ? '已更新' : '已创建')
    showModal.value = false
    await loadProjects()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteProject(id)
    message.success('已删除')
    await loadProjects()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}
const projectColumns: DataTableColumns<ProjectVO> = [
  { title: '名称', key: 'name' },
  { title: '说明', key: 'description', ellipsis: { tooltip: true }, render: row => parseProjectDescription(row).text || '-' },
  { title: '截止', key: 'deadline', width: 120, render: row => row.deadline ? formatDate(row.deadline, 'date') : '-' },
  {
    title: '提交要求',
    key: 'submit',
    width: 150,
    render: row => {
      const config = parseProjectDescription(row).artifact
      return h(NSpace, { size: 4 }, () => [
        h(NTag, { size: 'tiny', bordered: false }, () => config.submitMode === 'folder' ? '文件夹' : '文件'),
        h(NTag, { size: 'tiny', bordered: false }, () => config.allowedExtensions.length ? config.allowedExtensions.map(ext => `.${ext}`).join(' ') : '不限格式'),
      ])
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: row => h(NSpace, { size: 2 }, () => [
      h(NButton, { size: 'tiny', quaternary: true, title: '查看提交和批改', 'aria-label': '查看提交和批改', onClick: () => router.push(`/teacher/projects/${row.id}/submissions`) }, () => h(NIcon, { size: 14 }, () => h(EyeOutline))),
      h(NButton, { size: 'tiny', quaternary: true, title: '编辑项目', 'aria-label': '编辑项目', onClick: () => openEdit(row) }, () => h(NIcon, { size: 14 }, () => h(CreateOutline))),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, title: '删除项目', 'aria-label': '删除项目' }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))),
        default: () => '确认删除？',
      }),
    ]),
  },
]
watch(activeSemesterId, loadProjects)

onMounted(async () => {
  try {
    await loadCourses()
  } catch (e) {
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="项目管理" subtitle="创建和管理项目化学习任务">
      <template #actions>
        <NButton size="small" :disabled="!activeSemesterId" @click="openCreate">
          <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建项目
        </NButton>
      </template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" :disabled="!activeCourseId" />
    </div>
    <NDataTable v-if="projects.length" :data="projects" :columns="projectColumns" size="small" :row-key="(r: ProjectVO) => r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无项目" />
    <NEmpty v-else description="请选择课程和学期后管理项目" />

    <ProjectCreateModal
      v-model:show="showModal"
      v-model:form="form"
      :title="editingId ? '编辑项目' : '创建项目'"
      @submit="handleSubmit"
    />

  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.toolbar { margin: 16px 0; display: flex; gap: 12px; flex-wrap: wrap; }
.toolbar-select { width: 220px; }
.preview-modal { width: 90vw; max-width: 1100px; height: 85vh; }
.preview-body { height: calc(85vh - 90px); display: flex; align-items: stretch; justify-content: stretch; min-height: 0; }
.preview-frame { display: block; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: 0; flex: 1 1 auto; }
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
}
</style>
