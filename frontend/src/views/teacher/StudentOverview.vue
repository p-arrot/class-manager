<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NDataTable, NButton, NIcon, NInput, NTag, NModal, NCard, NSpin, NEmpty, NSpace, NPopconfirm, useMessage } from 'naive-ui'
import { SearchOutline, EyeOutline, TrashOutline, FolderOutline, DocumentOutline, CloudDownloadOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import StudentProfileModal from '@/components/StudentProfileModal.vue'
import { formatDate } from '@/utils/date'

const message = useMessage()
const loading = ref(false)
const students = ref<any[]>([])
const searchKeyword = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const showProfile = ref(false)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')
const showDrive = ref(false)
const driveStudentId = ref<number | null>(null)
const driveItems = ref<any[]>([])
const driveLoading = ref(false)

async function loadStudents() {
  loading.value = true
  try {
    const r: any = await http.get('/students', { params: { page: page.value, size: pageSize.value, keyword: searchKeyword.value || undefined } })
    students.value = r.records || []
    total.value = r.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function openDrive(studentId: number) {
  driveStudentId.value = studentId; showDrive.value = true
  driveLoading.value = true
  try { driveItems.value = await http.get('/drive/tree', { params: { userId: studentId } }) }
  catch { driveItems.value = [] }
  finally { driveLoading.value = false }
}

async function handleDriveDelete(itemId: number) {
  try { await http.delete(`/drive/${itemId}`); message.success('已删除'); await openDrive(driveStudentId.value!) }
  catch (e: any) { message.error('删除失败') }
}

function handleDriveDownload(item: any) {
  window.open(`/api/drive/${item.id}/raw`, '_blank')
}
function handleDrivePreview(item: any) {
  http.get(`/drive/${item.id}/preview`).then((r: any) => {
    if (r?.url) window.open(r.url.replace('minio:9000', 'localhost:9000'), '_blank')
  })
}

function getTotalDriveSize(): string {
  const bytes = driveItems.value.reduce((sum: number, i: any) => sum + (i.fileSize || 0), 0)
  if (bytes > 1024*1024*1024) return (bytes/(1024*1024*1024)).toFixed(1) + ' GB'
  if (bytes > 1024*1024) return (bytes/(1024*1024)).toFixed(1) + ' MB'
  if (bytes > 1024) return (bytes/1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

onMounted(loadStudents)
</script>

<template>
  <div class="page">
    <PageHeader title="学生管理" subtitle="查看所有学生信息" />
    <div style="display:flex;gap:12px;margin-bottom:16px">
      <NInput v-model:value="searchKeyword" placeholder="搜索学号/姓名" style="width:200px" clearable @keyup.enter="loadStudents" @clear="loadStudents" />
      <NButton @click="loadStudents"><template #icon><NIcon :size="16"><SearchOutline /></NIcon></template>搜索</NButton>
    </div>
    <NDataTable :loading="loading" :data="students" size="small" :pagination="{ page, pageSize, itemCount: total, prefix: () => `共 ${total} 条` }" :row-key="(r:any)=>r.id"
      @update:page="(p:number)=>{page=p;loadStudents()}" @update:page-size="(s:number)=>{pageSize=s;loadStudents()}" remote
      :columns="[
        {title:'学号',key:'studentNo',width:100},{title:'姓名',key:'studentName',width:100},
        {title:'班级',key:'className',width:120,render:(r:any)=>r.className||'-'},
        {title:'创建时间',key:'createdAt',width:120,render:(r:any)=>formatDate(r.createdAt,'date')},
        {title:'操作',key:'actions',width:180,render:(r:any)=>h(NSpace,{size:4},()=>[
          h(NButton,{size:'tiny',quaternary:true,onClick:()=>{profileStudentId=r.id;profileStudentName=r.studentName;showProfile=true}},()=>[h(NIcon,{size:14},()=>h(EyeOutline)),' 档案']),
          h(NButton,{size:'tiny',quaternary:true,onClick:()=>openDrive(r.id)},()=>[h(NIcon,{size:14},()=>h(FolderOutline)),' 网盘']),
        ])}
      ]"
    />

    <!-- Student Profile Modal -->
    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="null" @close="profileStudentId = null" />

    <!-- Drive Manager Modal -->
    <NModal v-model:show="showDrive" preset="card" title="学生网盘管理" style="width:600px;max-height:80vh">
      <NSpin :show="driveLoading">
        <div style="font-size:13px;color:var(--n-text-color-3);margin-bottom:12px">总占用: {{ getTotalDriveSize() }}</div>
        <div v-if="driveItems.length" style="display:flex;flex-direction:column;gap:4px">
          <div v-for="item in driveItems" :key="item.id" style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px;border-radius:4px;border:1px solid var(--n-border-color)">
            <NIcon :size="16" :color="item.type==='FOLDER'?'#F97316':'#6b6b65'"><FolderOutline v-if="item.type==='FOLDER'" /><DocumentOutline v-else /></NIcon>
            <span style="flex:1;font-size:13px;font-weight:500">{{ item.name }}</span>
            <span style="font-size:11px;color:var(--n-text-color-3)">{{ item.fileSize ? (item.fileSize > 1024 ? (item.fileSize/1024).toFixed(0)+'KB' : item.fileSize+'B') : '' }}</span>
            <NButton v-if="item.type==='FILE'" size="tiny" quaternary @click="handleDriveDownload(item)" title="下载"><template #icon><NIcon :size="14"><CloudDownloadOutline /></NIcon></template></NButton>
            <NButton v-if="item.type==='FILE'" size="tiny" quaternary @click="handleDrivePreview(item)" title="预览"><template #icon><NIcon :size="14"><EyeOutline /></NIcon></template></NButton>
            <NPopconfirm @positive-click="()=>handleDriveDelete(item.id)"><template #trigger><NButton size="tiny" quaternary type="error"><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton></template>确认删除？</NPopconfirm>
          </div>
        </div>
        <NEmpty v-else description="网盘为空" />
      </NSpin>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
</style>
