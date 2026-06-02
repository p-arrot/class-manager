<script setup lang="ts">
import { ref, watch } from 'vue'
import { NEmpty, NButton, NDataTable, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import { formatDate } from '@/utils/date'

const props = defineProps<{ courseId: number }>()
const message = useMessage()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const exams = ref<any[]>([])
const papers = ref<any[]>([])
const showModal = ref(false); const editingId = ref<number | null>(null)
const form = ref({ name: '', paperId: null as number | null, startTime: '', endTime: '', weight: '1.0' })

async function loadSemesters() {
  try { semesters.value = await http.get(`/courses/${props.courseId}/semesters`) || [] } catch { /* ignore */ }
}
watch(() => props.courseId, () => { loadSemesters() }, { immediate: true })

async function loadExams() {
  if (!activeSemesterId.value) { exams.value = []; return }
  try { exams.value = await http.get(`/semesters/${activeSemesterId.value}/exams`) || [] } catch { /* ignore */ }
}
watch(activeSemesterId, loadExams)

async function loadPapers() { try { papers.value = await http.get('/exam-papers') || [] } catch { /* ignore */ } }
watch(() => props.courseId, loadPapers, { immediate: true })

function openCreate() { editingId.value = null; form.value = { name: '', paperId: null, startTime: '', endTime: '', weight: '1.0' }; showModal.value = true }
async function handleSubmit() {
  try {
    const body: any = { name: form.value.name, paperId: form.value.paperId, startTime: form.value.startTime, endTime: form.value.endTime, weight: parseFloat(form.value.weight) || 1.0 }
    if (editingId.value) await http.put(`/exams/${editingId.value}`, body)
    else await http.post(`/semesters/${activeSemesterId.value}/exams`, body)
    message.success(editingId.value ? '已更新' : '已创建'); showModal.value = false; loadExams()
  } catch (e: any) { message.error(e.message || '操作失败') }
}
async function handleDelete(id: number) { try { await http.delete(`/exams/${id}`); message.success('已删除'); loadExams() } catch (e: any) { message.error(e.message || '删除失败') } }
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
      <NButton v-if="activeSemesterId" size="small" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建考试</NButton>
    </div>
    <NDataTable v-if="exams.length" :data="exams" :columns="[
      {title:'名称',key:'name'},{title:'开始',key:'startTime',width:120,render:(r:any)=>formatDate(r.startTime,'datetime')},{title:'结束',key:'endTime',width:120,render:(r:any)=>formatDate(r.endTime,'datetime')},{title:'权重',key:'weight',width:70},
      {title:'操作',key:'actions',width:80,render:(r:any)=>h(NSpace,{size:2},()=>[h(NPopconfirm,{onPositiveClick:()=>handleDelete(r.id)},{trigger:()=>h(NButton,{size:'tiny',quaternary:true},()=>h(NIcon,{size:14},()=>h(TrashOutline))),default:()=>'确认删除？'})])}
    ]" size="small" :row-key="(r:any)=>r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无考试" style="padding:40px 0" />
    <NModal v-model:show="showModal" :title="editingId?'编辑考试':'创建考试'" preset="card" style="width:480px">
      <NForm label-placement="left" label-width="64">
        <NFormItem label="名称"><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="试卷"><NSelect v-model:value="form.paperId" :options="papers.map((p:any)=>({label:p.title,value:p.id}))" /></NFormItem>
        <NFormItem label="开始"><NInput v-model:value="form.startTime" placeholder="2027-01-01T09:00" /></NFormItem>
        <NFormItem label="结束"><NInput v-model:value="form.endTime" placeholder="2027-01-01T11:00" /></NFormItem>
        <NFormItem label="权重"><NInput v-model:value="form.weight" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal=false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<script lang="ts">import { h } from 'vue'</script>
