<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NDatePicker, NTag, NSpace, NIcon, NModal, NForm, NFormItem, NInput, NSelect, NPopconfirm, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { listTasks, getTask, createTask, updateTask, deleteTask } from '@/api/tasks'
import { formatDate, toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import WorksheetEditor from '@/components/WorksheetEditor.vue'
import { useAuthStore } from '@/stores/auth'
import type { TaskVO, TaskCreateDTO, TaskUpdateDTO } from '@/types/api'
import http from '@/api/request'

interface SubmissionStatus {
  status: string
  id: number
}

interface SubmissionStats {
  submitted: number
  graded: number
  total: number
}

interface TaskFormValue {
  title: string
  type: TaskCreateDTO['type']
  description: string
  formSchema: string
  deadline: number | null
}

const router = useRouter()
const auth = useAuthStore()

const props = defineProps<{
  lessonId: number
  readonly?: boolean
}>()

const message = useMessage()
const tasks = ref<TaskVO[]>([])
const loading = ref(false)
const mySubmissions = ref<Record<number, SubmissionStatus>>({})

// Task modal
const showModal = ref(false)
const editingId = ref<number | null>(null)
const formValue = ref<TaskFormValue>({ title: '', type: 'worksheet', description: '', formSchema: '', deadline: null })

const typeOptions = [
  { label: '学习单', value: 'worksheet' },
  { label: '课堂作品', value: 'artifact' },
]

const taskStats = ref<Record<number, { submitted: number; total: number }>>({})

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await listTasks(props.lessonId)
  } catch (e) {
    tasks.value = []
    message.error(getErrorMessage(e, '加载任务列表失败'))
  } finally {
    loading.value = false
  }
  // Teacher: load submission stats
  if (!props.readonly) {
    const entries = await Promise.all(tasks.value.map(async (t) => {
      try {
        const stats = await http.get<SubmissionStats>(`/tasks/${t.id}/submission-stats`)
        return stats ? [t.id, { submitted: stats.submitted + stats.graded, total: stats.total }] as const : null
      } catch {
        return null
      }
    }))
    taskStats.value = Object.fromEntries(entries.filter(entry => entry !== null))
  }
  // Student: check own submission status
  if (props.readonly && auth.userInfo?.userId) {
    const entries = await Promise.all(tasks.value.map(async (t) => {
      try {
        const mine = await http.get<SubmissionStatus | null>(`/tasks/${t.id}/my-submission`)
        return mine ? [t.id, { status: mine.status, id: mine.id }] as const : null
      } catch {
        return null
      }
    }))
    mySubmissions.value = Object.fromEntries(entries.filter(entry => entry !== null))
  }
}

function openCreate() {
  editingId.value = null
  formValue.value = { title: '', type: 'worksheet', description: '', formSchema: '', deadline: null }
  showModal.value = true
}

async function openEdit(task: TaskVO) {
  editingId.value = task.id
  formValue.value = {
    title: task.title,
    type: task.type,
    description: task.description || '',
    formSchema: '',
    deadline: task.deadline ? new Date(task.deadline).getTime() : null,
  }
  // Load full task detail to get formSchema
  try {
    const detail = await getTask(task.id)
    if (detail?.formSchema) formValue.value.formSchema = detail.formSchema
  } catch (e) {
    message.error(getErrorMessage(e, '加载任务详情失败'))
  }
  showModal.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      const dto: TaskUpdateDTO = {
        title: formValue.value.title,
        description: formValue.value.description || undefined,
        deadline: toLocalDateTime(formValue.value.deadline),
        formSchema: formValue.value.formSchema || undefined,
      }
      await updateTask(editingId.value, dto)
      message.success('已更新')
    } else {
      await createTask(props.lessonId, {
        title: formValue.value.title,
        type: formValue.value.type,
        description: formValue.value.description || undefined,
        deadline: toLocalDateTime(formValue.value.deadline),
        formSchema: formValue.value.formSchema || undefined,
      })
      message.success('已创建')
    }
    showModal.value = false
    await loadTasks()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTask(id)
    message.success('已删除')
    await loadTasks()
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

const typeLabel = (t: string) => t === 'worksheet' ? '学习单' : '课堂作品'
const typeColor = (t: string) => t === 'worksheet' ? '#7C3AED' : '#F97316'
const studentTaskPath = (taskId: number) => mySubmissions.value[taskId]?.status === 'graded'
  ? `/student/tasks/${taskId}/result`
  : `/student/tasks/${taskId}`
const studentTaskAction = (taskId: number) => {
  const status = mySubmissions.value[taskId]?.status
  if (status === 'graded') return '详情'
  return status ? '查看' : '作答'
}

onMounted(loadTasks)
</script>

<template>
  <div class="task-panel">
    <div class="task-head">
      <span class="task-label">课堂任务</span>
      <NButton v-if="!readonly" size="tiny" quaternary @click="router.push(`/teacher/tasks/create/${props.lessonId}`)">
        <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>
        创建任务
      </NButton>
    </div>

    <div v-if="tasks.length" class="task-list">
      <div v-for="t in tasks" :key="t.id" class="task-row">
        <div class="task-info">
          <NTag :color="{ color: typeColor(t.type), textColor: '#fff' }" size="tiny" :bordered="false">
            {{ typeLabel(t.type) }}
          </NTag>
          <span class="task-title">{{ t.title }}</span>
          <span v-if="!readonly && taskStats[t.id]" class="task-meta">{{ taskStats[t.id].submitted }}/{{ taskStats[t.id].total }} 已提交</span>
          <span v-if="t.deadline" class="task-deadline">截止 {{ formatDate(t.deadline, 'date') }}</span>
          <NTag v-if="readonly && mySubmissions[t.id]?.status === 'submitted'" size="tiny" type="warning" :bordered="false">已提交 · 待评分</NTag>
          <NTag v-if="readonly && mySubmissions[t.id]?.status === 'graded'" size="tiny" type="success" :bordered="false">已评分</NTag>
          <NTag v-if="readonly && !mySubmissions[t.id]" size="tiny" :bordered="false" class="muted-tag">未提交</NTag>
        </div>
        <NButton v-if="readonly" size="tiny" :type="mySubmissions[t.id] ? 'default' : 'primary'" @click="router.push(studentTaskPath(t.id))">
          <template #icon><NIcon :size="14"><CreateOutline /></NIcon></template>{{ studentTaskAction(t.id) }}
        </NButton>
        <NSpace v-if="!readonly" :size="2">
          <NButton size="tiny" @click="router.push(`/teacher/tasks/${t.id}/analytics`)">数据</NButton>
          <NButton size="tiny" @click="router.push(`/teacher/grading/${t.id}`)">评分</NButton>
          <NButton size="tiny" quaternary title="编辑任务" aria-label="编辑任务" @click="openEdit(t)">
            <template #icon><NIcon :size="14"><CreateOutline /></NIcon></template>
          </NButton>
          <NPopconfirm @positive-click="() => handleDelete(t.id)">
            <template #trigger>
              <NButton size="tiny" quaternary title="删除任务" aria-label="删除任务">
                <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
              </NButton>
            </template>
            确定删除「{{ t.title }}」？
          </NPopconfirm>
        </NSpace>
      </div>
    </div>

    <div v-else-if="!loading" class="task-empty">
      <span>暂无任务</span>
      <NButton v-if="!readonly" size="tiny" text type="primary" @click="openCreate()">+ 创建第一个任务</NButton>
    </div>

    <!-- Create/Edit Modal -->
    <NModal v-model:show="showModal" :title="editingId ? '编辑任务' : '创建任务'" preset="card" class="task-modal">
      <NForm label-placement="left" label-width="64">
        <NFormItem label="标题" required>
          <NInput v-model:value="formValue.title" placeholder="任务标题" />
        </NFormItem>
        <NFormItem label="类型" required>
          <NSelect v-model:value="formValue.type" :options="typeOptions" :disabled="!!editingId" />
        </NFormItem>
        <NFormItem label="说明">
          <NInput v-model:value="formValue.description" type="textarea" placeholder="任务说明（可选）" :autosize="{ minRows: 2, maxRows: 4 }" />
        </NFormItem>
        <NFormItem v-if="formValue.type === 'worksheet'" label="题目">
          <WorksheetEditor v-model="formValue.formSchema" />
        </NFormItem>
        <NFormItem label="截止时间">
          <NDatePicker v-model:value="formValue.deadline" type="datetime" clearable class="date-picker" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.task-panel {
  border-top: 1px solid var(--n-border-color);
  padding: 10px 0 4px;
  margin-top: 4px;
}
.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.task-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--n-text-color-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.task-list { display: flex; flex-direction: column; gap: 4px; }
.task-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 8px; border-radius: 6px;
}
.task-row:hover { background: var(--n-color-embedded); }
.task-info { display: flex; align-items: center; gap: 8px; min-width: 0; }
.task-title { font-size: 13px; font-weight: 500; }
.task-meta { font-size: 11px; color: var(--n-text-color-3); }
.task-deadline { font-size: 11px; color: var(--n-text-color-3); }
.muted-tag { opacity: 0.5; }
.task-empty { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px dashed var(--n-border-color); border-radius: 6px; font-size: 13px; color: var(--n-text-color-3); }
.task-modal { width: 560px; }
.date-picker { width: 100%; }
</style>
