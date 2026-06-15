<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import {
  NButton,
  NDataTable,
  NEmpty,
  NIcon,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  NSpin,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { AddOutline, EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { listSemesters } from '@/api/semesters'
import { createProject, deleteProject, listProjects } from '@/api/projects'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { useProjectSubmissionScoring } from '@/composables/useProjectSubmissionScoring'
import ProjectCreateModal from '@/components/project/ProjectCreateModal.vue'
import ProjectSubmissionModal from '@/components/project/ProjectSubmissionModal.vue'
import { buildProjectDescription, createEmptyProjectForm, parseProjectDescription } from '@/types/project'
import type { ProjectVO, SemesterVO } from '@/types/api'

const props = defineProps<{ courseId: number; semesterId?: number | null }>()
const message = useMessage()
const semesters = ref<SemesterVO[]>([])
const activeSemesterId = ref<number | null>(null)
const projects = ref<ProjectVO[]>([])
const showModal = ref(false)
const form = ref(createEmptyProjectForm())
const {
  showSubmissions,
  submissionRows,
  activeProjectRubric,
  submissionModalTitle,
  previewUrl,
  previewTitle,
  previewLoading,
  openSubmissions,
  previewFile,
  downloadFile,
  closePreview,
  getProjectScore,
  setProjectScore,
  saveProjectScore,
} = useProjectSubmissionScoring()

const columns: DataTableColumns<ProjectVO> = [
  { title: '名称', key: 'name' },
  { title: '说明', key: 'description', ellipsis: { tooltip: true } },
  { title: '组队上限', key: 'maxTeamSize', width: 90 },
  { title: '截止', key: 'deadline', width: 150, render: row => row.deadline ? formatDate(row.deadline, 'datetime') : '-' },
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
    width: 110,
    render: row => h(NSpace, { size: 2 }, () => [
      h(NButton, { size: 'tiny', quaternary: true, title: '查看提交', 'aria-label': '查看提交', onClick: () => openSubmissions(row) }, () => h(NIcon, { size: 14 }, () => h(EyeOutline))),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, title: '删除项目', 'aria-label': '删除项目' }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))),
        default: () => '确认删除？',
      }),
    ]),
  },
]

const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))
const showSemesterPicker = computed(() => props.semesterId === undefined)

watch(() => props.courseId, async () => {
  if (!showSemesterPicker.value) return
  try {
    semesters.value = await listSemesters(props.courseId) || []
    activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
  } catch (e) {
    semesters.value = []
    activeSemesterId.value = null
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
}, { immediate: true })
watch(() => props.semesterId, value => {
  if (!showSemesterPicker.value) activeSemesterId.value = value ?? null
}, { immediate: true })

async function loadProjects() {
  if (!activeSemesterId.value) {
    projects.value = []
    return
  }
  try {
    projects.value = await listProjects(activeSemesterId.value) || []
  } catch (e) {
    projects.value = []
    message.error(getErrorMessage(e, '加载项目列表失败'))
  }
}
watch(activeSemesterId, loadProjects)

function pickCurrentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(s => new Date(s.startTime).getTime() <= now && now <= new Date(s.endTime).getTime())
}

function openCreate() {
  form.value = createEmptyProjectForm()
  showModal.value = true
}

async function handleSubmit() {
  if (!activeSemesterId.value) return
  try {
    await createProject(activeSemesterId.value, {
      name: form.value.name,
      description: buildProjectDescription(form.value),
      maxTeamSize: form.value.maxTeamSize,
      deadline: toLocalDateTime(form.value.deadline),
      weight: 1,
    })
    message.success('已创建')
    showModal.value = false
    loadProjects()
  } catch (e) {
    message.error(getErrorMessage(e))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteProject(id)
    message.success('已删除')
    loadProjects()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

</script>

<template>
  <div>
    <div class="toolbar">
      <NSelect v-if="showSemesterPicker" v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" />
      <span v-else class="semester-hint">当前学期项目</span>
      <NButton v-if="activeSemesterId" size="small" @click="openCreate">
        <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>
        创建项目
      </NButton>
    </div>
    <NDataTable v-if="projects.length" :data="projects" :columns="columns" size="small" :row-key="r => r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无项目" class="empty-state" />

    <ProjectCreateModal v-model:show="showModal" v-model:form="form" @submit="handleSubmit" />

    <ProjectSubmissionModal
      v-model:show="showSubmissions"
      :title="submissionModalTitle"
      :rows="submissionRows"
      :rubric="activeProjectRubric"
      :get-score="getProjectScore"
      @preview="previewFile"
      @download="downloadFile"
      @score-change="setProjectScore"
      @save-score="saveProjectScore"
    />

    <NModal
      :show="!!previewTitle"
      preset="card"
      :title="previewTitle"
      class="preview-modal"
      :bordered="false"
      @update:show="v => { if (!v) closePreview() }"
    >
      <div class="preview-body">
        <NSpin v-if="previewLoading" />
        <iframe v-else-if="previewUrl" :src="previewUrl" class="preview-frame" />
        <NEmpty v-else description="暂无预览" />
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; }
.toolbar-select { width: 220px; }
.semester-hint { font-size: 13px; color: var(--n-text-color-3); }
.empty-state { padding: 40px 0; }
.preview-modal { width: 90vw; max-width: 1100px; height: 85vh; }
.preview-body { height: calc(85vh - 90px); display: flex; align-items: stretch; justify-content: stretch; min-height: 0; }
.preview-frame { display: block; width: 100%; height: 100%; min-width: 100%; min-height: 100%; border: 0; flex: 1 1 auto; }
@media (max-width: 720px) {
  .toolbar { align-items: stretch; flex-direction: column; }
  .toolbar-select { width: 100%; }
}
</style>
