<script setup lang="ts">
import { ref } from 'vue'
import { NButton, NForm, NFormItem, NInput, NModal, NSpace, NSwitch } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'

export interface TeacherFormValue {
  username: string
  name: string
  password: string
  phone: string
  email: string
  enabled: boolean
}

const show = defineModel<boolean>('show', { required: true })
const formValue = defineModel<TeacherFormValue>('formValue', { required: true })
const formRef = ref<FormInst | null>(null)

defineProps<{
  title: string
  editing: boolean
  rules?: FormRules
}>()

const emit = defineEmits<{
  submit: []
}>()

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
      <NFormItem v-if="!editing" label="用户名" path="username">
        <NInput v-model:value="formValue.username" placeholder="登录用户名" />
      </NFormItem>
      <NFormItem label="姓名" path="name">
        <NInput v-model:value="formValue.name" placeholder="真实姓名" />
      </NFormItem>
      <NFormItem v-if="!editing" label="密码" path="password">
        <NInput v-model:value="formValue.password" type="password" placeholder="最少6位" />
      </NFormItem>
      <NFormItem v-if="editing" label="电话">
        <NInput v-model:value="formValue.phone" placeholder="选填" />
      </NFormItem>
      <NFormItem v-if="editing" label="邮箱">
        <NInput v-model:value="formValue.email" placeholder="选填" />
      </NFormItem>
      <NFormItem v-if="editing" label="启用">
        <NSwitch v-model:value="formValue.enabled" />
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
