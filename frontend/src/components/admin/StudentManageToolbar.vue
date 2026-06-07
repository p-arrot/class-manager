<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NIcon, NInput, NSelect, NSpace } from 'naive-ui'
import { AddOutline, CloudUploadOutline } from '@vicons/ionicons5'
import type { ClassVO } from '@/types/api'

const keyword = defineModel<string>('keyword', { required: true })
const classId = defineModel<number | undefined>('classId', { required: true })

const props = defineProps<{
  classes: ClassVO[]
  total: number
  checkedCount: number
}>()

const emit = defineEmits<{
  search: []
  create: []
  import: []
  batchResetPassword: []
  batchDelete: []
}>()

const classOptions = computed(() => [
  { label: '全部班级', value: 0 },
  ...props.classes.map(item => ({
    label: `${item.grade}级${item.name}`,
    value: item.id,
  })),
])

function handleClassChange(value: number | null) {
  classId.value = value || undefined
  emit('search')
}
</script>

<template>
  <div class="page-head">
    <h2 class="page-title">学生管理</h2>
    <div class="head-actions">
      <NInput
        v-model:value="keyword"
        placeholder="搜索学号或姓名"
        clearable
        size="small"
        class="search-input"
        @keyup.enter="emit('search')"
        @clear="emit('search')"
      />
      <NButton size="small" @click="emit('create')">
        <template #icon>
          <NIcon :size="16"><AddOutline /></NIcon>
        </template>
        新建学生
      </NButton>
      <NButton size="small" @click="emit('import')">
        <template #icon>
          <NIcon :size="16"><CloudUploadOutline /></NIcon>
        </template>
        导入 Excel
      </NButton>
    </div>
  </div>

  <div class="filter-bar">
    <NSelect
      :value="classId"
      :options="classOptions"
      placeholder="按班级筛选"
      clearable
      size="small"
      class="class-filter"
      @update:value="handleClassChange"
    />
    <span class="filter-total">共 {{ total }} 名学生</span>
    <NSpace v-if="checkedCount" :size="8" class="batch-actions">
      <span class="checked-count">已选 {{ checkedCount }} 项</span>
      <NButton size="tiny" @click="emit('batchResetPassword')">批量重置密码</NButton>
      <NButton size="tiny" type="error" @click="emit('batchDelete')">批量删除</NButton>
    </NSpace>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.search-input {
  width: 200px;
}
.class-filter {
  width: 180px;
}
.batch-actions {
  margin-left: auto;
}
.checked-count,
.filter-total {
  font-size: 13px;
  color: var(--n-text-color-3);
}
@media (max-width: 640px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .head-actions,
  .filter-bar,
  .batch-actions {
    width: 100%;
  }
  .head-actions :deep(.n-button),
  .search-input,
  .class-filter {
    flex: 1 1 100%;
    width: 100%;
  }
  .filter-total {
    width: 100%;
  }
}
</style>
