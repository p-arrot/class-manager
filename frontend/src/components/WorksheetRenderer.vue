<script setup lang="ts">
import { computed } from 'vue'
import { NCheckbox, NCheckboxGroup, NInput, NRadio, NRadioGroup, NTag } from 'naive-ui'
import MarkdownView from '@/components/MarkdownView.vue'
import { parseTaskSchema, questionStem, type TaskQuestion } from '@/types/taskSchema'
import type { WorksheetAnswerMap, WorksheetAnswerValue } from '@/types/taskSchema'

type CheckboxGroupValue = Array<string | number>

const props = withDefaults(defineProps<{ schema: string; readonly?: boolean }>(), { readonly: false })
const model = defineModel<WorksheetAnswerMap>({ default: () => ({}) })

const questions = computed<TaskQuestion[]>(() => parseTaskSchema(props.schema).questions ?? [])

function setAnswer(id: string, value: WorksheetAnswerValue) {
  if (props.readonly) return
  model.value = { ...model.value, [id]: value }
}

function setMultipleAnswer(id: string, value: CheckboxGroupValue) {
  if (props.readonly) return
  model.value = { ...model.value, [id]: value.map(String) }
}

function stringAnswer(id: string): string {
  const value = model.value[id]
  return typeof value === 'string' ? value : ''
}

function multipleAnswer(id: string): Array<string | number> {
  const value = model.value[id]
  return Array.isArray(value) ? value : []
}

function booleanAnswer(id: string): boolean | null {
  const value = model.value[id]
  return typeof value === 'boolean' ? value : null
}

</script>

<template>
  <div v-if="questions.length" class="survey">
    <article v-for="(q, index) in questions" :key="q.id" class="question">
      <div class="question-index">{{ index + 1 }}</div>
      <div class="question-body">
        <header class="question-head">
          <div class="question-title">
            <span>第 {{ index + 1 }} 题</span>
            <NTag v-if="q.required" size="tiny" type="error" :bordered="false">必填</NTag>
          </div>
          <MarkdownView class="question-markdown" :content="questionStem(q) || '未填写题干'" />
        </header>

        <img v-if="q.imageUrl" :src="q.imageUrl" class="question-image" alt="题目配图" />

        <NRadioGroup
          v-if="q.type === 'single'"
          :value="stringAnswer(q.id)"
          class="choice-list"
          :disabled="readonly"
          @update:value="value => setAnswer(q.id, value)"
        >
          <NRadio v-for="option in q.options ?? []" :key="option" :value="option" class="choice-option">
            {{ option }}
          </NRadio>
        </NRadioGroup>

        <NCheckboxGroup
          v-else-if="q.type === 'multiple'"
          :value="multipleAnswer(q.id)"
          class="choice-list"
          :disabled="readonly"
          @update:value="value => setMultipleAnswer(q.id, value)"
        >
          <NCheckbox v-for="option in q.options ?? []" :key="option" :value="option" class="choice-option">
            {{ option }}
          </NCheckbox>
        </NCheckboxGroup>

        <NRadioGroup
          v-else-if="q.type === 'true_false'"
          :value="booleanAnswer(q.id)"
          class="choice-list"
          :disabled="readonly"
          @update:value="value => setAnswer(q.id, value)"
        >
          <NRadio :value="true" class="choice-option">正确</NRadio>
          <NRadio :value="false" class="choice-option">错误</NRadio>
        </NRadioGroup>

        <NInput
          v-else-if="q.type === 'blank'"
          :value="stringAnswer(q.id)"
          placeholder="请输入答案"
          :readonly="readonly"
          @update:value="value => setAnswer(q.id, value)"
        />

        <NInput
          v-else
          type="textarea"
          :value="stringAnswer(q.id)"
          placeholder="请输入作答内容"
          :readonly="readonly"
          :autosize="{ minRows: 4, maxRows: 12 }"
          @update:value="value => setAnswer(q.id, value)"
        />
      </div>
    </article>
  </div>
  <div v-else class="survey-empty">此学习单暂无题目</div>
</template>

<style scoped>
.survey { display: flex; flex-direction: column; gap: 18px; }
.survey-empty { text-align: center; padding: 40px; color: var(--n-text-color-3); font-size: 14px; }
.question { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 14px; padding: 18px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-card-color); }
.question-index { width: 34px; height: 34px; border-radius: 999px; background: var(--n-primary-color); color: white; display: grid; place-items: center; font-size: 14px; font-weight: 650; }
.question-body { min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.question-head { display: flex; flex-direction: column; gap: 6px; }
.question-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 650; line-height: 1.5; }
.question-markdown { background: var(--n-color-embedded); border-radius: 6px; padding: 10px 12px; }
.question-image { max-width: min(100%, 520px); max-height: 260px; border-radius: 8px; border: 1px solid var(--n-border-color); object-fit: contain; }
.choice-list { display: flex; flex-direction: column; gap: 8px; }
.choice-option { padding: 8px 10px; border-radius: 6px; transition: background-color 150ms ease; }
.choice-option:hover { background: var(--n-color-embedded); }
@media (max-width: 640px) {
  .question { grid-template-columns: 1fr; padding: 14px; }
  .question-index { width: 28px; height: 28px; }
}
</style>
