<script setup lang="ts">
import { NButton, NIcon, NInput } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'

const keyword = defineModel<string>('keyword', { required: true })

defineProps<{
  total: number
}>()

const emit = defineEmits<{
  search: []
  create: []
}>()
</script>

<template>
  <div class="page-head">
    <h2 class="page-title">教师管理</h2>
    <div class="head-actions">
      <NInput
        v-model:value="keyword"
        placeholder="搜索用户名或姓名"
        clearable
        size="small"
        class="search-input"
        @keyup.enter="emit('search')"
        @clear="emit('search')"
      />
      <span class="total-text">共 {{ total }} 名教师</span>
      <NButton type="primary" size="small" @click="emit('create')">
        <template #icon>
          <NIcon :size="16"><AddOutline /></NIcon>
        </template>
        创建教师
      </NButton>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0;
  margin: 0;
}
.head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  min-width: 0;
}
.search-input {
  width: 200px;
}
.total-text {
  color: var(--n-text-color-3);
  font-size: 13px;
  white-space: nowrap;
}
@media (max-width: 640px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .head-actions {
    width: 100%;
  }
  .head-actions :deep(.n-button),
  .search-input {
    flex: 1 1 100%;
    width: 100%;
  }
  .total-text {
    width: 100%;
  }
}
</style>
