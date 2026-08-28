<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NCheckbox, NEmpty, NIcon, NInput, NModal, NSpace, NSpin, NTag, useMessage } from 'naive-ui'
import { ArrowBackOutline, ReturnUpBackOutline } from '@vicons/ionicons5'
import PageHeader from '@/components/PageHeader.vue'
import SubmissionRoster from '@/components/grading/SubmissionRoster.vue'
import { getExam, gradeExamSubmission, listExamSubmissions, returnExamSubmission } from '@/api/exams'
import { CORE_DIMENSIONS, normalizeDimensionScores, parseTaskSchema, questionStem, questionTotalScore } from '@/types/taskSchema'
import { getErrorMessage } from '@/utils/error'
import { submissionStatusLabel, submissionStatusType } from '@/utils/submissionStatus'
import type { ExamSubmissionVO, ExamVO, QuestionDimensionScoreDTO } from '@/types/api'
import type { TaskQuestion, WorksheetAnswerMap } from '@/types/taskSchema'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const examId = Number(route.params.examId)
const exam = ref<ExamVO | null>(null)
const rows = ref<ExamSubmissionVO[]>([])
const selected = ref<ExamSubmissionVO | null>(null)
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const absent = ref(false)
const scores = ref<QuestionDimensionScoreDTO[]>([])
const showReturn = ref(false)
const returnReason = ref('')

const questions = computed(() => parseTaskSchema(exam.value?.paperContent).questions ?? [])
const answers = computed<WorksheetAnswerMap>(() => {
  try { return JSON.parse(selected.value?.answers || '{}') as WorksheetAnswerMap } catch { return {} }
})
const total = computed(() => scores.value.reduce((sum, item) => sum + Number(item.earnedScore || 0), 0))
async function load() {
  loading.value = true
  try {
    const [examData, submissions] = await Promise.all([getExam(examId), listExamSubmissions(examId)])
    exam.value = examData
    rows.value = submissions
    const queryStudent = Number(route.query.studentId)
    if (queryStudent) selectRow(submissions.find(row => row.studentId === queryStudent) ?? submissions[0])
  } catch (error) {
    message.error(getErrorMessage(error, '加载考试批改数据失败'))
  } finally { loading.value = false }
}

function selectRow(row?: ExamSubmissionVO) {
  if (!row) return
  selected.value = row
  editing.value = row.status === 'submitted'
  absent.value = row.status === 'absent'
  scores.value = questions.value.flatMap(question => normalizeDimensionScores(question.dimensionScores, question.score)
    .filter(item => item.maxScore > 0)
    .map(item => ({ questionId: question.id, dimension: item.dimension, earnedScore: 0, maxScore: item.maxScore, autoGraded: false })))
  router.replace({ query: { ...route.query, studentId: row.studentId } })
}

function answerText(question: TaskQuestion) {
  const value = answers.value[question.id]
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'boolean') return value ? '正确' : '错误'
  return value == null || value === '' ? '未作答' : String(value)
}

function updateScore(questionId: string, dimension: string, value: string) {
  const target = scores.value.find(item => item.questionId === questionId && item.dimension === dimension)
  if (target) target.earnedScore = Math.max(0, Math.min(Number(value) || 0, target.maxScore))
}

function dimensionLabel(value: string) {
  return CORE_DIMENSIONS.find(item => item.key === value)?.label ?? value
}

async function saveGrade() {
  if (!selected.value?.submissionId) return
  saving.value = true
  try {
    await gradeExamSubmission(selected.value.submissionId, { score: absent.value ? 0 : Math.round(total.value), absent: absent.value, dimensionScores: absent.value ? [] : scores.value })
    message.success(absent.value ? '已标记缺考' : '考试批改已保存')
    await load()
    selectRow(rows.value.find(row => row.studentId === selected.value?.studentId))
    editing.value = false
  } catch (error) { message.error(getErrorMessage(error, '保存批改失败')) } finally { saving.value = false }
}

async function confirmReturn() {
  if (!selected.value?.submissionId || !returnReason.value.trim()) return
  try {
    await returnExamSubmission(selected.value.submissionId, returnReason.value.trim())
    message.success('已退回学生修改，原成绩已清除')
    showReturn.value = false
    returnReason.value = ''
    selected.value = null
    await load()
  } catch (error) { message.error(getErrorMessage(error, '退回失败')) }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader :title="exam?.name || '考试批改'" subtitle="先从班级名单选择学生，再查看和批改答卷">
      <template #actions><NButton size="small" @click="router.push('/teacher/exams')"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回考试管理</NButton></template>
    </PageHeader>
    <NSpin :show="loading">
      <SubmissionRoster v-if="!selected" :rows="rows" :loading="loading" @select="row => selectRow(row as ExamSubmissionVO)" />
      <section v-else class="detail">
        <div class="detail-head">
          <NButton quaternary @click="selected = null"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回学生名单</NButton>
          <div class="student"><strong>{{ selected.studentName }}</strong><span>{{ selected.className }} · {{ selected.studentNo }}</span></div>
          <NTag :bordered="false" :type="submissionStatusType(selected.status)">{{ submissionStatusLabel(selected.status) }}</NTag>
          <strong v-if="selected.score != null" class="score">{{ selected.score }} 分</strong>
        </div>
        <NAlert v-if="selected.status === 'not_submitted'" type="warning" :bordered="false">该学生尚未提交考试，暂时不能批改。</NAlert>
        <NAlert v-else-if="selected.status === 'in_progress'" type="info" :bordered="false">该学生正在答题，草稿不会进入批改。</NAlert>
        <NAlert v-else-if="selected.status === 'returned'" type="warning" :bordered="false">已退回修改：{{ selected.returnReason }}</NAlert>
        <template v-else>
          <div class="actions">
            <NButton v-if="selected.status === 'graded' || selected.status === 'absent'" @click="editing = true">重新批改</NButton>
            <NButton type="warning" @click="showReturn = true"><template #icon><NIcon><ReturnUpBackOutline /></NIcon></template>退回修改</NButton>
          </div>
          <NCheckbox v-if="editing" v-model:checked="absent">标记为缺考，本次考试记 0 分</NCheckbox>
          <div v-if="questions.length" class="questions" :class="{ readonly: !editing }">
            <article v-for="(question, index) in questions" :key="question.id" class="question">
              <div class="question-head"><NTag size="small" :bordered="false">第 {{ index + 1 }} 题</NTag><strong>{{ questionTotalScore(question) }} 分</strong></div>
              <p>{{ questionStem(question) }}</p>
              <div class="answer"><span>学生答案</span><strong>{{ answerText(question) }}</strong></div>
              <div v-if="editing && !absent" class="score-grid">
                <label v-for="item in scores.filter(score => score.questionId === question.id)" :key="item.dimension">
                  <span>{{ dimensionLabel(item.dimension) }}</span>
                  <NInput :value="String(item.earnedScore)" @update:value="value => updateScore(question.id, item.dimension, value)"><template #suffix>/ {{ item.maxScore }}</template></NInput>
                </label>
              </div>
            </article>
          </div>
          <NEmpty v-else description="该试卷没有可展示的结构化题目" />
          <div v-if="editing" class="save-bar"><strong>{{ absent ? 0 : Math.round(total) }} 分</strong><NButton type="primary" :loading="saving" @click="saveGrade">保存批改</NButton></div>
        </template>
      </section>
    </NSpin>
    <NModal v-model:show="showReturn" title="退回学生修改" preset="card" class="return-modal">
      <NInput v-model:value="returnReason" type="textarea" placeholder="请说明需要修改的内容" :maxlength="500" show-count />
      <template #footer><NSpace justify="end"><NButton @click="showReturn = false">取消</NButton><NButton type="warning" :disabled="!returnReason.trim()" @click="confirmReturn">确认退回</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1120px; margin: 0 auto; }
.detail { display: grid; gap: 16px; }
.detail-head { display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--n-border-color); padding-bottom: 12px; }
.student { display: grid; gap: 2px; margin-right: auto; }
.student span { color: var(--n-text-color-3); font-size: 12px; }
.score { font-size: 20px; }
.actions { display: flex; justify-content: flex-end; gap: 8px; }
.questions { display: grid; gap: 12px; }
.questions.readonly { opacity: .92; }
.question { border: 1px solid var(--n-border-color); border-radius: 8px; padding: 16px; }
.question-head { display: flex; justify-content: space-between; align-items: center; }
.question p { white-space: pre-wrap; line-height: 1.6; }
.answer { display: grid; grid-template-columns: 88px 1fr; gap: 8px; padding: 10px 12px; background: var(--n-color-embedded); border-radius: 6px; }
.answer span { color: var(--n-text-color-3); }
.score-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 12px; }
.score-grid label { display: grid; grid-template-columns: minmax(100px, 1fr) 130px; gap: 8px; align-items: center; }
.save-bar { position: sticky; bottom: 0; display: flex; justify-content: flex-end; align-items: center; gap: 16px; padding: 12px 0; background: var(--n-color); border-top: 1px solid var(--n-border-color); }
.return-modal { width: min(480px, calc(100vw - 32px)); }
@media (max-width: 640px) {
  .detail-head { align-items: flex-start; flex-wrap: wrap; }
  .student { order: 3; width: 100%; }
  .score-grid, .score-grid label { grid-template-columns: 1fr; }
  .answer { grid-template-columns: 1fr; }
}
</style>
