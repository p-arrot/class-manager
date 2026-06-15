<script setup lang="ts">
import { NButton, NCheckbox, NCheckboxGroup, NModal, NSpace } from 'naive-ui'
import type { ClassVO } from '@/types/api'

const show = defineModel<boolean>('show', { required: true })
const checkedClassIds = defineModel<number[]>('checkedClassIds', { required: true })

defineProps<{
  teacherName: string
  classes: ClassVO[]
}>()

const emit = defineEmits<{
  submit: []
}>()
</script>

<template>
  <NModal v-model:show="show" title="班级绑定" preset="card" class="form-modal form-modal-wide">
    <p class="bind-tip">
      为 <strong>{{ teacherName }}</strong> 选择授课班级
    </p>
    <NCheckboxGroup v-model:value="checkedClassIds">
      <NSpace vertical :size="6">
        <NCheckbox v-for="item in classes" :key="item.id" :value="item.id" :label="`${item.grade}级${item.name}`" />
      </NSpace>
    </NCheckboxGroup>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="show = false">取消</NButton>
        <NButton type="primary" @click="emit('submit')">保存</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.form-modal {
  width: min(420px, calc(100vw - 32px));
}
.form-modal-wide {
  width: min(460px, calc(100vw - 32px));
}
.bind-tip {
  margin: 0 0 16px;
  color: var(--n-text-color-2);
  font-size: 14px;
}
</style>
