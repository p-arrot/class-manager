<script setup lang="ts">
import { computed } from 'vue'
import { NInputNumber, NTag } from 'naive-ui'
import MarkdownView from '@/components/MarkdownView.vue'
import { CORE_DIMENSIONS, normalizeDimensionScores, questionStem, questionTotalScore } from '@/types/taskSchema'
import type { ParsedSubmissionContent } from '@/types/grading'
import type { TaskQuestion } from '@/types/taskSchema'

const props = defineProps<{
  questions: TaskQuestion[]
  content: ParsedSubmissionContent
  submissionId: number
  fallbackContent?: string | null
  getScore: (submissionId: number, questionId: string, dimension: string) => number
}>()

const emit = defineEmits<{
  scoreChange: [submissionId: number, questionId: string, dimension: string, value: number | null]
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
</script>

<template>
  <div class="worksheet-result">
    <div v-if="autoSummary" class="auto-score">
      自动批改：{{ autoSummary.earned }}/{{ autoSummary.total }} 分 · {{ autoSummary.correct }}/{{ autoSummary.count }} 题正确
    </div>
    <div v-for="(question, index) in questions" :key="question.id" class="answer-row">
      <div class="question-title">第 {{ index + 1 }} 题</div>
      <MarkdownView class="question-md" :content="questionStem(question) || '未填写题干'" />
      <img v-if="question.imageUrl" :src="question.imageUrl" class="question-image" alt="题目配图" />
      <div class="answer-line">
        <span class="answer-label">学生作答</span>
        <span>{{ answerText(question) }}</span>
        <NTag v-if="question.autoGrade" size="tiny" :type="isCorrect(question) ? 'success' : 'error'" :bordered="false">
          {{ isCorrect(question) ? '正确' : '错误' }}
        </NTag>
      </div>
      <div v-if="question.autoGrade" class="expected-line">参考答案：{{ expectedText(question) || '未设置' }}</div>
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
    </div>
    <div v-if="!questions.length" class="content-text">{{ fallbackContent || '无内容' }}</div>
  </div>
</template>

<style scoped>
.worksheet-result { display: flex; flex-direction: column; gap: 12px; }
.auto-score { padding: 10px 12px; border-radius: 8px; background: rgba(24, 160, 88, 0.1); color: var(--n-success-color); font-size: 13px; font-weight: 600; }
.answer-row { padding: 14px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-color); }
.question-title { font-size: 14px; font-weight: 600; line-height: 1.5; }
.question-md { margin-top: 6px; padding: 10px 12px; border-radius: 8px; background: var(--n-color-embedded); }
.question-image { display: block; max-width: 360px; width: 100%; max-height: 220px; object-fit: contain; margin-top: 10px; border-radius: 6px; border: 1px solid var(--n-border-color); }
.answer-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 10px; font-size: 14px; }
.answer-label { color: var(--n-text-color-3); font-size: 12px; }
.expected-line { margin-top: 6px; color: var(--n-text-color-3); font-size: 12px; }
.question-score-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--n-border-color); }
.question-score-cell { display: grid; grid-template-columns: minmax(110px, 1fr) 110px 42px; align-items: center; gap: 8px; font-size: 13px; color: var(--n-text-color-2); }
.score-max { color: var(--n-text-color-3); }
.content-text { padding: 12px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; font-size: 14px; min-height: 60px; white-space: pre-wrap; background: var(--n-color-embedded); }
@media (max-width: 720px) {
  .question-score-grid { grid-template-columns: 1fr; }
  .question-score-cell { grid-template-columns: 1fr; }
}
</style>
