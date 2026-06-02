<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, NSpace, NIcon, NModal, NForm, NFormItem, NInput, NSelect, NPopconfirm, NEmpty, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { listTasks, createTask, updateTask, deleteTask } from '@/api/tasks'
import { formatDate } from '@/utils/date'
import WorksheetEditor from '@/components/WorksheetEditor.vue'
import type { TaskVO, TaskCreateDTO, TaskUpdateDTO } from '@/types/api'

const router = useRouter()

const props = defineProps<{
  lessonId: number
  readonly?: boolean
}>()

const message = useMessage()
const tasks = ref<TaskVO[]>([])
const loading = ref(false)

// Task modal
const showModal = ref(false)
const editingId = ref<number | null>(null)
const formValue = ref<TaskCreateDTO>({ title: '', type: 'worksheet', description: '', formSchema: '', deadline: '' })

const typeOptions = [
  { label: '学习单', value: 'worksheet' },
  { label: '课堂作品', value: 'artifact' },
]

async function loadTasks() {
  loading.value = true
  try { tasks.value = await listTasks(props.lessonId) }
  catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  formValue.value = { title: '', type: 'worksheet', description: '', formSchema: '', deadline: '' }
  showModal.value = true
}

function openEdit(task: TaskVO) {
  editingId.value = task.id
  formValue.value = {
    title: task.title,
    type: task.type,
    description: task.description || '',
    formSchema: '',
    deadline: task.deadline || '',
  }
  showModal.value = true
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      const dto: TaskUpdateDTO = { title: formValue.value.title, description: formValue.value.description || undefined, deadline: formValue.value.deadline || undefined }
      await updateTask(editingId.value, dto)
      message.success('已更新')
    } else {
      await createTask(props.lessonId, formValue.value)
      message.success('已创建')
    }
    showModal.value = false
    await loadTasks()
  } catch (e: any) { message.error(e.message || '操作失败') }
}

async function handleDelete(id: number) {
  try {
    await deleteTask(id)
    message.success('已删除')
    await loadTasks()
  } catch (e: any) { message.error(e.message || '删除失败') }
}

const typeLabel = (t: string) => t === 'worksheet' ? '学习单' : '课堂作品'
const typeColor = (t: string) => t === 'worksheet' ? '#7C3AED' : '#F97316'

onMounted(loadTasks)
</script>

<template>
  <div class="task-panel">
    <div class="task-head">
      <span class="task-label">课堂任务</span>
      <NButton v-if="!readonly" size="tiny" quaternary @click="openCreate">
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
          <span class="task-meta">{{ t.submissionCount }} 人提交</span>
          <span v-if="t.deadline" class="task-deadline">截止 {{ formatDate(t.deadline, 'date') }}</span>
        </div>
        <NButton v-if="readonly" size="tiny" @click="router.push(`/student/tasks/${t.id}`)">
          <template #icon><NIcon :size="14"><CreateOutline /></NIcon></template>作答
        </NButton>
        <NSpace v-if="!readonly" :size="2">
          <NButton size="tiny" quaternary @click="openEdit(t)">
            <template #icon><NIcon :size="14"><CreateOutline /></NIcon></template>
          </NButton>
          <NPopconfirm @positive-click="() => handleDelete(t.id)">
            <template #trigger>
              <NButton size="tiny" quaternary>
                <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
              </NButton>
            </template>
            确定删除「{{ t.title }}」？
          </NPopconfirm>
        </NSpace>
      </div>
    </div>

    <div v-else class="task-empty">暂无任务</div>

    <!-- Create/Edit Modal -->
    <NModal v-model:show="showModal" :title="editingId ? '编辑任务' : '创建任务'" preset="card" style="width:560px">
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
          <NInput v-model:value="formValue.deadline" placeholder="如：2027-06-30T23:59:59" />
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
.task-empty { font-size: 12px; color: var(--n-text-color-3); padding: 8px 0; }
</style>
