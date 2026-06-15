<script setup lang="ts">
import { NButton, NDatePicker, NForm, NFormItem, NInput, NModal, NSelect, NSpace } from 'naive-ui'
import { CORE_DIMENSIONS, normalizeDimensionScores } from '@/types/taskSchema'
import type { ProjectFormValue } from '@/types/project'

const show = defineModel<boolean>('show', { required: true })
const form = defineModel<ProjectFormValue>('form', { required: true })

withDefaults(defineProps<{
  title?: string
}>(), {
  title: '创建项目',
})

const emit = defineEmits<{
  submit: []
}>()

const submitModeOptions = [
  { label: '文件', value: 'file' },
  { label: '文件夹', value: 'folder' },
]
</script>

<template>
  <NModal v-model:show="show" :title="title" preset="card" class="project-modal">
    <NForm label-placement="left" label-width="92">
      <NFormItem label="名称">
        <NInput v-model:value="form.name" />
      </NFormItem>
      <NFormItem label="说明">
        <NInput v-model:value="form.description" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" />
      </NFormItem>
      <NFormItem label="组队上限">
        <NInput :value="String(form.maxTeamSize)" @update:value="value => { form.maxTeamSize = Number(value) || 1 }" />
      </NFormItem>
      <NFormItem label="截止时间">
        <NDatePicker v-model:value="form.deadline" type="datetime" clearable class="full-field" />
      </NFormItem>
      <NFormItem label="提交方式">
        <NSelect v-model:value="form.submitMode" :options="submitModeOptions" />
      </NFormItem>
      <NFormItem label="文件后缀">
        <NInput v-model:value="form.allowedExtensions" placeholder="如：zip, py, docx；留空表示不限" />
      </NFormItem>
      <NFormItem label="项目分值">
        <div class="rubric-grid">
          <label v-for="dim in CORE_DIMENSIONS" :key="dim.key" class="rubric-cell">
            <span>{{ dim.label }}</span>
            <NInput
              :value="String(form.dimensionScores.find(item => item.dimension === dim.key)?.maxScore ?? 0)"
              size="small"
              @update:value="value => {
                form.dimensionScores = normalizeDimensionScores(form.dimensionScores)
                const target = form.dimensionScores.find(item => item.dimension === dim.key)
                if (target) target.maxScore = Math.max(0, Number(value) || 0)
              }"
            >
              <template #suffix>分</template>
            </NInput>
          </label>
        </div>
      </NFormItem>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="show = false">取消</NButton>
        <NButton type="primary" @click="emit('submit')">确定</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.project-modal {
  width: min(520px, calc(100vw - 32px));
}
.full-field {
  width: 100%;
}
.rubric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}
.rubric-cell {
  display: grid;
  grid-template-columns: minmax(110px, 1fr) 100px;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: var(--n-text-color-2);
}
@media (max-width: 720px) {
  .rubric-grid,
  .rubric-cell {
    grid-template-columns: 1fr;
  }
}
</style>
