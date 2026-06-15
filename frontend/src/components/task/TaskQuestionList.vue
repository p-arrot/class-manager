<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NCard, NIcon, NSpace, NTag } from 'naive-ui'
import type { UploadCustomRequestOptions } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import TaskQuestionCard from '@/components/task/TaskQuestionCard.vue'
import { emptyQuestion, type QuestionType, type TaskQuestion } from '@/types/taskSchema'

const questions = defineModel<TaskQuestion[]>({ required: true })

const emit = defineEmits<{
  uploadImage: [question: TaskQuestion, options: UploadCustomRequestOptions]
}>()

const questionTypes: Array<{ label: string; value: QuestionType }> = [
  { label: '填空', value: 'blank' },
  { label: '单选', value: 'single' },
  { label: '多选', value: 'multiple' },
  { label: '是非', value: 'true_false' },
  { label: '简答', value: 'short' },
]

const typeLabelMap = computed(() => new Map(questionTypes.map(item => [item.value, item.label])))

function addQuestion(type: QuestionType) {
  questions.value.push(emptyQuestion(type))
}

function removeQuestion(index: number) {
  questions.value.splice(index, 1)
}
</script>

<template>
  <NCard size="small" class="panel">
    <template #header>
      <div class="panel-head">
        <span>题目设计</span>
        <NTag size="small" :bordered="false">{{ questions.length }} 题</NTag>
      </div>
    </template>

    <div class="question-list">
      <TaskQuestionCard
        v-for="(question, index) in questions"
        :key="question.id"
        v-model="questions[index]"
        :index="index"
        :type-label="typeLabelMap.get(question.type) ?? question.type"
        @remove="removeQuestion(index)"
        @upload-image="(target, options) => emit('uploadImage', target, options)"
      />
    </div>

    <NSpace :size="8" class="add-buttons">
      <NButton v-for="type in questionTypes" :key="type.value" size="small" @click="addQuestion(type.value)">
        <template #icon><NIcon :size="14"><AddOutline /></NIcon></template>
        {{ type.label }}
      </NButton>
    </NSpace>
  </NCard>
</template>

<style scoped>
.panel { border-radius: 8px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.question-list { display: flex; flex-direction: column; gap: 14px; }
.add-buttons { margin-top: 14px; flex-wrap: wrap; }
</style>
