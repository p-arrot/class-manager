<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NCard, NIcon, NInput, NSpin, NSpace, NTag, useMessage } from 'naive-ui'
import { ArrowBackOutline, CloudUploadOutline, FolderOutline } from '@vicons/ionicons5'
import { getMyTaskSubmission, getTask, submitTask } from '@/api/tasks'
import { uploadDriveFile } from '@/api/drive'
import WorksheetRenderer from '@/components/WorksheetRenderer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import { parseTaskSchema, questionStem } from '@/types/taskSchema'
import type { ArtifactSchema, WorksheetAnswerMap } from '@/types/taskSchema'
import type { DriveItemVO, SubmissionVO, TaskDetailVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const taskId = Number(route.params.taskId)

const task = ref<TaskDetailVO | null>(null)
const submission = ref<SubmissionVO | null>(null)
const submitted = ref(false)
const loading = ref(false)
const worksheetAnswer = ref<WorksheetAnswerMap>({})
const artifactNote = ref('')
const uploadedItems = ref<DriveItemVO[]>([])
const uploadLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)

const schema = computed(() => parseTaskSchema(task.value?.formSchema))
const artifactSchema = computed<ArtifactSchema>(() => schema.value.artifact ?? { submitMode: 'file', allowedExtensions: [] })
const extensionLabel = computed(() => artifactSchema.value.allowedExtensions.length ? artifactSchema.value.allowedExtensions.map(ext => `.${ext}`).join('、') : '不限格式')

async function loadTask() {
  loading.value = true
  try {
    const [taskData, existing] = await Promise.all([getTask(taskId), getMyTaskSubmission(taskId)])
    task.value = taskData
    submission.value = existing
    if (existing && ['graded', 'special'].includes(existing.status)) {
      await router.replace(`/student/tasks/${taskId}/result`)
      return
    }
    if (existing?.content) restoreSubmission(existing.content)
  } catch (error) {
    message.error(getErrorMessage(error, '加载失败'))
    router.back()
  } finally {
    loading.value = false
  }
}

function restoreSubmission(content: string) {
  try {
    const parsed = JSON.parse(content) as Record<string, unknown>
    if (task.value?.type === 'worksheet') {
      worksheetAnswer.value = parsed as WorksheetAnswerMap
      return
    }
    artifactNote.value = typeof parsed.note === 'string' ? parsed.note : ''
    uploadedItems.value = Array.isArray(parsed.files) ? parsed.files.filter(item => item && typeof item === 'object') as DriveItemVO[] : []
  } catch {
    if (task.value?.type === 'artifact') artifactNote.value = content
  }
}

function validateWorksheet() {
  const questions = schema.value.questions ?? []
  for (const question of questions) {
    if (!question.required) continue
    const answer = worksheetAnswer.value[question.id]
    if (answer == null || answer === '' || (Array.isArray(answer) && !answer.length)) {
      message.warning(`请完成必填题：${questionStem(question).slice(0, 24) || '未命名题目'}`)
      return false
    }
  }
  return true
}

function fileAllowed(file: File) {
  const allowed = artifactSchema.value.allowedExtensions
  if (!allowed.length || artifactSchema.value.submitMode === 'folder') return true
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
      const item = await uploadDriveFile(fd)
      uploadedItems.value.push(item)
    }
    message.success(`${list.length} 个文件已上传`)
  } catch (error) {
    message.error(getErrorMessage(error, '上传失败'))
  } finally {
    uploadLoading.value = false
  }
}

async function handleFileInput(event: Event) {
  const files = (event.target as HTMLInputElement).files
  if (files?.length) await uploadFiles(files)
  ;(event.target as HTMLInputElement).value = ''
}

async function handleSubmit() {
  if (!task.value) return
  if (task.value.type === 'worksheet' && !validateWorksheet()) return
  if (task.value.type === 'artifact' && !uploadedItems.value.length && !artifactNote.value.trim()) {
    message.warning('请上传作品或填写作品说明')
    return
  }

  loading.value = true
  try {
    const content = task.value.type === 'worksheet'
      ? JSON.stringify(worksheetAnswer.value)
      : JSON.stringify({
          note: artifactNote.value,
          submitMode: artifactSchema.value.submitMode,
          files: uploadedItems.value.map(item => ({ id: item.id, name: item.name, fileSize: item.fileSize, type: item.type })),
        })
    await submitTask(taskId, { content })
    submitted.value = true
  } catch (error) {
    message.error(getErrorMessage(error, '提交失败'))
  } finally {
    loading.value = false
  }
}

function typeLabel(type: string) {
  return type === 'worksheet' ? '学习单' : '课堂作品'
}

onMounted(loadTask)
</script>

<template>
  <div class="page">
    <NButton text @click="router.back()">
      <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
      返回
    </NButton>

    <NSpin :show="loading">
      <div v-if="submitted" class="success-state">
        <NCard size="small" class="success-card">
          <div class="success-icon">✓</div>
          <h2>提交成功</h2>
          <p>{{ task?.type === 'worksheet' ? '练习已提交' : '作品已提交' }}，请等待教师查看和评分。</p>
          <NSpace justify="center">
            <NButton @click="router.back()">返回</NButton>
            <NButton @click="router.push('/student/home')">回到首页</NButton>
            <NButton type="primary" @click="router.push(`/student/tasks/${taskId}/result`)">查看提交状态</NButton>
          </NSpace>
        </NCard>
      </div>

      <div v-else-if="task" class="task-detail">
        <PageHeader :title="task.title" :subtitle="`${typeLabel(task.type)} · 截止 ${task.deadline ? formatDate(task.deadline, 'datetime') : '未设置'} · ${task.submissionCount} 人已提交`" />
        <p v-if="task.description" class="task-desc">{{ task.description }}</p>
        <NAlert v-if="submission?.status === 'returned'" type="warning" :bordered="false" class="return-alert">教师退回：{{ submission.returnReason }}</NAlert>
        <NAlert v-else-if="submission?.status === 'submitted'" type="info" :bordered="false" class="return-alert">当前内容已提交且尚未批改，截止前仍可修改并重新提交。</NAlert>

        <NCard v-if="task.type === 'worksheet' && task.formSchema" title="填写练习" size="small" class="answer-card">
          <WorksheetRenderer :schema="task.formSchema" v-model="worksheetAnswer" />
        </NCard>

        <NCard v-if="task.type === 'artifact'" title="提交作品" size="small" class="answer-card">
          <NAlert type="info" :bordered="false" class="artifact-hint">
            提交方式：{{ artifactSchema.submitMode === 'folder' ? '文件夹' : '文件' }}；文件格式：{{ extensionLabel }}
          </NAlert>

          <div class="upload-actions">
            <NButton :loading="uploadLoading" @click="fileInput?.click()">
              <template #icon><NIcon><CloudUploadOutline /></NIcon></template>
              上传文件
            </NButton>
            <NButton v-if="artifactSchema.submitMode === 'folder'" :loading="uploadLoading" @click="folderInput?.click()">
              <template #icon><NIcon><FolderOutline /></NIcon></template>
              上传文件夹
            </NButton>
            <input ref="fileInput" type="file" :multiple="artifactSchema.submitMode === 'folder'" hidden @change="handleFileInput" />
            <input ref="folderInput" type="file" webkitdirectory multiple hidden @change="handleFileInput" />
          </div>

          <div v-if="uploadedItems.length" class="uploaded-list">
            <div v-for="item in uploadedItems" :key="item.id" class="uploaded-item">
              <span>{{ item.name }}</span>
              <NTag size="tiny" :bordered="false">已上传</NTag>
            </div>
          </div>

          <NInput v-model:value="artifactNote" type="textarea" placeholder="作品说明、链接或补充信息" :autosize="{ minRows: 3, maxRows: 8 }" />
        </NCard>

        <div class="submit-bar">
          <NButton type="primary" size="large" :loading="loading" @click="handleSubmit">提交</NButton>
        </div>
      </div>
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 820px; margin: 0 auto; padding: 24px 0; }
.task-desc { font-size: 14px; color: var(--n-text-color-2); margin: 12px 0 20px; line-height: 1.7; white-space: pre-wrap; }
.answer-card { margin-top: 16px; }
.return-alert { margin: 12px 0; }
.artifact-hint { margin-bottom: 14px; }
.upload-actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.uploaded-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.uploaded-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 10px; border-radius: 6px; background: var(--n-color-embedded); font-size: 13px; }
.submit-bar { margin-top: 24px; text-align: center; }
.success-card { text-align: center; padding: 40px; }
.success-icon { width: 52px; height: 52px; border-radius: 999px; display: grid; place-items: center; margin: 0 auto 16px; background: var(--n-success-color); color: white; font-size: 28px; font-weight: 700; }
.success-card h2 { margin: 0 0 8px; font-size: 20px; }
.success-card p { margin: 0 0 20px; color: var(--n-text-color-2); }
</style>
