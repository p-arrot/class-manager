<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NCard, NEmpty, NIcon, NProgress, NSpin, NTag, useMessage } from 'naive-ui'
import { ArrowBackOutline, CheckmarkCircleOutline, TimeOutline, WarningOutline } from '@vicons/ionicons5'
import { getMyTaskResult } from '@/api/tasks'
import MarkdownView from '@/components/MarkdownView.vue'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { TaskResultQuestionResultVO, TaskResultVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const taskId = Number(route.params.taskId)

const loading = ref(false)
const result = ref<TaskResultVO | null>(null)
const errorMessage = ref('')

const dimLabels: Record<string, string> = {
  AWARENESS: '信息意识',
  COMPUTING: '计算思维',
  DIGITAL_LEARNING: '数字化学习与创新',
  RESPONSIBILITY: '信息社会责任',
}

const statusMeta = computed(() => {
  const status = result.value?.status
  if (status === 'graded') return { label: '已批改', type: 'success' as const, icon: CheckmarkCircleOutline }
  if (status === 'special') return { label: '特殊处理', type: 'warning' as const, icon: WarningOutline }
  if (status === 'submitted') return { label: '待教师批改', type: 'warning' as const, icon: TimeOutline }
  return { label: '未提交', type: 'default' as const, icon: TimeOutline }
})

const resultByQuestion = computed<Record<string, TaskResultQuestionResultVO>>(() => {
  const entries = result.value?.questionResults.map(item => [item.questionId, item] as const) ?? []
  return Object.fromEntries(entries)
})

const totalEarned = computed(() => result.value?.questionResults.reduce((sum, item) => sum + (item.earnedScore || 0), 0) ?? 0)
const totalMax = computed(() => result.value?.questionResults.reduce((sum, item) => sum + (item.maxScore || 0), 0) ?? 0)
const scoreRate = computed(() => totalMax.value > 0 ? Math.round(totalEarned.value * 1000 / totalMax.value) / 10 : 0)

async function loadResult() {
  loading.value = true
  errorMessage.value = ''
  try {
    result.value = await getMyTaskResult(taskId)
  } catch (error) {
    const fallback = '只能查看自己的提交批改详情'
    errorMessage.value = getErrorMessage(error, fallback)
    message.error(errorMessage.value)
    result.value = null
  } finally {
    loading.value = false
  }
}

function answerText(value: unknown) {
  if (value == null || value === '') return '未作答'
  if (Array.isArray(value)) return value.length ? value.map(String).join('、') : '未作答'
  if (typeof value === 'boolean') return value ? '正确' : '错误'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function scoreText(item?: TaskResultQuestionResultVO) {
  if (!item || item.maxScore <= 0) return '未评分'
  return `${item.earnedScore}/${item.maxScore}`
}

function resultTag(item?: TaskResultQuestionResultVO) {
  if (!item) return { label: '待评分', type: 'warning' as const }
  if (item.correct === true) return { label: '正确', type: 'success' as const }
  if (item.correct === false) return { label: '需订正', type: 'error' as const }
  return item.maxScore > 0 ? { label: '已评分', type: 'success' as const } : { label: '待评分', type: 'warning' as const }
}

function goCourse() {
  const courseId = result.value?.task.courseId
  if (courseId) router.push(`/student/courses/${courseId}`)
  else router.push('/student/home')
}

onMounted(loadResult)
</script>

<template>
  <div class="page">
    <NButton text class="back-button" @click="router.back()">
      <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
      返回
    </NButton>

    <NSpin :show="loading">
      <div v-if="result" class="content">
        <PageHeader :title="result.task.title" :subtitle="[result.task.courseName, result.task.lessonName].filter(Boolean).join(' / ')" />

        <section class="summary-band">
          <div class="summary-main">
            <NTag :type="statusMeta.type" :bordered="false" size="small">
              <template #icon><NIcon :component="statusMeta.icon" /></template>
              {{ statusMeta.label }}
            </NTag>
            <div>
              <div class="summary-title">{{ totalMax ? `${totalEarned}/${totalMax} 分` : '暂无分数' }}</div>
              <div class="summary-sub">
                提交：{{ result.submission?.submittedAt ? formatDate(result.submission.submittedAt, 'datetime') : '暂无' }}
                <span v-if="result.submission?.gradedAt"> / 批改：{{ formatDate(result.submission.gradedAt, 'datetime') }}</span>
              </div>
            </div>
          </div>
          <NProgress v-if="totalMax" type="line" :percentage="scoreRate" :show-indicator="false" color="#16a34a" rail-color="#dcfce7" class="summary-progress" />
        </section>

        <NAlert v-if="result.status === 'not_submitted'" type="warning" :bordered="false" class="state-alert">
          你还没有提交这项任务。完成提交后，老师批改完成时这里会显示批改详情。
        </NAlert>
        <NAlert v-else-if="result.status === 'submitted'" type="info" :bordered="false" class="state-alert">
          你的答案已提交，正在等待教师批改。当前仅显示提交内容，不展示空分数。
        </NAlert>
        <NAlert v-else-if="result.status === 'special'" type="warning" :bordered="false" class="state-alert">
          这次任务被标记为特殊处理，不计入评价统计。
          <span v-if="result.submission?.teacherComment"> {{ result.submission.teacherComment }}</span>
        </NAlert>
        <NAlert v-else-if="result.submission?.teacherComment" type="success" :bordered="false" class="state-alert">
          {{ result.submission.teacherComment }}
        </NAlert>

        <div v-if="result.dimensionSummary.length" class="dimension-grid">
          <div v-for="dim in result.dimensionSummary" :key="dim.dimension" class="dimension-item">
            <div class="dimension-name">{{ dimLabels[dim.dimension] || dim.dimension }}</div>
            <div class="dimension-score">{{ dim.earnedScore }}/{{ dim.maxScore }}</div>
            <NProgress type="line" :percentage="Math.round((dim.rate || 0) * 1000) / 10" :show-indicator="false" />
            <NTag size="tiny" :bordered="false">{{ dim.grade }}</NTag>
          </div>
        </div>

        <section class="questions">
          <h3 class="section-title">逐题明细</h3>
          <NCard v-for="question in result.questions" :key="question.id" size="small" class="question-card">
            <div class="question-head">
              <div class="question-title">第 {{ question.index }} 题</div>
              <div class="question-tags">
                <NTag size="tiny" :bordered="false">{{ question.autoGrade ? '自动题' : '人工题' }}</NTag>
                <NTag size="tiny" :type="resultTag(resultByQuestion[question.id]).type" :bordered="false">
                  {{ resultTag(resultByQuestion[question.id]).label }}
                </NTag>
                <NTag size="tiny" :bordered="false">{{ scoreText(resultByQuestion[question.id]) }}</NTag>
              </div>
            </div>

            <MarkdownView :content="question.stem" />

            <div class="answer-block">
              <div class="answer-label">我的答案</div>
              <div class="answer-value">{{ answerText(result.answers[question.id]) }}</div>
            </div>

            <div v-if="question.referenceAnswerVisible" class="answer-block reference">
              <div class="answer-label">参考答案</div>
              <div class="answer-value">{{ answerText(question.referenceAnswer) }}</div>
            </div>

            <div v-if="resultByQuestion[question.id]?.dimensionScores.length" class="score-list">
              <span v-for="score in resultByQuestion[question.id].dimensionScores" :key="score.dimension" class="score-chip">
                {{ dimLabels[score.dimension] || score.dimension }} {{ score.earnedScore }}/{{ score.maxScore }}
              </span>
            </div>

            <NAlert v-if="resultByQuestion[question.id]?.comment" type="info" :bordered="false" class="question-comment">
              {{ resultByQuestion[question.id].comment }}
            </NAlert>
          </NCard>
        </section>

        <div class="actions">
          <NButton @click="goCourse">返回课程</NButton>
          <NButton type="primary" @click="router.push('/student/evaluation')">查看学习评价</NButton>
        </div>
      </div>

      <NEmpty v-else-if="!loading" :description="errorMessage || '暂无批改详情'">
        <template #extra>
          <NButton @click="router.push('/student/home')">回到首页</NButton>
        </template>
      </NEmpty>
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 980px; margin: 0 auto; padding: 24px 0 40px; }
.back-button { margin-bottom: 12px; min-height: 44px; }
.content { animation: fadein 180ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.summary-band {
  display: flex; align-items: center; justify-content: space-between; gap: 18px;
  padding: 16px 18px; border: 1px solid var(--n-border-color); border-radius: 8px; margin: 18px 0 14px;
  background: transparent;
}
.summary-main { display: flex; align-items: center; gap: 14px; min-width: 0; }
.summary-title { font-size: 24px; font-weight: 700; line-height: 1.2; }
.summary-sub { margin-top: 4px; font-size: 13px; color: var(--n-text-color-2); }
.summary-progress { width: 220px; flex: 0 0 220px; }
.state-alert { margin-bottom: 16px; }
.dimension-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 20px; }
.dimension-item { border: 1px solid var(--n-border-color); border-radius: 8px; padding: 12px; display: grid; gap: 8px; background: transparent; }
.dimension-name { font-size: 13px; color: var(--n-text-color-2); }
.dimension-score { font-size: 18px; font-weight: 700; }
.questions { display: grid; gap: 12px; }
.section-title { font-size: 16px; font-weight: 700; margin: 4px 0; }
.question-card { border-radius: 8px; }
.question-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.question-title { font-size: 15px; font-weight: 700; }
.question-tags { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
.answer-block { margin-top: 12px; padding: 10px 12px; border-radius: 6px; background: var(--n-color-embedded); }
.answer-block.reference { background: rgba(22, 163, 74, 0.08); }
.answer-label { font-size: 12px; color: var(--n-text-color-3); margin-bottom: 4px; }
.answer-value { font-size: 14px; line-height: 1.6; overflow-wrap: anywhere; white-space: pre-wrap; }
.score-list { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.score-chip { font-size: 12px; padding: 4px 8px; border-radius: 999px; background: var(--n-color-embedded); color: var(--n-text-color-2); }
.question-comment { margin-top: 12px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
@media (max-width: 720px) {
  .page { padding: 16px 0 28px; }
  .summary-band { align-items: flex-start; flex-direction: column; }
  .summary-progress { width: 100%; flex: none; }
  .dimension-grid { grid-template-columns: 1fr 1fr; }
  .question-head { align-items: flex-start; flex-direction: column; }
  .question-tags { justify-content: flex-start; }
  .actions { flex-direction: column; }
}
</style>
