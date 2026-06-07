<script setup lang="ts">
import { computed, ref } from 'vue'
import { NButton, NForm, NFormItem, NInput, NModal, NSelect, NSpace } from 'naive-ui'
import type { ClassVO } from '@/types/api'
import type { FormInst, FormRules } from 'naive-ui'

export interface StudentFormValue {
  studentNo: string
  name: string
  classId: number | null
  password: string
}

const show = defineModel<boolean>('show', { required: true })
const formValue = defineModel<StudentFormValue>('formValue', { required: true })
const formRef = ref<FormInst | null>(null)

const props = defineProps<{
  title: string
  editing: boolean
  classes: ClassVO[]
  rules?: FormRules
}>()

const emit = defineEmits<{
  submit: []
}>()

const classOptions = computed(() => props.classes.map(item => ({
  label: `${item.grade}级${item.name}`,
  value: item.id,
})))

async function validate() {
  await formRef.value?.validate()
}

defineExpose({ validate })
</script>

<template>
  <NModal v-model:show="show" :title="title" preset="card" class="form-modal">
    <NForm
      ref="formRef"
      :model="formValue"
      :rules="editing ? undefined : rules"
      label-placement="left"
      label-width="72"
    >
      <NFormItem label="学号" path="studentNo">
        <NInput v-model:value="formValue.studentNo" placeholder="全局唯一" :disabled="editing" />
      </NFormItem>
      <NFormItem label="姓名" path="name">
        <NInput v-model:value="formValue.name" placeholder="学生姓名" />
      </NFormItem>
      <NFormItem label="班级" path="classId">
        <NSelect v-model:value="formValue.classId" :options="classOptions" placeholder="选择班级" />
      </NFormItem>
      <NFormItem v-if="!editing" label="密码">
        <NInput v-model:value="formValue.password" type="password" placeholder="留空默认 123456" />
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
.form-modal {
  width: min(420px, calc(100vw - 32px));
}
</style>
