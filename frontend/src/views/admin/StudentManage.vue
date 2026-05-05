<script setup lang="ts">
import {ref, reactive, onMounted, h, computed} from 'vue'
import { formatDate } from '@/utils/date'
import {
  NButton,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NUpload,
  NTag,
  NSpace,
  NIcon,
  NEmpty,
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type UploadFileInfo
} from 'naive-ui'
import {AddOutline, CreateOutline, TrashOutline, RefreshOutline, CloudUploadOutline, KeyOutline} from '@vicons/ionicons5'
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
import {listAllClasses} from '@/api/classes'
import type {StudentVO, StudentImportResultVO, StudentPageQuery, ClassVO} from '@/types/api'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const records = ref<StudentVO[]>([])
const total = ref(0)
const query = reactive<StudentPageQuery>({page: 1, size: 10, classId: undefined, keyword: ''})
const classes = ref<ClassVO[]>([])
const checkedRowKeys = ref<number[]>([])

// Create/Edit modal
const showModal = ref(false)
const modalTitle = ref('新建学生')
const editingId = ref<number | null>(null)
const formRef = ref<FormInst | null>(null)
const formValue = reactive({studentNo: '', name: '', classId: null as number | null, password: ''})
const createRules: FormRules = {
  studentNo: {required: true, message: '请输入学号', trigger: 'blur'},
  name: {required: true, message: '请输入姓名', trigger: 'blur'},
  classId: {required: true, type: 'number', message: '请选择班级', trigger: 'change'},
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
  {type: 'selection'},
  {title: '学号', key: 'studentNo', width: 120},
  {title: '姓名', key: 'name', width: 100},
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
          onClick: () => openEdit(row)
        }, () => h(NIcon, {size: 15}, () => h(CreateOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          onClick: () => openResetPwd(row)
        }, () => h(NIcon, {size: 15}, () => h(KeyOutline))),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          onClick: () => handleDelete(row)
        }, () => h(NIcon, {size: 15}, () => h(TrashOutline))),
      ])
    },
  },
]

async function fetchData() {
  loading.value = true
  try {
    const r = await listStudents(query);
    records.value = r.records;
    total.value = r.total
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page;
  fetchData()
}

function handleCheck(keys: number[]) {
  checkedRowKeys.value = keys
}

async function loadClasses() {
  try {
    classes.value = await listAllClasses()
  } catch { /* ignore */
  }
}

// ---- CRUD ----
function openCreate() {
  modalTitle.value = '新建学生';
  editingId.value = null
  Object.assign(formValue, {studentNo: '', name: '', classId: null, password: ''})
  showModal.value = true
}

function openEdit(row: StudentVO) {
  modalTitle.value = '编辑学生';
  editingId.value = row.id
  Object.assign(formValue, {studentNo: row.studentNo, name: row.name, classId: row.classId, password: ''})
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
  } catch (e: any) {
    message.error(e.message || '操作失败')
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
        await deleteStudent(row.id);
        message.success('已删除');
        fetchData()
      } catch (e: any) {
        message.error(e.message || '删除失败')
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
        await batchDeleteStudents({ids: checkedRowKeys.value});
        message.success('已删除');
        checkedRowKeys.value = [];
        fetchData()
      } catch (e: any) {
        message.error(e.message || '操作失败')
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
        await batchResetPassword({ids: checkedRowKeys.value});
        message.success('已重置');
        checkedRowKeys.value = [];
        fetchData()
      } catch (e: any) {
        message.error(e.message || '操作失败')
      }
    },
  })
}

// ---- Import ----
function openImport() {
  importResult.value = null;
  showImport.value = true
}

async function handleUpload({file}: { file: UploadFileInfo; fileList: UploadFileInfo[] }) {
  if (!file.file) return
  uploading.value = true
  try {
    importResult.value = await importStudents(file.file)
  } catch (e: any) {
    message.error(e.message || '导入失败')
  } finally {
    uploading.value = false
  }
}

function openResetPwd(row: StudentVO) {
  pwdStudentId.value = row.id;
  pwdStudentName.value = row.name;
  newPassword.value = '';
  showPwd.value = true
}

async function handleResetPwd() {
  if (!pwdStudentId.value) return
  try {
    await resetPassword(pwdStudentId.value, {newPassword: newPassword.value || undefined});
    message.success('密码已重置');
    showPwd.value = false
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}


onMounted(() => {
  fetchData();
  loadClasses()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">学生管理</h2>
      <div class="head-actions">
        <NInput v-model:value="query.keyword" placeholder="搜索学号或姓名" clearable size="small" style="width:200px"
                @keyup.enter="fetchData" @clear="fetchData"/>
        <NButton size="small" @click="openCreate">
          <template #icon>
            <NIcon :size="16">
              <AddOutline/>
            </NIcon>
          </template>
          新建学生
        </NButton>
        <NButton size="small" @click="openImport">
          <template #icon>
            <NIcon :size="16">
              <CloudUploadOutline/>
            </NIcon>
          </template>
          导入 Excel
        </NButton>
      </div>
    </div>

    <div class="filter-bar">
      <NSelect v-model:value="query.classId"
               :options="[{ label: '全部班级', value: null }, ...classes.map(c => ({ label: c.grade + '级' + c.name, value: c.id }))]"
               placeholder="按班级筛选" clearable size="small" style="width:180px"
               @update:value="(v: number | null) => { query.classId = v || undefined; query.page = 1; fetchData() }"/>
      <span class="filter-total">共 {{ total }} 名学生</span>
      <NSpace v-if="checkedRowKeys.length" :size="8" style="margin-left: auto">
        <span style="font-size:13px;color:var(--n-text-color-3)">已选 {{ checkedRowKeys.length }} 项</span>
        <NButton size="tiny" @click="handleBatchResetPwd">批量重置密码</NButton>
        <NButton size="tiny" type="error" @click="handleBatchDelete">批量删除</NButton>
      </NSpace>
    </div>

    <NDataTable :columns="columns" :data="records" :loading="loading"
                :pagination="{ page: query.page, pageSize: query.size, itemCount: total }" remote
                :row-key="(r: StudentVO) => r.id" :checked-row-keys="checkedRowKeys" @update:page="handlePageChange"
                @update:page-size="(s: number) => { query.size = s; fetchData() }"
                @update:checked-row-keys="handleCheck" size="small">
      <template #empty>
        <NEmpty description="暂无学生数据"/>
      </template>
    </NDataTable>

    <!-- Create/Edit Modal -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width:420px">
      <NForm ref="formRef" :model="formValue" :rules="editingId ? undefined : createRules" label-placement="left"
             label-width="72">
        <NFormItem label="学号" path="studentNo">
          <NInput v-model:value="formValue.studentNo" placeholder="全局唯一" :disabled="!!editingId"/>
        </NFormItem>
        <NFormItem label="姓名" path="name">
          <NInput v-model:value="formValue.name" placeholder="学生姓名"/>
        </NFormItem>
        <NFormItem label="班级" path="classId">
          <NSelect v-model:value="formValue.classId"
                   :options="classes.map(c => ({ label: c.grade + '级' + c.name, value: c.id }))" placeholder="选择班级"/>
        </NFormItem>
        <NFormItem v-if="!editingId" label="密码">
          <NInput v-model:value="formValue.password" type="password" placeholder="留空默认 123456"/>
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- Import Modal -->
    <NModal v-model:show="showImport" title="导入学生" preset="card" style="width:480px">
      <NUpload accept=".xlsx,.xls" :max="1" :show-file-list="true" :default-upload="false" @change="handleUpload">
        <NButton :loading="uploading">选择 Excel 文件</NButton>
      </NUpload>
      <p style="font-size:12px;color:var(--n-text-color-3);margin-top:8px">
        表头须含：年级、班级、学号、姓名。每行独立处理。</p>
      <div v-if="importResult" class="import-result">
        <p>成功 <strong>{{ importResult.successCount }}</strong> 条，失败 <strong
            style="color:var(--n-error-color)">{{ importResult.failCount }}</strong> 条</p>
        <div v-if="importResult.errors.length" class="error-list">
          <div v-for="(e, i) in importResult.errors" :key="i" class="error-item">
            <span>第 {{ e.rowNum }} 行</span><span>{{ e.studentNo }} {{ e.name }}</span><span
              style="color:var(--n-error-color)">{{ e.errorMsg }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showImport = false; fetchData()">关闭</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- Reset Password Modal -->
    <NModal v-model:show="showPwd" title="重置密码" preset="card" style="width:380px">
      <p style="margin:0 0 16px;font-size:14px">为 <strong>{{ pwdStudentName }}</strong> 重置密码</p>
      <NInput v-model:value="newPassword" placeholder="留空则重置为默认密码 123456"/>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showPwd = false">取消</NButton>
          <NButton type="primary" @click="handleResetPwd">确认重置</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 1100px;
  animation: fadein 200ms ease;
  min-height: 100vh;
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
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
  margin: 0;
}

.head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-total {
  font-size: 13px;
  color: var(--n-text-color-3);
}

.import-result {
  margin-top: 16px;
  padding: 12px;
  background: var(--n-color-embedded);
  border-radius: 8px;
  font-size: 13px;
}

.error-list {
  max-height: 160px;
  overflow-y: auto;
  margin-top: 8px;
}

.error-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--n-border-color);
}
</style>
