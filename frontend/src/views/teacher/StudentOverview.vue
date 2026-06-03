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
const showDrivePreview = ref(false)
const drivePreviewUrl = ref('')
const drivePreviewName = ref('')

async function loadStudents() {
  loading.value = true
  try {
    const r: any = await http.get('/students', { params: { page: page.value, size: pageSize.value, keyword: searchKeyword.value || undefined } })
    students.value = r.records || []
    total.value = r.total || 0
  } catch (e) { console.error('加载学生列表失败', e) }
  finally { loading.value = false }
}

const driveParentId = ref<number | null>(null)
const driveBreadcrumb = ref<any[]>([])

async function openDrive(studentId: number) {
  driveStudentId.value = studentId; showDrive.value = true
  driveParentId.value = null; driveBreadcrumb.value = []
  await loadDriveItems()
}
async function loadDriveItems() {
  if (!driveStudentId.value) return
  driveLoading.value = true
  try { driveItems.value = await http.get('/drive/tree', { params: { userId: driveStudentId.value, parentId: driveParentId.value || undefined } }) }
  catch (e) { console.error('加载学生网盘失败', e); driveItems.value = [] }
  finally { driveLoading.value = false }
}
function enterDriveFolder(item: any) { driveBreadcrumb.value.push(item); driveParentId.value = item.id; loadDriveItems() }
function goDriveBack(idx: number) { driveBreadcrumb.value = driveBreadcrumb.value.slice(0, idx); driveParentId.value = idx > 0 ? driveBreadcrumb.value[idx-1].id : null; loadDriveItems() }

async function handleDriveDelete(itemId: number) {
  try { await http.delete(`/drive/${itemId}`); message.success('已删除'); await openDrive(driveStudentId.value!) }
  catch (e: any) { message.error('删除失败') }
}

async function handleDriveDownload(item: any) {
  try {
    const blob = await http.get(`/drive/${item.id}/raw`, { responseType: 'blob' }) as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = item.name; a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) { message.error('下载失败') }
}
async function handleDrivePreview(item: any) {
  try {
    const r: any = await http.get(`/drive/${item.id}/preview`)
    if (r?.url) {
      drivePreviewUrl.value = r.url
      drivePreviewName.value = item.name
      showDrivePreview.value = true
    }
  } catch (e: any) { message.error('预览失败') }
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
    <NModal v-model:show="showDrive" preset="card" title="学生网盘管理" style="width:640px;max-height:80vh">
      <NSpin :show="driveLoading">
        <div v-if="driveBreadcrumb.length" style="margin-bottom:8px;display:flex;align-items:center;gap:2px;font-size:12px">
          <NButton text size="tiny" @click="goDriveBack(0)">根目录</NButton>
          <template v-for="(b,i) in driveBreadcrumb" :key="b.id">
            <span style="color:var(--n-text-color-3)">/</span>
            <NButton text size="tiny" @click="goDriveBack(i+1)">{{ b.name }}</NButton>
          </template>
        </div>
        <div style="font-size:13px;color:var(--n-text-color-3);margin-bottom:12px">总占用: {{ getTotalDriveSize() }}</div>
        <div v-if="driveItems.length" style="display:flex;flex-direction:column;gap:4px">
          <div v-for="item in driveItems" :key="item.id" style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px;border-radius:4px;border:1px solid var(--n-border-color)" :style="{cursor:item.type==='FOLDER'?'pointer':'default'}" @click="item.type==='FOLDER'?enterDriveFolder(item):undefined">
            <NIcon :size="16" :color="item.type==='FOLDER'?'#F97316':'#6b6b65'"><FolderOutline v-if="item.type==='FOLDER'" /><DocumentOutline v-else /></NIcon>
            <span style="flex:1;font-size:13px;font-weight:500;margin-left:8px">{{ item.name }}</span>
            <span style="font-size:11px;color:var(--n-text-color-3)">{{ item.fileSize ? (item.fileSize > 1024 ? (item.fileSize/1024).toFixed(0)+'KB' : item.fileSize+'B') : '' }}</span>
            <NButton v-if="item.type==='FILE'" size="tiny" quaternary @click.stop="handleDriveDownload(item)" title="下载"><template #icon><NIcon :size="14"><CloudDownloadOutline /></NIcon></template></NButton>
            <NButton v-if="item.type==='FILE'" size="tiny" quaternary @click.stop="handleDrivePreview(item)" title="预览"><template #icon><NIcon :size="14"><EyeOutline /></NIcon></template></NButton>
            <NPopconfirm @positive-click="()=>handleDriveDelete(item.id)"><template #trigger><NButton size="tiny" quaternary type="error" @click.stop><template #icon><NIcon :size="14"><TrashOutline /></NIcon></template></NButton></template>确认删除？</NPopconfirm>
          </div>
        </div>
        <NEmpty v-else description="网盘为空" />
      </NSpin>
    </NModal>

    <!-- Drive Preview Modal (kkFileView iframe) -->
    <NModal v-model:show="showDrivePreview" :title="drivePreviewName" preset="card" style="width:90vw;max-width:900px;height:80vh">
      <iframe v-if="drivePreviewUrl" :src="drivePreviewUrl" style="width:100%;height:calc(80vh - 60px);border:none;border-radius:0 0 8px 8px" />
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
</style>
