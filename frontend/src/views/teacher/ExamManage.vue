<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NButton, NDataTable, NTag, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'

interface Exam { id: number; name: string; startTime: string; endTime: string; weight: number; semesterId: number }
interface Paper { id: number; title: string }

const message = useMessage()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const exams = ref<Exam[]>([])
const papers = ref<Paper[]>([])
const loading = ref(false)
const showModal = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', paperId: null as number | null, startTime: '', endTime: '', weight: 1.0 })

async function loadSemesters() {
  try { const cs: any[] = await http.get('/courses?page=1&size=50'); if (cs.length) semesters.value = await http.get(`/courses/${cs[0].id}/semesters`) }
  catch (e) { console.error("ExamManage.vue failed", e) }
}
async function loadExams() {
  if (!activeSemesterId.value) return
  loading.value = true
  try { exams.value = await http.get(`/semesters/${activeSemesterId.value}/exams`) } catch (e) { console.error("ExamManage.vue failed", e) }
  finally { loading.value = false }
}
async function loadPapers() { try { papers.value = await http.get('/exam-papers') } catch (e) { console.error("ExamManage.vue failed", e) } }

function openCreate() {
  editingId.value = null; form.value = { name: '', paperId: null, startTime: '', endTime: '', weight: 1.0 }; showModal.value = true
}

async function handleSubmit() {
  try {
    const dto = { name: form.value.name, paperId: form.value.paperId, startTime: form.value.startTime, endTime: form.value.endTime, weight: form.value.weight }
    if (editingId.value) await http.put(`/exams/${editingId.value}`, dto)
    else await http.post(`/semesters/${activeSemesterId.value}/exams`, dto)
    message.success(editingId.value ? '已更新' : '已创建'); showModal.value = false; await loadExams()
  } catch (e: any) { message.error(e.message || '操作失败') }
}

async function handleDelete(id: number) {
  try { await http.delete(`/exams/${id}`); message.success('已删除'); await loadExams() } catch(e:any){message.error(e.message||'删除失败')}
}

onMounted(async () => { await loadSemesters(); await loadPapers() })
</script>

<template>
  <div class="page">
    <PageHeader title="考试管理" subtitle="创建和管理学期考试">
      <template #actions>
        <NButton size="small" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建考试</NButton>
      </template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" @update:value="loadExams" />
    </div>
    <NDataTable v-if="exams.length" :data="exams" :columns="[
      {title:'考试名称',key:'name'},
      {title:'开始时间',key:'startTime',render:(r:Exam)=>formatDate(r.startTime,'datetime')},
      {title:'结束时间',key:'endTime',render:(r:Exam)=>formatDate(r.endTime,'datetime')},
      {title:'权重',key:'weight',width:80},
      {title:'操作',key:'actions',width:100,render:(r:Exam)=>h(NSpace,{size:2},()=>[h(NButton,{size:'tiny',quaternary:true,onClick:()=>{editingId=r.id;form={name:r.name,paperId:r.id,startTime:r.startTime,endTime:r.endTime,weight:r.weight};showModal=true}},()=>h(NIcon,{size:14},()=>h(CreateOutline))),h(NPopconfirm,{onPositiveClick:()=>handleDelete(r.id)},{trigger:()=>h(NButton,{size:'tiny',quaternary:true},()=>h(NIcon,{size:14},()=>h(TrashOutline))),default:()=>'确认删除？'})])}
    ]" size="small" :row-key="(r:Exam)=>r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    <NModal v-model:show="showModal" :title="editingId?'编辑考试':'创建考试'" preset="card" style="width:480px">
      <NForm label-placement="left" label-width="72">
        <NFormItem label="名称" required><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="试卷"><NSelect v-model:value="form.paperId" :options="papers.map(p=>({label:p.title,value:p.id}))" /></NFormItem>
        <NFormItem label="开始时间"><NInput v-model:value="form.startTime" placeholder="2027-01-01T09:00:00" /></NFormItem>
        <NFormItem label="结束时间"><NInput v-model:value="form.endTime" placeholder="2027-01-01T11:00:00" /></NFormItem>
        <NFormItem label="权重"><NInput v-model:value="form.weight" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal=false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<script lang="ts">
import { h } from 'vue'
</script>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.toolbar { margin: 16px 0; }
</style>
