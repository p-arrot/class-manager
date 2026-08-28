<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { formatDate } from '@/utils/date'
import {
  NButton,
  NDataTable,
  NTag,
  NSpace,
  NIcon,
  NEmpty,
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormRules,
  type UploadFileInfo
} from 'naive-ui'
import { CreateOutline, TrashOutline, KeyOutline } from '@vicons/ionicons5'
import StudentFormModal, { type StudentFormValue } from '@/components/admin/StudentFormModal.vue'
import StudentImportModal from '@/components/admin/StudentImportModal.vue'
import StudentManageToolbar from '@/components/admin/StudentManageToolbar.vue'
import StudentPasswordModal from '@/components/admin/StudentPasswordModal.vue'
import {
  listStudents,
  createStudent,
  updateStudent,
  deleteStudent,
  importStudents,
  resetPassword,
  batchDeleteStudents,
  batchResetPassword
} from '@/api/students'
import { listAllClasses } from '@/api/classes'
import type { StudentVO, StudentImportResultVO, StudentPageQuery, ClassVO } from '@/types/api'
import { getErrorMessage } from '@/utils/error'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const records = ref<StudentVO[]>([])
const total = ref(0)
const query = reactive<StudentPageQuery & { keyword: string }>({ page: 1, size: 10, classId: undefined, keyword: '' })
const classes = ref<ClassVO[]>([])
const checkedRowKeys = ref<number[]>([])

// Create/Edit modal
const showModal = ref(false)
const modalTitle = ref('新建学生')
const editingId = ref<number | null>(null)
const formRef = ref<InstanceType<typeof StudentFormModal> | null>(null)
const formValue = reactive<StudentFormValue>({ studentNo: '', name: '', classId: null, password: '' })
const createRules: FormRules = {
  studentNo: { required: true, message: '请输入学号', trigger: 'blur' },
  name: { required: true, message: '请输入姓名', trigger: 'blur' },
  classId: { required: true, type: 'number', message: '请选择班级', trigger: 'change' },
}

// Import modal
const showImport = ref(false)
const importResult = ref<StudentImportResultVO | null>(null)
const uploading = ref(false)

// Password modal
const showPwd = ref(false)
const pwdStudentId = ref<number | null>(null)
const pwdStudentName = ref('')
const newPassword = ref('')

const columns: DataTableColumns<StudentVO> = [
  { type: 'selection' },
  { title: '学号', key: 'studentNo', width: 120 },
  { title: '姓名', key: 'name', width: 100 },
  {
    title: '班级', key: 'className', width: 130, render(row) {
      return row.grade && row.className ? row.grade + '级' + row.className : '—'
    }
  },
  {
    title: '状态', key: 'enabled', width: 80, render(row) {
      return h(NTag, {
        type: row.enabled ? 'success' : 'default',
        size: 'small',
        bordered: false
      }, () => row.enabled ? '正常' : '已禁用')
    }
  },
  {
    title: '创建时间', key: 'createdAt', width: 170, render(row) {
      return formatDate(row.createdAt)
    }
  },
  {
    title: '操作', key: 'actions', width: 140,
    render(row) {
      return h(NSpace, {size: 2}, () => [
        h(NButton, {
        size: 'tiny',
        quaternary: true,
        title: '编辑学生',
        'aria-label': '编辑学生',
        onClick: () => openEdit(row)
      }, () => h(NIcon, {size: 15}, () => h(CreateOutline))),
        h(NButton, {
        size: 'tiny',
        quaternary: true,
        title: '重置密码',
        'aria-label': '重置密码',
        onClick: () => openResetPwd(row)
      }, () => h(NIcon, {size: 15}, () => h(KeyOutline))),
        h(NButton, {
        size: 'tiny',
        quaternary: true,
        title: '删除学生',
        'aria-label': '删除学生',
        onClick: () => handleDelete(row)
      }, () => h(NIcon, {size: 15}, () => h(TrashOutline))),
      ])
    },
  },
]

async function fetchData() {
  loading.value = true
  try {
    const r = await listStudents(query)
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

function handleSearch() {
  query.page = 1
  fetchData()
}

function handleCheck(keys: Array<string | number>) {
  checkedRowKeys.value = keys.map(Number).filter(Number.isFinite)
}

async function loadClasses() {
  try {
    classes.value = await listAllClasses()
  } catch (e) {
    classes.value = []
    message.error(getErrorMessage(e, '加载班级列表失败'))
  }
}

// ---- CRUD ----
function openCreate() {
  modalTitle.value = '新建学生'
  editingId.value = null
  Object.assign(formValue, { studentNo: '', name: '', classId: null, password: '' })
  showModal.value = true
}

function openEdit(row: StudentVO) {
  modalTitle.value = '编辑学生'
  editingId.value = row.id
  Object.assign(formValue, { studentNo: row.studentNo, name: row.name, classId: row.classId, password: '' })
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
      await updateStudent(editingId.value, {name: formValue.name, classId: formValue.classId ?? undefined})
      message.success('更新成功')
    } else {
      await createStudent({
        studentNo: formValue.studentNo,
        name: formValue.name,
        classId: formValue.classId!,
        password: formValue.password || undefined
      })
      message.success('创建成功')
    }
    showModal.value = false;
    fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}

function handleDelete(row: StudentVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除学生 ${row.name}(${row.studentNo})？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteStudent(row.id)
        message.success('已删除')
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '删除失败'))
      }
    },
  })
}

// ---- Batch operations ----
function handleBatchDelete() {
  if (!checkedRowKeys.value.length) return
  dialog.warning({
    title: '确认批量删除',
    content: `确定删除选中的 ${checkedRowKeys.value.length} 名学生？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await batchDeleteStudents({ ids: checkedRowKeys.value })
        message.success('已删除')
        checkedRowKeys.value = []
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '操作失败'))
      }
    },
  })
}

function handleBatchResetPwd() {
  if (!checkedRowKeys.value.length) return
  dialog.warning({
    title: '批量重置密码',
    content: `确定将选中的 ${checkedRowKeys.value.length} 名学生密码重置为 123456？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await batchResetPassword({ ids: checkedRowKeys.value })
        message.success('已重置')
        checkedRowKeys.value = []
        fetchData()
      } catch (e) {
        message.error(getErrorMessage(e, '操作失败'))
      }
    },
  })
}

// ---- Import ----
function openImport() {
  importResult.value = null
  showImport.value = true
}

async function handleUpload({file}: { file: UploadFileInfo; fileList: UploadFileInfo[] }) {
  if (!file.file) return
  uploading.value = true
  try {
    importResult.value = await importStudents(file.file)
  } catch (e) {
    message.error(getErrorMessage(e, '导入失败'))
  } finally {
    uploading.value = false
  }
}

function openResetPwd(row: StudentVO) {
  pwdStudentId.value = row.id
  pwdStudentName.value = row.name
  newPassword.value = ''
  showPwd.value = true
}

async function handleResetPwd() {
  if (!pwdStudentId.value) return
  try {
    await resetPassword(pwdStudentId.value, { newPassword: newPassword.value || undefined })
    message.success('密码已重置')
    showPwd.value = false
  } catch (e) {
    message.error(getErrorMessage(e, '操作失败'))
  }
}


onMounted(() => {
  fetchData()
  loadClasses()
})
</script>

<template>
  <div class="page">
    <StudentManageToolbar
      v-model:keyword="query.keyword"
      v-model:class-id="query.classId"
      :classes="classes"
      :total="total"
      :checked-count="checkedRowKeys.length"
      @search="handleSearch"
      @create="openCreate"
      @import="openImport"
      @batch-reset-password="handleBatchResetPwd"
      @batch-delete="handleBatchDelete"
    />

    <NDataTable :columns="columns" :data="records" :loading="loading" :scroll-x="920"
                :pagination="{ page: query.page, pageSize: query.size, itemCount: total }" remote
                :row-key="(r: StudentVO) => r.id" :checked-row-keys="checkedRowKeys" @update:page="handlePageChange"
                @update:page-size="handlePageSizeChange"
                @update:checked-row-keys="handleCheck" size="small">
      <template #empty>
        <NEmpty description="暂无学生数据"/>
      </template>
    </NDataTable>

    <StudentFormModal
      v-model:show="showModal"
      v-model:form-value="formValue"
      ref="formRef"
      :title="modalTitle"
      :editing="!!editingId"
      :classes="classes"
      :rules="createRules"
      @submit="handleSubmit"
    />

    <StudentImportModal
      v-model:show="showImport"
      :uploading="uploading"
      :result="importResult"
      @upload="handleUpload"
      @close="() => { showImport = false; fetchData() }"
    />

    <StudentPasswordModal
      v-model:show="showPwd"
      v-model:password="newPassword"
      :student-name="pwdStudentName"
      @submit="handleResetPwd"
    />
  </div>
</template>

<style scoped>
.page {
  max-width: 1100px;
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
