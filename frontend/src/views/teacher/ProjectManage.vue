<script setup lang="ts">
import { ref, onMounted, h, watch } from 'vue'
import { NDatePicker, NEmpty, NButton, NDataTable, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { createProject, deleteProject, listProjects, updateProject } from '@/api/projects'
import PageHeader from '@/components/PageHeader.vue'
import { useCourseSemesterPicker } from '@/composables/useCourseSemesterPicker'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { DataTableColumns } from 'naive-ui'
import type { ProjectVO } from '@/types/api'

interface ProjectForm {
  name: string
  description: string
  maxTeamSize: number
  deadline: number | null
}

const message = useMessage()
const { activeCourseId, activeSemesterId, courseOptions, semesterOptions, loadCourses } = useCourseSemesterPicker()
const projects = ref<ProjectVO[]>([])
const showModal = ref(false)
const editingId = ref<number | null>(null)
const form = ref<ProjectForm>({ name: '', description: '', maxTeamSize: 1, deadline: null })

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
  form.value = { name: '', description: '', maxTeamSize: 1, deadline: null }
  showModal.value = true
}

function openEdit(row: ProjectVO) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    description: row.description || '',
    maxTeamSize: row.maxTeamSize,
    deadline: row.deadline ? new Date(row.deadline).getTime() : null,
  }
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
  if (form.value.maxTeamSize < 1) {
    message.warning('组队上限不能小于 1')
    return
  }
  try {
    const body = {
      ...form.value,
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
  { title: '说明', key: 'description', ellipsis: { tooltip: true } },
  { title: '组队上限', key: 'maxTeamSize', width: 90 },
  { title: '截止', key: 'deadline', width: 120, render: row => row.deadline ? formatDate(row.deadline, 'date') : '-' },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row => h(NSpace, { size: 2 }, () => [
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
    <NModal v-model:show="showModal" :title="editingId ? '编辑项目' : '创建项目'" preset="card" class="form-modal">
      <NForm label-placement="left" label-width="72">
        <NFormItem label="名称" required><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="说明"><NInput v-model:value="form.description" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" /></NFormItem>
        <NFormItem label="组队上限"><NInput :value="String(form.maxTeamSize)" @update:value="v => { form.maxTeamSize = Number(v) || 1 }" /></NFormItem>
        <NFormItem label="截止时间"><NDatePicker v-model:value="form.deadline" type="datetime" clearable class="date-picker" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal = false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.toolbar { margin: 16px 0; display: flex; gap: 12px; flex-wrap: wrap; }
.toolbar-select { width: 220px; }
.form-modal { width: min(480px, calc(100vw - 32px)); }
.date-picker { width: 100%; }
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
}
</style>
