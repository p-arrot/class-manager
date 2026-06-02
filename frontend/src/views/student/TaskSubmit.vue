<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NCard, NTag, NInput, NSpin, NSpace, NUpload, NIcon, useMessage } from 'naive-ui'
import { ArrowBackOutline, CloudUploadOutline } from '@vicons/ionicons5'
import { getTask } from '@/api/tasks'
import { submitTask } from '@/api/tasks'
import WorksheetRenderer from '@/components/WorksheetRenderer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'
import type { TaskDetailVO } from '@/types/api'
import http from '@/api/request'

const route = useRoute(); const router = useRouter(); const message = useMessage()
const taskId = Number(route.params.taskId)
const task = ref<TaskDetailVO | null>(null)
const submitted = ref(false)
const loading = ref(false)
const worksheetAnswer = ref<Record<string, any>>({})
const artifactContent = ref('')
const uploadLoading = ref(false)

async function loadTask() {
  loading.value = true
  try { task.value = await getTask(taskId) } catch (e: any) { message.error('加载失败'); router.back() }
  finally { loading.value = false }
}

async function handleSubmit() {
  if (!task.value) return
  loading.value = true
  try {
    const content = task.value.type === 'worksheet'
      ? JSON.stringify(worksheetAnswer.value)
      : artifactContent.value || JSON.stringify({ uploaded: true })
    await submitTask(taskId, { content })
    submitted.value = true
  } catch (e: any) { message.error(e.message || '提交失败') }
  finally { loading.value = false }
}

async function handleFileUpload({ file }: any) {
  if (!file.file) return
  uploadLoading.value = true
  try {
    const fd = new FormData(); fd.append('file', file.file)
    const r: any = await http.post('/files/upload', fd)
    artifactContent.value = JSON.stringify({ resourceId: r.resourceId, fileName: file.file.name })
    message.success('文件上传成功')
  } catch (e: any) { message.error('上传失败') }
  finally { uploadLoading.value = false }
}

function typeLabel(t: string) { return t === 'worksheet' ? '学习单' : '课堂作品' }

onMounted(loadTask)
</script>

<template>
  <div class="page">
    <NButton text @click="router.back()"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回</NButton>
    <NSpin :show="loading">
      <!-- Success state -->
      <div v-if="submitted" class="success-state">
        <NCard size="small" style="text-align:center;padding:40px">
          <div style="font-size:48px;margin-bottom:16px">&#x2705;</div>
          <h2 style="margin:0 0 8px;font-size:20px">提交成功</h2>
          <p style="color:var(--n-text-color-2);margin:0 0 20px">{{ task?.type === 'worksheet' ? '学习单已提交，等待教师评分' : '作品已提交，等待教师评分' }}</p>
          <NSpace justify="center">
            <NButton @click="router.back()">返回</NButton>
            <NButton type="primary" @click="router.push('/student/home')">回到首页</NButton>
          </NSpace>
        </NCard>
      </div>

      <div v-else-if="task" class="task-detail">
        <PageHeader :title="task.title" :subtitle="`${typeLabel(task.type)} · 截止 ${task.deadline ? formatDate(task.deadline, 'datetime') : '未设置'} · ${task.submissionCount} 人已提交`" />
        <p v-if="task.description" class="task-desc">{{ task.description }}</p>

        <!-- Worksheet -->
        <NCard v-if="task.type === 'worksheet' && task.formSchema" title="填写学习单" size="small">
          <WorksheetRenderer :schema="task.formSchema" v-model="worksheetAnswer" />
        </NCard>

        <!-- Artifact -->
        <NCard v-if="task.type === 'artifact'" title="提交作品" size="small">
          <NUpload :show-file-list="false" :custom-request="handleFileUpload" accept="*" :max="1">
            <NButton :loading="uploadLoading"><template #icon><NIcon><CloudUploadOutline /></NIcon></template>上传文件</NButton>
          </NUpload>
          <NInput v-model:value="artifactContent" type="textarea" placeholder="或填写作品说明/链接" :autosize="{ minRows: 3 }" style="margin-top:12px" />
        </NCard>

        <div class="submit-bar">
          <NButton type="primary" size="large" @click="handleSubmit" :loading="loading">提交</NButton>
        </div>
      </div>
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; padding: 24px 0; }
.task-desc { font-size: 14px; color: var(--n-text-color-2); margin: 12px 0 20px; line-height: 1.6; }
.submit-bar { margin-top: 24px; text-align: center; }
</style>
