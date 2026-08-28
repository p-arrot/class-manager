<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { formatDate } from '@/utils/date'
import {
  NButton,
  NDataTable,
  NSpace,
  NTag,
  NIcon,
  NEmpty,
  useMessage,
  useDialog
} from 'naive-ui'
import { CreateOutline, KeyOutline, LinkOutline, TrashOutline } from '@vicons/ionicons5'
import TeacherBindModal from '@/components/admin/TeacherBindModal.vue'
import TeacherFormModal from '@/components/admin/TeacherFormModal.vue'
import TeacherManageToolbar from '@/components/admin/TeacherManageToolbar.vue'
import { listTeachers, createTeacher, updateTeacher, getTeacherClasses, bindClasses, unbindClasses, deleteTeacher, resetTeacherPassword } from '@/api/teachers'
import { listAllClasses } from '@/api/classes'
import type { TeacherVO, ClassVO, PageQuery } from '@/types/api'
import type { DataTableColumns, FormRules } from 'naive-ui'
import { getErrorMessage } from '@/utils/error'
import type { TeacherFormValue } from '@/components/admin/TeacherFormModal.vue'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const records = ref<TeacherVO[]>([])
const total = ref(0)
const query = reactive<PageQuery>({ page: 1, size: 10, keyword: '' })
const teacherKeyword = ref('')

// Create/Edit modal
const showModal = ref(false)
const modalTitle = ref('创建教师')
const editingId = ref<number | null>(null)
const formRef = ref<InstanceType<typeof TeacherFormModal> | null>(null)
const formValue = reactive<TeacherFormValue>({
  username: '',
  name: '',
  password: '',
  phone: '',
  email: '',
  enabled: true
})
const createRules: FormRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  name: { required: true, message: '请输入姓名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur', min: 6 },
}

// Bind modal
const showBind = ref(false)
const bindTeacherId = ref<number | null>(null)
const bindTeacherName = ref('')
const allClasses = ref<ClassVO[]>([])
const checkedClassIds = ref<number[]>([])

const columns: DataTableColumns<TeacherVO> = [
  { title: '用户名', key: 'username', width: 130 },
  { title: '姓名', key: 'name', width: 120 },
  {
    title: '状态', key: 'enabled', width: 90,
    render(row: TeacherVO) {
      return h(NTag, {
        type: row.enabled ? 'success' : 'default',
        size: 'small',
        bordered: false
      }, () => row.enabled ? '正常' : '已禁用')
    },
  },
  {
    title: '负责班级', key: 'classIds', ellipsis: { tooltip: true },
    render(row: TeacherVO) {
      return row.classIds?.length ? `共 ${row.classIds.length} 个班级` : '未绑定'
    },
  },
  {
    title: '创建时间', key: 'createdAt', width: 170, render(row: TeacherVO) {
      return formatDate(row.createdAt)
    }
  },
  {
    title: '操作', key: 'actions', width: 200,
    render(row: TeacherVO) {
      return h(NSpace, { size: 2 }, () => [
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '绑定班级',
          'aria-label': '绑定班级',
          onClick: () => openBind(row)
        }, () => h(NIcon, { size: 15 }, () => h(LinkOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '编辑教师',
          'aria-label': '编辑教师',
          onClick: () => openEdit(row)
        }, () => h(NIcon, { size: 15 }, () => h(CreateOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '重置密码',
          'aria-label': '重置密码',
          onClick: () => handleResetPassword(row)
        }, () => h(NIcon, { size: 15 }, () => h(KeyOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          title: '删除教师',
          'aria-label': '删除教师',
          onClick: () => handleDelete(row)
        }, () => h(NIcon, { size: 15 }, () => h(TrashOutline))),
      ])
    },
  },
]

async function fetchData() {
  loading.value = true
  try {
    query.keyword = teacherKeyword.value
    const r = await listTeachers(query)
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

function openCreate() {
  modalTitle.value = '创建教师'
  editingId.value = null
  Object.assign(formValue, { username: '', name: '', password: '', phone: '', email: '', enabled: true })
  showModal.value = true
}

function openEdit(row: TeacherVO) {
  modalTitle.value = '编辑教师'
  editingId.value = row.id
  Object.assign(formValue, {
    username: row.username,
    name: row.name,
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    enabled: row.enabled
  })
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
      await updateTeacher(editingId.value, {
        name: formValue.name,
        phone: formValue.phone || undefined,
        email: formValue.email || undefined,
        enabled: formValue.enabled
      })
      message.success('更新成功')
    } else {
      await createTeacher({ username: formValue.username, name: formValue.name, password: formValue.password })
      message.success('创建成功')
    }
    showModal.value = false
    fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

async function openBind(row: TeacherVO) {
  bindTeacherId.value = row.id
  bindTeacherName.value = row.name
  try {
    const [classes, bindings] = await Promise.all([listAllClasses(), getTeacherClasses(row.id)])
    allClasses.value = classes
    checkedClassIds.value = bindings.map(b => b.classId)
    showBind.value = true
  } catch (e) {
    message.error(getErrorMessage(e, '加载失败'))
  }
}

function handleDelete(row: TeacherVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除教师「${row.name}」吗？教师的所有课程将保留。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteTeacher(row.id)
        message.success('已删除')
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '删除失败'))
      }
    },
  })
}

function handleResetPassword(row: TeacherVO) {
  dialog.warning({
    title: '重置密码',
    content: `将「${row.name}」的密码重置为默认密码 123456，确定继续？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await resetTeacherPassword(row.id)
        message.success('密码已重置为 123456')
      } catch (e) {
        message.error(getErrorMessage(e, '重置失败'))
      }
    },
  })
}

async function handleBind() {
  if (bindTeacherId.value === null) return
  try {
    const current = await getTeacherClasses(bindTeacherId.value)
    const currentIds = current.map(b => b.classId)
    const toAdd = checkedClassIds.value.filter(id => !currentIds.includes(id))
    const toRemove = currentIds.filter(id => !checkedClassIds.value.includes(id))
    if (toAdd.length) await bindClasses(bindTeacherId.value, { classIds: toAdd })
    if (toRemove.length) await unbindClasses(bindTeacherId.value, { classIds: toRemove })
    message.success('班级绑定已更新')
    showBind.value = false
    fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}


onMounted(fetchData)
</script>

<template>
  <div class="page">
    <TeacherManageToolbar v-model:keyword="teacherKeyword" :total="total" @search="fetchData" @create="openCreate" />

    <NDataTable :columns="columns" :data="records" :loading="loading" :scroll-x="820"
                :pagination="{ page: query.page, pageSize: query.size, itemCount: total, prefix: () => `共 ${total} 条` }"
                remote :row-key="(r: TeacherVO) => r.id" @update:page="handlePageChange"
                @update:page-size="handlePageSizeChange" size="small">
      <template #empty>
        <NEmpty description="暂无教师数据" />
      </template>
    </NDataTable>

    <TeacherFormModal
      ref="formRef"
      v-model:show="showModal"
      v-model:form-value="formValue"
      :title="modalTitle"
      :editing="Boolean(editingId)"
      :rules="createRules"
      @submit="handleSubmit"
    />

    <TeacherBindModal
      v-model:show="showBind"
      v-model:checked-class-ids="checkedClassIds"
      :teacher-name="bindTeacherName"
      :classes="allClasses"
      @submit="handleBind"
    />
  </div>
</template>

<style scoped>
.page {
  max-width: 1000px;
  animation: fadein 200ms ease;
  min-height: 0;
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
</style>
