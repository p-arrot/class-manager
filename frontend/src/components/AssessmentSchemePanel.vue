<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { NAlert, NButton, NForm, NFormItem, NInputNumber, useMessage } from 'naive-ui'
import { getAssessmentScheme, saveAssessmentScheme } from '@/api/semesters'
import { getErrorMessage } from '@/utils/error'

const props = defineProps<{ semesterId: number | null }>()
const message = useMessage()

const form = reactive({ processPercent: 50, examPercent: 50, projectPercent: 0 })
const total = computed(() => form.processPercent + form.examPercent + form.projectPercent)
const isValid = computed(() => total.value === 100)

async function loadScheme() {
  if (!props.semesterId) return
  try {
    const scheme = await getAssessmentScheme(props.semesterId)
    form.processPercent = scheme.processPercent
    form.examPercent = scheme.examPercent
    form.projectPercent = scheme.projectPercent
  } catch (e) {
    message.error(getErrorMessage(e, '加载考核方案失败'))
  }
}

async function handleSave() {
  if (!props.semesterId) return
  if (!isValid.value) {
    message.warning('三项占比之和必须为100%')
    return
  }
  try {
    await saveAssessmentScheme(props.semesterId, { ...form })
    message.success('考核方案已保存')
  } catch (e) {
    message.error(getErrorMessage(e, '保存失败'))
  }
}

watch(() => props.semesterId, loadScheme, { immediate: true })
</script>

<template>
  <div class="scheme-panel">
    <div class="scheme-head">
      <div>
        <h3>设置考核方案</h3>
        <p>用于折算该学期总评，考试和项目自身不再设置权重。</p>
      </div>
      <NButton type="primary" size="small" :disabled="!semesterId || !isValid" @click="handleSave">保存方案</NButton>
    </div>

    <NAlert v-if="!semesterId" type="info" :bordered="false">请选择学期后设置考核方案。</NAlert>
    <template v-else>
      <NForm label-placement="top" class="scheme-form">
        <NFormItem label="平时任务">
          <NInputNumber v-model:value="form.processPercent" :min="0" :max="100" button-placement="both" class="percent-input">
            <template #suffix>%</template>
          </NInputNumber>
        </NFormItem>
        <NFormItem label="考试">
          <NInputNumber v-model:value="form.examPercent" :min="0" :max="100" button-placement="both" class="percent-input">
            <template #suffix>%</template>
          </NInputNumber>
        </NFormItem>
        <NFormItem label="项目">
          <NInputNumber v-model:value="form.projectPercent" :min="0" :max="100" button-placement="both" class="percent-input">
            <template #suffix>%</template>
          </NInputNumber>
        </NFormItem>
      </NForm>
      <div :class="['scheme-total', { invalid: !isValid }]">
        当前合计：{{ total }}%
        <span v-if="!isValid">，必须等于 100%</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.scheme-panel { padding: 16px; border: 1px solid var(--n-border-color); border-radius: 8px; margin-bottom: 18px; }
.scheme-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 14px; }
.scheme-head h3 { margin: 0 0 4px; font-size: 16px; }
.scheme-head p { margin: 0; color: var(--n-text-color-3); font-size: 13px; line-height: 1.5; }
.scheme-form { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.percent-input { width: 100%; }
.scheme-total { font-size: 13px; color: var(--n-text-color-2); }
.scheme-total.invalid { color: var(--n-error-color); }
@media (max-width: 720px) {
  .scheme-head { flex-direction: column; }
  .scheme-form { grid-template-columns: 1fr; }
}
</style>
