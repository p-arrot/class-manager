<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { formatDate } from '@/utils/date'
import {
  NButton,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NIcon,
  NEmpty,
  useMessage,
  useDialog
} from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { listClasses, createClass, updateClass, deleteClass } from '@/api/classes'
import { getErrorMessage } from '@/utils/error'
import type { ClassVO, ClassCreateDTO, ClassUpdateDTO, ClassPageQuery } from '@/types/api'
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const records = ref<ClassVO[]>([])
const total = ref(0)
const query = reactive<ClassPageQuery>({ page: 1, size: 10, grade: undefined, keyword: '' })

const showModal = ref(false)
const modalTitle = ref('新建班级')
const editingId = ref<number | null>(null)
const formRef = ref<FormInst | null>(null)
const formValue = reactive<ClassCreateDTO>({ grade: '', name: '' })

const rules: FormRules = {
  grade: { required: true, message: '请输入入学年份', trigger: 'blur' },
  name: { required: true, message: '请输入班级名称', trigger: 'blur' },
}

const gradeOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({length: 9}, (_, i) => {
    const year = currentYear - 5 + i
    return { label: year + '级', value: String(year) }
  })
})

const columns: DataTableColumns<ClassVO> = [
  {
    title: '年级', key: 'grade', width: 120,
    render(row: ClassVO) { return row.grade + '级' },
    filterOptions: gradeOptions.value,
    filter(value: string | number, row: ClassVO) {
      return !value || row.grade === value
    },
  },
  { title: '班级名称', key: 'name', width: 150 },
  {
    title: '创建时间', key: 'createdAt', width: 180, render(row: ClassVO) {
      return formatDate(row.createdAt)
    }
  },
  {
    title: '操作', key: 'actions', width: 140,
    render(row: ClassVO) {
      return h(NSpace, { size: 2 }, () => [
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '编辑班级',
          'aria-label': '编辑班级',
          onClick: () => openEdit(row)
        }, () => h(NIcon, { size: 15 }, () => h(CreateOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '删除班级',
          'aria-label': '删除班级',
          onClick: () => handleDelete(row)
        }, () => h(NIcon, { size: 15 }, () => h(TrashOutline))),
      ])
    },
  },
]

async function fetchData() {
  loading.value = true
  try {
    const r = await listClasses(query)
    records.value = r.records
    total.value = r.total
  } catch (e) {
    message.error(getErrorMessage(e, '加载失败'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  query.size = size
  query.page = 1
  fetchData()
}

function handleFilter(filters: Record<string, unknown>) {
  const grade = Array.isArray(filters.grade) ? filters.grade[0] : filters.grade
  query.grade = typeof grade === 'string' ? grade : undefined
  query.page = 1
  fetchData()
}

function openCreate() {
  modalTitle.value = '新建班级'
  editingId.value = null
  formValue.grade = ''
  formValue.name = ''
  showModal.value = true
}

function openEdit(row: ClassVO) {
  modalTitle.value = '编辑班级'
  editingId.value = row.id
  formValue.grade = row.grade
  formValue.name = row.name
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    if (editingId.value) {
      await updateClass(editingId.value, formValue as ClassUpdateDTO)
      message.success('更新成功')
    } else {
      await createClass(formValue as ClassCreateDTO)
      message.success('创建成功')
    }
    showModal.value = false
    fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

function handleDelete(row: ClassVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除 ${row.grade}${row.name}？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteClass(row.id)
        message.success('已删除')
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '删除失败'))
      }
    },
  })
}


onMounted(fetchData)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">班级管理</h2>
      <NButton type="primary" size="small" @click="openCreate">
        <template #icon>
          <NIcon :size="16">
            <AddOutline />
          </NIcon>
        </template>
        新建班级
      </NButton>
    </div>
    <NDataTable :columns="columns" :data="records" :loading="loading" :scroll-x="760"
                :pagination="{ page: query.page, pageSize: query.size, itemCount: total, prefix: () => `共 ${total} 条` }"
                remote :row-key="(r: ClassVO) => r.id" @update:page="handlePageChange"
                @update:page-size="handlePageSizeChange" @update:filters="handleFilter"
                size="small">
      <template #empty>
        <NEmpty description="暂无班级数据" />
      </template>
    </NDataTable>

    <NModal v-model:show="showModal" :title="modalTitle" preset="card" class="form-modal">
      <NForm ref="formRef" :model="formValue" :rules="rules" label-placement="left" label-width="72">
        <NFormItem label="年级" path="grade">
          <NInput v-model:value="formValue.grade" placeholder="如：2026" />
        </NFormItem>
        <NFormItem label="班级名称" path="name">
          <NInput v-model:value="formValue.name" placeholder="如：1班" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 900px;
  min-height: 0;
  animation: fadein 200ms ease;
  display: flex;
  flex-direction: column;
}

.table-wrap {
  flex: 1;
}

@keyframes fadein {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}

.form-modal {
  width: min(400px, calc(100vw - 32px));
}

@media (max-width: 640px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .page-head :deep(.n-button) {
    width: 100%;
  }
}
</style>
