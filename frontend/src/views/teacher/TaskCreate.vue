<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  useMessage,
} from 'naive-ui'
import type { UploadCustomRequestOptions } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { createTask } from '@/api/tasks'
import { getLesson } from '@/api/lessons'
import { getSemester } from '@/api/semesters'
import { getStreamUrl } from '@/api/files'
import PageHeader from '@/components/PageHeader.vue'
import TaskQuestionList from '@/components/task/TaskQuestionList.vue'
import { toLocalDateTime } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import {
  emptyQuestion,
  normalizeDimensionScores,
  questionTotalScore,
  type ArtifactSchema,
  type TaskQuestion,
  type TaskFormSchema,
} from '@/types/taskSchema'
import type { FileUploadVO, TaskCreateDTO } from '@/types/api'
import http from '@/api/request'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const lessonId = Number(route.params.lessonId)
const lessonName = ref('')
const courseId = ref<number | null>(null)

type TaskType = TaskCreateDTO['type']

const form = ref<{
  title: string
  type: TaskType
  description: string
  deadline: number | null
}>({ title: '', type: 'worksheet', description: '', deadline: null })

const artifact = ref<ArtifactSchema>({ submitMode: 'file', allowedExtensions: [] })
const extensionText = ref('')
const questions = ref<TaskQuestion[]>([emptyQuestion('single')])

const schemaPreview = computed<TaskFormSchema>(() => {
  if (form.value.type === 'artifact') {
    return {
      version: 3,
      artifact: {
        submitMode: artifact.value.submitMode,
        allowedExtensions: extensionText.value
          .split(',')
          .map(item => item.trim().replace(/^\./, '').toLowerCase())
          .filter(Boolean),
      },
    }
  }
  return {
    version: 3,
    questions: questions.value.map(question => ({
      ...question,
      stem: question.stem.trim(),
      dimensionScores: normalizeDimensionScores(question.dimensionScores),
      title: undefined,
      markdown: undefined,
      score: undefined,
    })),
  }
})

async function handleImageUpload(question: TaskQuestion, { file }: UploadCustomRequestOptions) {
  const rawFile = file.file
  if (!rawFile) return
  if (!courseId.value) {
    message.error('无法确定课程，暂不能上传图片')
    return
  }
  try {
    const fd = new FormData()
    fd.append('file', rawFile)
    fd.append('courseId', String(courseId.value))
    const upload = await http.post<FileUploadVO>('/files/upload', fd)
    const stream = await getStreamUrl(upload.resourceId)
    question.imageUrl = stream.url
    message.success('题目图片已插入')
  } catch (error) {
    message.error(getErrorMessage(error, '图片上传失败'))
  }
}

function validateWorksheet() {
  if (!questions.value.length) {
    message.warning('请至少添加一道题目')
    return false
  }
  for (const [index, question] of questions.value.entries()) {
    if (!question.stem.trim()) {
      message.warning(`第 ${index + 1} 题缺少题干`)
      return false
    }
    if (questionTotalScore(question) <= 0) {
      message.warning(`第 ${index + 1} 题至少需要设置一个核心素养分值`)
      return false
    }
    if ((question.type === 'single' || question.type === 'multiple') && !(question.options ?? []).filter(Boolean).length) {
      message.warning(`第 ${index + 1} 题至少需要一个选项`)
      return false
    }
    if ((question.type === 'single' || question.type === 'multiple' || question.type === 'true_false') && question.autoGrade && (question.answer === '' || question.answer == null || (Array.isArray(question.answer) && !question.answer.length))) {
      message.warning(`第 ${index + 1} 题开启自动批改后需要设置正确答案`)
      return false
    }
  }
  return true
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    message.warning('请输入任务标题')
    return
  }
  if (form.value.type === 'worksheet' && !validateWorksheet()) return

  try {
    const payload: TaskCreateDTO = {
      title: form.value.title.trim(),
      type: form.value.type,
      description: form.value.description || undefined,
      deadline: toLocalDateTime(form.value.deadline),
      formSchema: JSON.stringify(schemaPreview.value),
    }
    await createTask(lessonId, payload)
    message.success('任务创建成功')
    router.back()
  } catch (error) {
    message.error(getErrorMessage(error, '创建失败'))
  }
}

onMounted(async () => {
  try {
    const lesson = await getLesson(lessonId)
    lessonName.value = lesson.name
    const semester = await getSemester(lesson.semesterId)
    courseId.value = semester.courseId
  } catch (error) {
    message.error(getErrorMessage(error, '加载课时失败'))
  }
})
</script>

<template>
  <div class="page">
    <div class="back-bar">
      <NButton text @click="router.back()">
        <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
        返回
      </NButton>
    </div>

    <PageHeader title="创建任务" :subtitle="lessonName || '课时任务'" />

    <div class="layout">
      <section class="main">
        <NCard size="small" class="panel">
          <NForm label-placement="top">
            <NFormItem label="任务标题" required>
              <NInput v-model:value="form.title" placeholder="如：Python 条件语句练习" size="large" />
            </NFormItem>
            <NFormItem label="任务类型" required>
              <NSelect
                v-model:value="form.type"
                :options="[
                  { label: '学习单 / 练习题', value: 'worksheet' },
                  { label: '课堂作品 / 文件提交', value: 'artifact' },
                ]"
                size="large"
              />
            </NFormItem>
            <NFormItem label="任务说明">
              <NInput v-model:value="form.description" type="textarea" placeholder="写给学生看的说明，可简述目标、要求和评分方式" :autosize="{ minRows: 3, maxRows: 8 }" />
            </NFormItem>
            <NFormItem label="截止时间">
              <NDatePicker v-model:value="form.deadline" type="datetime" clearable class="date-picker" />
            </NFormItem>
          </NForm>
        </NCard>

        <TaskQuestionList
          v-if="form.type === 'worksheet'"
          v-model="questions"
          @upload-image="handleImageUpload"
        />

        <NCard v-else size="small" class="panel">
          <template #header>作品提交设置</template>
          <NForm label-placement="top">
            <NFormItem label="提交方式">
              <NRadioGroup v-model:value="artifact.submitMode">
                <NSpace>
                  <NRadio value="file">提交文件</NRadio>
                  <NRadio value="folder">提交文件夹</NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
            <NFormItem v-if="artifact.submitMode === 'file'" label="限定文件后缀">
              <NInput v-model:value="extensionText" placeholder="例如：py,ipynb,png。留空表示不限制" />
            </NFormItem>
          </NForm>
        </NCard>
      </section>

      <aside class="side">
        <NCard size="small" class="panel">
          <template #header>发布检查</template>
          <div class="check-list">
            <span :class="{ ok: !!form.title.trim() }">任务标题</span>
            <span :class="{ ok: form.type === 'artifact' || questions.length > 0 }">题目或提交设置</span>
            <span :class="{ ok: !!form.deadline }">截止时间</span>
          </div>
          <NButton type="primary" size="large" block @click="handleSubmit">创建任务</NButton>
        </NCard>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 1180px; margin: 0 auto; padding: 24px 0 40px; }
.back-bar { margin-bottom: 8px; }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 18px; align-items: start; }
.main { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.side { position: sticky; top: 72px; min-width: 0; }
.panel { border-radius: 8px; }
.date-picker { width: 100%; }
.check-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; font-size: 13px; color: var(--n-text-color-3); }
.check-list span::before { content: '○'; margin-right: 6px; }
.check-list span.ok { color: var(--n-success-color); }
.check-list span.ok::before { content: '✓'; }
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .side { position: static; }
}
@media (max-width: 640px) {
  .page { padding-top: 8px; }
}
</style>
