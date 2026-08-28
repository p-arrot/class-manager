<script setup lang="ts">
import { computed } from 'vue'
import { NAlert, NCheckbox, NInput, NInputNumber, NTag } from 'naive-ui'
import MarkdownView from '@/components/MarkdownView.vue'
import { CORE_DIMENSIONS, normalizeDimensionScores, questionStem, questionTotalScore } from '@/types/taskSchema'
import type { ParsedSubmissionContent } from '@/types/grading'
import type { TaskQuestion } from '@/types/taskSchema'

const props = defineProps<{
  questions: TaskQuestion[]
  content: ParsedSubmissionContent
  submissionId: number
  fallbackContent?: string | null
  getScore: (submissionId: number, questionId: string, dimension: string) => number | null
  getComment?: (submissionId: number, questionId: string) => string
  getReferenceVisible?: (submissionId: number, questionId: string) => boolean
  validationErrors?: Record<string, string>
}>()

const emit = defineEmits<{
  scoreChange: [submissionId: number, questionId: string, dimension: string, value: number | null]
  feedbackChange: [submissionId: number, questionId: string, value: string]
  referenceVisibleChange: [submissionId: number, questionId: string, value: boolean]
}>()

const autoSummary = computed(() => {
  let total = 0
  let earned = 0
  let count = 0
  let correct = 0
  for (const question of props.questions) {
    if (!question.autoGrade) continue
    const score = questionTotalScore(question)
    total += score
    count += 1
    if (isCorrect(question)) {
      earned += score
      correct += 1
    }
  }
  return count ? { earned, total, correct, count } : null
})

function answerText(question: TaskQuestion) {
  const answer = props.content[question.id]
  if (answer == null || answer === '') return '未作答'
  if (question.type === 'true_false') return answer === true || answer === 'true' ? '正确' : '错误'
  if (Array.isArray(answer)) return answer.join('、')
  return String(answer)
}

function expectedText(question: TaskQuestion) {
  const expected = question.answer
  if (expected == null || expected === '') return ''
  if (question.type === 'true_false') return expected === true || expected === 'true' ? '正确' : '错误'
  if (Array.isArray(expected)) return expected.join('、')
  return String(expected)
}

function isCorrect(question: TaskQuestion) {
  const expected = question.answer
  const actual = props.content[question.id]
  if (Array.isArray(expected)) {
    if (!Array.isArray(actual)) return false
    return [...expected].map(String).sort().join('|') === [...actual].map(String).sort().join('|')
  }
  return String(expected) === String(actual)
}

function dimensionLabel(key: string) {
  return CORE_DIMENSIONS.find(item => item.key === key)?.label ?? key
}

function commentValue(questionId: string) {
  return props.getComment?.(props.submissionId, questionId) ?? ''
}

function referenceVisible(questionId: string) {
  return props.getReferenceVisible?.(props.submissionId, questionId) ?? false
}
</script>

<template>
  <div class="worksheet-result">
    <div v-if="autoSummary" class="auto-score">
      <span class="auto-label">自动题预评分</span>
      <strong>{{ autoSummary.earned }}/{{ autoSummary.total }} 分</strong>
      <span>{{ autoSummary.correct }}/{{ autoSummary.count }} 题正确</span>
      <span class="auto-note">保存批改后写入评价数据</span>
    </div>
    <div v-for="(question, index) in questions" :key="question.id" class="answer-row" :data-question-id="question.id">
      <div class="question-header">
        <div>
          <div class="question-title">第 {{ index + 1 }} 题</div>
          <div class="question-type">{{ question.autoGrade ? '自动题预评分' : '人工评分题' }}</div>
        </div>
        <NTag v-if="question.autoGrade" size="small" :type="isCorrect(question) ? 'success' : 'error'" :bordered="false">
          {{ isCorrect(question) ? '正确' : '错误' }}
        </NTag>
        <NTag v-else size="small" :bordered="false">逐题评分</NTag>
      </div>
      <MarkdownView class="question-md" :content="questionStem(question) || '未填写题干'" />
      <img v-if="question.imageUrl" :src="question.imageUrl" class="question-image" alt="题目配图" />
      <div class="answer-line">
        <span class="answer-label">学生作答</span>
        <span>{{ answerText(question) }}</span>
      </div>
      <div v-if="question.autoGrade" class="expected-line">参考答案：{{ expectedText(question) || '未设置' }}</div>
      <NAlert v-if="validationErrors?.[question.id]" type="error" :bordered="false" class="question-error">
        {{ validationErrors[question.id] }}
      </NAlert>
      <div class="question-score-grid">
        <label
          v-for="dim in normalizeDimensionScores(question.dimensionScores).filter(item => item.maxScore > 0)"
          :key="dim.dimension"
          class="question-score-cell"
        >
          <span>{{ dimensionLabel(dim.dimension) }}</span>
          <NInputNumber
            :value="getScore(submissionId, question.id, dim.dimension)"
            :min="0"
            :max="dim.maxScore"
            :precision="1"
            size="small"
            @update:value="value => emit('scoreChange', submissionId, question.id, dim.dimension, value)"
          />
          <span class="score-max">/ {{ dim.maxScore }}</span>
        </label>
      </div>
      <div class="question-feedback">
        <label class="feedback-label" :for="`feedback-${submissionId}-${question.id}`">逐题评语</label>
        <NInput
          :id="`feedback-${submissionId}-${question.id}`"
          :value="commentValue(question.id)"
          type="textarea"
          placeholder="给学生的本题反馈，可写改进建议或亮点"
          :autosize="{ minRows: 2, maxRows: 4 }"
          @update:value="value => emit('feedbackChange', submissionId, question.id, value)"
        />
        <NCheckbox
          v-if="question.autoGrade"
          :checked="referenceVisible(question.id)"
          @update:checked="value => emit('referenceVisibleChange', submissionId, question.id, Boolean(value))"
        >
          向学生展示参考答案
        </NCheckbox>
      </div>
    </div>
    <div v-if="!questions.length" class="content-text">{{ fallbackContent || '无内容' }}</div>
  </div>
</template>

<style scoped>
.worksheet-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.auto-score {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-color-embedded);
  color: var(--n-text-color-2);
  font-size: 13px;
}
.auto-label {
  color: var(--n-text-color-3);
}
.auto-score strong {
  color: var(--n-primary-color);
  font-size: 15px;
}
.auto-note {
  color: var(--n-text-color-3);
}
.answer-row {
  padding: 14px 16px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-card-color);
}
.question-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.question-title {
  color: var(--n-text-color);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
}
.question-type {
  color: var(--n-text-color-3);
  font-size: 12px;
}
.question-md {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-color-embedded);
}
.question-image {
  display: block;
  max-width: 360px;
  width: 100%;
  max-height: 220px;
  object-fit: contain;
  margin-top: 10px;
  border-radius: 6px;
  border: 1px solid var(--n-border-color);
}
.answer-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  color: var(--n-text-color);
  font-size: 14px;
}
.answer-label {
  color: var(--n-text-color-3);
  font-size: 12px;
}
.expected-line {
  margin-top: 6px;
  color: var(--n-text-color-3);
  font-size: 12px;
}
.question-error {
  margin-top: 10px;
}
.question-score-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--n-border-color);
}
.question-feedback {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--n-border-color);
}
.feedback-label {
  color: var(--n-text-color-2);
  font-size: 13px;
  font-weight: 600;
}
.question-score-cell {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 110px 42px;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  background: var(--n-color-embedded);
  color: var(--n-text-color-2);
  font-size: 13px;
}
.score-max {
  color: var(--n-text-color-3);
}
.content-text {
  padding: 12px 16px;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  background: var(--n-color-embedded);
  font-size: 14px;
  min-height: 60px;
  white-space: pre-wrap;
}
@media (max-width: 720px) {
  .question-score-grid { grid-template-columns: 1fr; }
  .question-score-cell { grid-template-columns: 1fr; }
}
</style>
