<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { NDatePicker, NEmpty, NButton, NDataTable, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { listSemesters } from '@/api/semesters'
import { createExam, deleteExam, listExamPapers, listExams, updateExam } from '@/api/exams'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { ExamPaperVO, ExamVO, SemesterVO } from '@/types/api'

const props = defineProps<{ courseId: number; semesterId?: number | null }>()
const message = useMessage()
const semesters = ref<SemesterVO[]>([])
const activeSemesterId = ref<number | null>(null)
const exams = ref<ExamVO[]>([])
const papers = ref<ExamPaperVO[]>([])
const showModal = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', paperId: null as number | null, timeRange: null as [number, number] | null })
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))
const showSemesterPicker = computed(() => props.semesterId === undefined)
const paperOptions = computed(() => papers.value.map(paper => ({
  label: paper.title,
  value: paper.id,
})))

const columns: DataTableColumns<ExamVO> = [
  { title: '名称', key: 'name' },
  { title: '开始', key: 'startTime', width: 120, render: row => formatDate(row.startTime, 'datetime') },
  { title: '结束', key: 'endTime', width: 120, render: row => formatDate(row.endTime, 'datetime') },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: row => h(NSpace, { size: 2 }, () => [
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
        trigger: () => h(NButton, { size: 'tiny', quaternary: true, title: '删除考试', 'aria-label': '删除考试' }, () => h(NIcon, { size: 14 }, () => h(TrashOutline))),
        default: () => '确认删除？',
      }),
    ]),
  },
]

async function loadSemesters() {
  if (!showSemesterPicker.value) return
  try {
    semesters.value = await listSemesters(props.courseId) || []
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
}
watch(() => props.courseId, () => { loadSemesters() }, { immediate: true })
watch(() => props.semesterId, value => {
  if (!showSemesterPicker.value) activeSemesterId.value = value ?? null
}, { immediate: true })

async function loadExams() {
  if (!activeSemesterId.value) {
    exams.value = []
    return
  }
  try {
    exams.value = await listExams(activeSemesterId.value) || []
  } catch (e) {
    exams.value = []
    message.error(getErrorMessage(e, '加载考试列表失败'))
  }
}
watch(activeSemesterId, loadExams)

async function loadPapers() {
  try {
    papers.value = await listExamPapers() || []
  } catch (e) {
    papers.value = []
    message.error(getErrorMessage(e, '加载试卷列表失败'))
  }
}
watch(() => props.courseId, loadPapers, { immediate: true })

function openCreate() {
  editingId.value = null
  form.value = { name: '', paperId: null, timeRange: null }
  showModal.value = true
}

async function handleSubmit() {
  if (!activeSemesterId.value) {
    message.error('请先选择学期')
    return
  }
  try {
    const body = {
      name: form.value.name,
      paperId: form.value.paperId,
      startTime: form.value.timeRange ? toLocalDateTime(form.value.timeRange[0]) : '',
      endTime: form.value.timeRange ? toLocalDateTime(form.value.timeRange[1]) : '',
      weight: 1,
    }
    if (editingId.value) {
      await updateExam(editingId.value, body)
    } else {
      await createExam(activeSemesterId.value, body)
    }
    message.success(editingId.value ? '已更新' : '已创建')
    showModal.value = false
    loadExams()
  } catch (e) {
    message.error(getErrorMessage(e))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteExam(id)
    message.success('已删除')
    loadExams()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}
</script>

<template>
  <div>
    <div class="exam-toolbar">
      <NSelect v-if="showSemesterPicker" v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="semester-select" />
      <span v-else class="semester-hint">当前学期考试</span>
      <NButton v-if="activeSemesterId" size="small" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建考试</NButton>
    </div>
    <NDataTable v-if="exams.length" :data="exams" :columns="columns" size="small" :row-key="r => r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无考试" class="empty-state" />
    <NModal v-model:show="showModal" :title="editingId?'编辑考试':'创建考试'" preset="card" class="exam-modal">
      <NForm label-placement="left" label-width="64">
        <NFormItem label="名称"><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="试卷"><NSelect v-model:value="form.paperId" :options="paperOptions" /></NFormItem>
        <NFormItem label="考试时间"><NDatePicker v-model:value="form.timeRange" type="datetimerange" clearable class="date-picker" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal=false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.exam-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.semester-select { width: 200px; }
.semester-hint { font-size: 13px; color: var(--n-text-color-3); }
.empty-state { padding: 40px 0; }
.exam-modal { width: 480px; }
.date-picker { width: 100%; }
</style>
