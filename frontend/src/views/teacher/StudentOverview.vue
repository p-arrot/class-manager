<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NDataTable, NButton, NIcon, NInput, NSpace, useMessage } from 'naive-ui'
import { SearchOutline, EyeOutline, FolderOutline } from '@vicons/ionicons5'
import { listStudents } from '@/api/students'
import { deleteDriveItem, getDrivePreview, getDriveRaw, listDriveItems } from '@/api/drive'
import PageHeader from '@/components/PageHeader.vue'
import StudentDriveModal from '@/components/teacher/StudentDriveModal.vue'
import StudentProfileModal from '@/components/StudentProfileModal.vue'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { DataTableColumns } from 'naive-ui'
import type { DriveItemVO, StudentVO } from '@/types/api'

interface StudentOverviewRow extends StudentVO {
  studentName?: string
}

const message = useMessage()
const loading = ref(false)
const students = ref<StudentOverviewRow[]>([])
const searchKeyword = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const showProfile = ref(false)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')
const showDrive = ref(false)
const driveStudentId = ref<number | null>(null)
const driveItems = ref<DriveItemVO[]>([])
const driveLoading = ref(false)
const showDrivePreview = ref(false)
const drivePreviewUrl = ref('')
const drivePreviewName = ref('')

async function loadStudents() {
  loading.value = true
  try {
    const r = await listStudents({ page: page.value, size: pageSize.value, keyword: searchKeyword.value || undefined })
    students.value = r.records || []
    total.value = r.total || 0
  } catch (e) {
    students.value = []
    total.value = 0
    message.error(getErrorMessage(e, '加载学生列表失败'))
  } finally {
    loading.value = false
  }
}

const driveParentId = ref<number | null>(null)
const driveBreadcrumb = ref<DriveItemVO[]>([])

async function openDrive(studentId: number) {
  driveStudentId.value = studentId
  showDrive.value = true
  driveParentId.value = null
  driveBreadcrumb.value = []
  await loadDriveItems()
}

async function loadDriveItems() {
  if (!driveStudentId.value) return
  driveLoading.value = true
  try {
    driveItems.value = await listDriveItems({ userId: driveStudentId.value, parentId: driveParentId.value || undefined })
  } catch (e) {
    driveItems.value = []
    message.error(getErrorMessage(e, '加载学生网盘失败'))
  } finally {
    driveLoading.value = false
  }
}

function enterDriveFolder(item: DriveItemVO) {
  driveBreadcrumb.value.push(item)
  driveParentId.value = item.id
  loadDriveItems()
}

function goDriveBack(idx: number) {
  driveBreadcrumb.value = driveBreadcrumb.value.slice(0, idx)
  driveParentId.value = idx > 0 ? driveBreadcrumb.value[idx - 1].id : null
  loadDriveItems()
}

async function handleDriveDelete(itemId: number) {
  if (!driveStudentId.value) return
  try {
    await deleteDriveItem(itemId)
    message.success('已删除')
    await openDrive(driveStudentId.value)
  } catch (e) {
    message.error(getErrorMessage(e, '删除失败'))
  }
}

async function handleDriveDownload(item: DriveItemVO) {
  try {
    const blob = await getDriveRaw(item.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = item.name
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
}

async function handleDrivePreview(item: DriveItemVO) {
  try {
    const r = await getDrivePreview(item.id)
    if (r?.url) {
      drivePreviewUrl.value = r.url
      drivePreviewName.value = item.name
      showDrivePreview.value = true
    }
  } catch (e) {
    message.error(getErrorMessage(e, '预览失败'))
  }
}

const studentColumns: DataTableColumns<StudentOverviewRow> = [
  { title: '学号', key: 'studentNo', width: 100 },
  { title: '姓名', key: 'studentName', width: 100 },
  { title: '班级', key: 'className', width: 120, render: row => row.className || '-' },
  { title: '创建时间', key: 'createdAt', width: 120, render: row => formatDate(row.createdAt, 'date') },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row => h(NSpace, { size: 4 }, () => [
      h(NButton, {
        size: 'tiny',
        quaternary: true,
        title: '查看学习档案',
        'aria-label': '查看学习档案',
        onClick: () => {
          profileStudentId.value = row.id
          profileStudentName.value = row.studentName || row.name
          showProfile.value = true
        },
      }, () => [h(NIcon, { size: 14 }, () => h(EyeOutline)), ' 档案']),
      h(NButton, {
        size: 'tiny',
        quaternary: true,
        title: '查看学生网盘',
        'aria-label': '查看学生网盘',
        onClick: () => openDrive(row.id),
      }, () => [
        h(NIcon, { size: 14 }, () => h(FolderOutline)),
        ' 网盘',
      ]),
    ]),
  },
]

onMounted(loadStudents)
</script>

<template>
  <div class="page">
    <PageHeader title="学生管理" subtitle="查看所有学生信息" />
    <div class="toolbar">
      <NInput v-model:value="searchKeyword" placeholder="搜索学号/姓名" class="search-input" clearable @keyup.enter="loadStudents" @clear="loadStudents" />
      <NButton @click="loadStudents">
        <template #icon><NIcon :size="16"><SearchOutline /></NIcon></template>
        搜索
      </NButton>
    </div>
    <NDataTable
      :loading="loading"
      :data="students"
      size="small"
      :pagination="{ page, pageSize, itemCount: total, prefix: () => `共 ${total} 条` }"
      :row-key="(row: StudentOverviewRow) => row.id"
      remote
      :columns="studentColumns"
      @update:page="value => { page = value; loadStudents() }"
      @update:page-size="value => { pageSize = value; loadStudents() }"
    />

    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="null" @close="profileStudentId = null" />
    <StudentDriveModal
      v-model:show="showDrive"
      v-model:show-preview="showDrivePreview"
      :loading="driveLoading"
      :items="driveItems"
      :breadcrumb="driveBreadcrumb"
      :preview-url="drivePreviewUrl"
      :preview-name="drivePreviewName"
      @back="goDriveBack"
      @enter-folder="enterDriveFolder"
      @download="handleDriveDownload"
      @preview="handleDrivePreview"
      @delete="handleDriveDelete"
    />
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.search-input {
  width: 200px;
}
@media (max-width: 640px) {
  .search-input,
  .toolbar :deep(.n-button) {
    width: 100%;
  }
}
</style>
