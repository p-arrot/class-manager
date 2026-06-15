<script setup lang="ts">
import { NButton, NInput, NModal, NSpace } from 'naive-ui'

const show = defineModel<boolean>('show', { required: true })
const password = defineModel<string>('password', { required: true })

defineProps<{
  studentName: string
}>()

const emit = defineEmits<{
  submit: []
}>()
</script>

<template>
  <NModal v-model:show="show" title="重置密码" preset="card" class="form-modal form-modal-narrow">
    <p class="password-copy">为 <strong>{{ studentName }}</strong> 重置密码</p>
    <NInput v-model:value="password" placeholder="留空则重置为默认密码 123456" />
    <template #footer>
      <NSpace justify="end">
        <NButton @click="show = false">取消</NButton>
        <NButton type="primary" @click="emit('submit')">确认重置</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.form-modal {
  width: min(420px, calc(100vw - 32px));
}
.form-modal-narrow {
  width: min(380px, calc(100vw - 32px));
}
.password-copy {
  margin: 0 0 16px;
  font-size: 14px;
}
</style>
