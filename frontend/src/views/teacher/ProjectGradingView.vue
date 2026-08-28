<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NEmpty, NIcon, NInput, NModal, NSpace, NTag, useMessage } from 'naive-ui'
import { ArrowBackOutline, CloudDownloadOutline, EyeOutline, ReturnUpBackOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import SubmissionRoster from '@/components/grading/SubmissionRoster.vue'
import { getProject, listProjectSubmissions, returnProjectSubmission, scoreProjectSubmission } from '@/api/projects'
import { getDrivePreview, getDriveRaw } from '@/api/drive'
import { CORE_DIMENSIONS } from '@/types/taskSchema'
import { parseProjectDescription, parseProjectSubmissionContent } from '@/types/project'
import { getErrorMessage } from '@/utils/error'
import { submissionStatusLabel, submissionStatusType } from '@/utils/submissionStatus'
import type { ArtifactFile } from '@/types/grading'
import type { ProjectSubmissionVO, ProjectVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const projectId = Number(route.params.projectId)
const project = ref<ProjectVO | null>(null)
const rows = ref<ProjectSubmissionVO[]>([])
const selected = ref<ProjectSubmissionVO | null>(null)
const scores = ref<Record<string, number>>({})
const editing = ref(false)
const showReturn = ref(false)
const returnReason = ref('')
const previewUrl = ref('')
const previewTitle = ref('')

const rubric = computed(() => parseProjectDescription(project.value).rubric.filter(item => item.maxScore > 0))
const content = computed(() => parseProjectSubmissionContent(selected.value?.content))

async function load() {
  try {
    const [projectData, submissions] = await Promise.all([getProject(projectId), listProjectSubmissions(projectId)])
    project.value = projectData
    rows.value = submissions
    const queryStudent = Number(route.query.studentId)
    if (queryStudent) selectRow(submissions.find(row => row.studentId === queryStudent) ?? submissions[0])
  } catch (error) { message.error(getErrorMessage(error, '加载项目批改数据失败')) }
}

function selectRow(row?: ProjectSubmissionVO) {
  if (!row) return
  selected.value = row
  editing.value = row.status === 'submitted'
  scores.value = Object.fromEntries(rubric.value.map(item => [
    item.dimension,
    row.dimensionScores?.find(score => score.dimension === item.dimension)?.earnedScore ?? 0,
  ]))
  router.replace({ query: { ...route.query, studentId: row.studentId } })
}

function dimensionLabel(value: string) { return CORE_DIMENSIONS.find(item => item.key === value)?.label ?? value }

async function saveGrade() {
  if (!selected.value?.submissionId) return
  try {
    await scoreProjectSubmission(selected.value.submissionId, rubric.value.map(item => ({ questionId: 'project', dimension: item.dimension, earnedScore: scores.value[item.dimension] || 0, maxScore: item.maxScore })))
    message.success('项目评分已保存')
    const studentId = selected.value.studentId
    selected.value = null
    await load()
    selectRow(rows.value.find(row => row.studentId === studentId))
    editing.value = false
  } catch (error) { message.error(getErrorMessage(error, '评分失败')) }
}

async function confirmReturn() {
  if (!selected.value?.submissionId || !returnReason.value.trim()) return
  try {
    await returnProjectSubmission(selected.value.submissionId, returnReason.value.trim())
    message.success('已退回学生修改，原成绩已清除')
    showReturn.value = false
    returnReason.value = ''
    selected.value = null
    await load()
  } catch (error) { message.error(getErrorMessage(error, '退回失败')) }
}

async function preview(file: ArtifactFile) {
  try { const data = await getDrivePreview(file.id); previewTitle.value = file.name; previewUrl.value = data.url } catch (error) { message.error(getErrorMessage(error, '预览失败')) }
}

async function download(file: ArtifactFile) {
  try {
    const blob = await getDriveRaw(file.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a'); link.href = url; link.download = file.name; link.click(); URL.revokeObjectURL(url)
  } catch (error) { message.error(getErrorMessage(error, '下载失败')) }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader :title="project?.name || '项目批改'" subtitle="按班级查看个人提交，学生备注中可填写协作成员">
      <template #actions><NButton size="small" @click="router.push('/teacher/projects')"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回项目管理</NButton></template>
    </PageHeader>
    <SubmissionRoster v-if="!selected" :rows="rows" @select="row => selectRow(row as ProjectSubmissionVO)" />
    <section v-else class="detail">
      <div class="detail-head">
        <NButton quaternary @click="selected = null"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回学生名单</NButton>
        <div class="student"><strong>{{ selected.studentName }}</strong><span>{{ selected.className }} · {{ selected.studentNo }}</span></div>
        <NTag :bordered="false" :type="submissionStatusType(selected.status)">{{ submissionStatusLabel(selected.status) }}</NTag>
        <strong v-if="selected.score != null">{{ selected.score }} 分</strong>
      </div>
      <NAlert v-if="selected.status === 'not_submitted'" type="warning" :bordered="false">该学生尚未提交项目作品。</NAlert>
      <NAlert v-else-if="selected.status === 'returned'" type="warning" :bordered="false">已退回修改：{{ selected.returnReason }}</NAlert>
      <template v-else>
        <div class="actions">
          <NButton v-if="selected.status === 'graded'" @click="editing = true">重新批改</NButton>
          <NButton type="warning" @click="showReturn = true"><template #icon><NIcon><ReturnUpBackOutline /></NIcon></template>退回修改</NButton>
        </div>
        <section class="content-section">
          <h3>学生备注 / 组员说明</h3>
          <p>{{ content.note || '未填写备注' }}</p>
        </section>
        <section class="content-section">
          <h3>提交文件</h3>
          <div v-if="content.files.length" class="files">
            <div v-for="file in content.files" :key="file.id" class="file-row"><span>{{ file.name }}</span><NSpace><NButton quaternary @click="preview(file)"><template #icon><NIcon><EyeOutline /></NIcon></template>预览</NButton><NButton quaternary @click="download(file)"><template #icon><NIcon><CloudDownloadOutline /></NIcon></template>下载</NButton></NSpace></div>
          </div>
          <NEmpty v-else description="没有提交文件" />
        </section>
        <section v-if="selected.status === 'graded' && !editing" class="content-section">
          <h3>批改详情</h3>
          <div v-if="selected.dimensionScores?.length" class="score-results">
            <span v-for="item in selected.dimensionScores" :key="`${item.questionId}-${item.dimension}`">
              {{ dimensionLabel(item.dimension) }} <strong>{{ item.earnedScore }}/{{ item.maxScore }}</strong>
            </span>
          </div>
          <NEmpty v-else description="暂无维度评分" />
        </section>
        <section v-if="editing" class="rubric">
          <label v-for="item in rubric" :key="item.dimension"><span>{{ dimensionLabel(item.dimension) }}</span><NInput :value="String(scores[item.dimension] || 0)" @update:value="value => { scores[item.dimension] = Math.max(0, Math.min(Number(value) || 0, item.maxScore)) }"><template #suffix>/ {{ item.maxScore }}</template></NInput></label>
          <NButton type="primary" :disabled="!rubric.length" @click="saveGrade">保存评分</NButton>
        </section>
      </template>
    </section>
    <NModal v-model:show="showReturn" title="退回学生修改" preset="card" class="return-modal"><NInput v-model:value="returnReason" type="textarea" placeholder="请说明需要修改的内容" :maxlength="500" show-count /><template #footer><NSpace justify="end"><NButton @click="showReturn = false">取消</NButton><NButton type="warning" :disabled="!returnReason.trim()" @click="confirmReturn">确认退回</NButton></NSpace></template></NModal>
    <NModal :show="!!previewTitle" :title="previewTitle" preset="card" class="preview-modal" @update:show="value => { if (!value) { previewTitle = ''; previewUrl = '' } }"><iframe v-if="previewUrl" :src="previewUrl" class="preview-frame" /></NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1120px; margin: 0 auto; }
.detail { display: grid; gap: 16px; }
.detail-head { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--n-border-color); padding-bottom: 12px; }
.student { display: grid; gap: 2px; margin-right: auto; }
.student span { color: var(--n-text-color-3); font-size: 12px; }
.actions { display: flex; justify-content: flex-end; gap: 8px; }
.content-section { border-bottom: 1px solid var(--n-border-color); padding-bottom: 16px; }
.content-section h3 { font-size: 14px; margin: 0 0 8px; }
.content-section p { white-space: pre-wrap; margin: 0; line-height: 1.6; }
.score-results { display: flex; flex-wrap: wrap; gap: 10px 24px; color: var(--n-text-color-2); }
.files { display: grid; gap: 8px; }
.file-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 12px; background: var(--n-color-embedded); border-radius: 6px; }
.rubric { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)) auto; gap: 10px; align-items: end; }
.rubric label { display: grid; gap: 5px; color: var(--n-text-color-2); font-size: 13px; }
.return-modal { width: min(480px, calc(100vw - 32px)); }
.preview-modal { width: min(1000px, calc(100vw - 32px)); }
.preview-frame { width: 100%; height: 70vh; border: 0; }
@media (max-width: 640px) {
  .detail-head { align-items: flex-start; flex-wrap: wrap; }
  .student { order: 3; width: 100%; }
  .file-row { align-items: flex-start; flex-direction: column; }
  .rubric { grid-template-columns: 1fr; }
}
</style>
