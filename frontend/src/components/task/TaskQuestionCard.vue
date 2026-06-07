<script setup lang="ts">
import {
  NButton,
  NCheckbox,
  NIcon,
  NInput,
  NRadio,
  NRadioGroup,
  NSelect,
  NTag,
  NTooltip,
  NUpload,
} from 'naive-ui'
import type { UploadCustomRequestOptions } from 'naive-ui'
import { ImageOutline, TrashOutline } from '@vicons/ionicons5'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import {
  CORE_DIMENSIONS,
  normalizeDimensionScores,
  questionTotalScore,
  type TaskQuestion,
} from '@/types/taskSchema'

const question = defineModel<TaskQuestion>({ required: true })

defineProps<{
  index: number
  typeLabel: string
}>()

const emit = defineEmits<{
  remove: []
  uploadImage: [question: TaskQuestion, options: UploadCustomRequestOptions]
}>()

function addOption() {
  question.value.options = [...(question.value.options ?? []), `选项 ${(question.value.options?.length ?? 0) + 1}`]
}

function removeOption(index: number) {
  const removed = question.value.options?.[index]
  question.value.options?.splice(index, 1)
  if (Array.isArray(question.value.answer)) {
    question.value.answer = question.value.answer.filter(item => item !== removed)
  } else if (question.value.answer === removed) {
    question.value.answer = ''
  }
}

function textAnswer(): string {
  return typeof question.value.answer === 'string' ? question.value.answer : ''
}

function setTextAnswer(value: string) {
  question.value.answer = value
}

function singleAnswer(): string | null {
  return typeof question.value.answer === 'string' ? question.value.answer : null
}

function setSingleAnswer(value: string) {
  question.value.answer = value
}

function multipleAnswer(): string[] {
  return Array.isArray(question.value.answer) ? question.value.answer : []
}

function setMultipleAnswer(value: string[]) {
  question.value.answer = value
}

function booleanAnswer(): boolean | null {
  return typeof question.value.answer === 'boolean' ? question.value.answer : null
}

function setBooleanAnswer(value: boolean) {
  question.value.answer = value
}

function updateDimensionScore(dimension: string, value: string) {
  question.value.dimensionScores = normalizeDimensionScores(question.value.dimensionScores)
  const target = question.value.dimensionScores.find(item => item.dimension === dimension)
  if (target) target.maxScore = Math.max(0, Number(value) || 0)
}

function answerOptions() {
  return (question.value.options ?? [])
    .filter(Boolean)
    .map(item => ({ label: item, value: item }))
}
</script>

<template>
  <article class="question-card">
    <div class="question-toolbar">
      <NTag size="small" :bordered="false">{{ typeLabel }}</NTag>
      <span class="question-no">第 {{ index + 1 }} 题</span>
      <NTooltip trigger="hover">
        <template #trigger>
          <NButton size="tiny" quaternary title="删除题目" aria-label="删除题目" @click="emit('remove')">
            <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
          </NButton>
        </template>
        删除题目
      </NTooltip>
    </div>

    <MarkdownEditor v-model="question.stem" :min-height="190" />

    <div v-if="question.imageUrl" class="image-preview">
      <img :src="question.imageUrl" alt="题目配图" />
      <NButton size="tiny" quaternary @click="question.imageUrl = undefined">移除图片</NButton>
    </div>
    <NUpload v-else :show-file-list="false" accept="image/*" :custom-request="options => emit('uploadImage', question, options)">
      <NButton size="tiny" text>
        <template #icon><NIcon :size="13"><ImageOutline /></NIcon></template>
        插入配图
      </NButton>
    </NUpload>

    <div v-if="question.type === 'single' || question.type === 'multiple'" class="options">
      <div v-for="(_option, optionIndex) in question.options" :key="optionIndex" class="option-row">
        <span class="option-mark">{{ question.type === 'single' ? '○' : '□' }}</span>
        <NInput v-model:value="question.options![optionIndex]" size="small" placeholder="选项文字" />
        <NButton size="tiny" quaternary title="删除选项" aria-label="删除选项" @click="removeOption(optionIndex)">
          <template #icon><NIcon :size="13"><TrashOutline /></NIcon></template>
        </NButton>
      </div>
      <NButton size="tiny" text @click="addOption">添加选项</NButton>
    </div>

    <div class="question-settings">
      <NCheckbox v-model:checked="question.required">必填</NCheckbox>
      <NCheckbox v-model:checked="question.autoGrade">自动批改</NCheckbox>
      <NTag size="small" :bordered="false">总分 {{ questionTotalScore(question) }}</NTag>
    </div>

    <div class="dimension-grid">
      <label v-for="dimension in CORE_DIMENSIONS" :key="dimension.key" class="dimension-cell">
        <span>{{ dimension.label }}</span>
        <NInput
          :value="String(question.dimensionScores.find(item => item.dimension === dimension.key)?.maxScore ?? 0)"
          size="small"
          @update:value="value => updateDimensionScore(dimension.key, value)"
        >
          <template #suffix>分</template>
        </NInput>
      </label>
    </div>

    <div v-if="question.autoGrade" class="answer-box">
      <NSelect
        v-if="question.type === 'single'"
        :value="singleAnswer()"
        :options="answerOptions()"
        placeholder="选择正确答案"
        @update:value="value => setSingleAnswer(value)"
      />
      <NSelect
        v-else-if="question.type === 'multiple'"
        :value="multipleAnswer()"
        multiple
        :options="answerOptions()"
        placeholder="选择正确答案"
        @update:value="value => setMultipleAnswer(value)"
      />
      <NRadioGroup v-else-if="question.type === 'true_false'" :value="booleanAnswer()" @update:value="value => setBooleanAnswer(value)">
        <NRadio :value="true">正确</NRadio>
        <NRadio :value="false">错误</NRadio>
      </NRadioGroup>
      <NInput v-else :value="textAnswer()" placeholder="填写正确答案；留空则不自动判分" @update:value="value => setTextAnswer(value)" />
    </div>
  </article>
</template>

<style scoped>
.question-card { display: flex; flex-direction: column; gap: 10px; padding: 14px; border: 1px solid var(--n-border-color); border-radius: 8px; }
.question-toolbar { display: flex; align-items: center; gap: 8px; }
.question-toolbar .n-button { margin-left: auto; }
.question-no { font-size: 12px; color: var(--n-text-color-3); }
.image-preview { display: flex; align-items: flex-end; gap: 12px; }
.image-preview img { max-width: min(320px, 100%); max-height: 200px; object-fit: contain; border-radius: 8px; border: 1px solid var(--n-border-color); }
.options { display: flex; flex-direction: column; gap: 6px; padding: 8px 0 0 18px; }
.option-row { display: flex; align-items: center; gap: 6px; }
.option-mark { width: 18px; color: var(--n-text-color-3); }
.question-settings { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.dimension-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; padding: 10px 12px; border-radius: 8px; background: var(--n-color-embedded); }
.dimension-cell { display: grid; grid-template-columns: minmax(120px, 1fr) 110px; gap: 8px; align-items: center; font-size: 13px; color: var(--n-text-color-2); }
.answer-box { padding: 10px 12px; border-radius: 6px; background: var(--n-color-embedded); }
@media (max-width: 640px) {
  .question-card { padding: 12px; }
  .question-settings { align-items: stretch; flex-direction: column; }
  .dimension-grid { grid-template-columns: 1fr; }
  .dimension-cell { grid-template-columns: 1fr; }
}
</style>
