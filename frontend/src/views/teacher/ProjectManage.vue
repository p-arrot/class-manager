<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NButton, NDataTable, NTag, NModal, NForm, NFormItem, NInput, NSelect, NSpace, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'

interface Project { id: number; name: string; description: string; maxTeamSize: number; deadline: string; weight: number; semesterId: number }

const message = useMessage()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const projects = ref<Project[]>([])
const showModal = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', description: '', maxTeamSize: 1, deadline: '', weight: 1.0 })

async function loadSemesters() {
  try { const cs: any[] = await http.get('/courses?page=1&size=50'); if (cs.length) semesters.value = await http.get(`/courses/${cs[0].id}/semesters`) } catch (e) { console.error("ProjectManage.vue failed", e) }
}
async function loadProjects() {
  if (!activeSemesterId.value) return
  try { projects.value = await http.get(`/semesters/${activeSemesterId.value}/projects`) } catch (e) { console.error("ProjectManage.vue failed", e) }
}
function openCreate() { editingId.value = null; form.value = { name: '', description: '', maxTeamSize: 1, deadline: '', weight: 1.0 }; showModal.value = true }
async function handleSubmit() {
  try {
    if (editingId.value) await http.put(`/projects/${editingId.value}`, form.value)
    else await http.post(`/semesters/${activeSemesterId.value}/projects`, form.value)
    message.success(editingId.value ? '已更新' : '已创建'); showModal.value = false; await loadProjects()
  } catch (e: any) { message.error(e.message || '操作失败') }
}
async function handleDelete(id: number) {
  try { await http.delete(`/projects/${id}`); message.success('已删除'); await loadProjects() } catch(e:any){message.error(e.message||'删除失败')}
}
onMounted(loadSemesters)
</script>

<template>
  <div class="page">
    <PageHeader title="项目管理" subtitle="创建和管理项目化学习任务">
      <template #actions><NButton size="small" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建项目</NButton></template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" @update:value="loadProjects" />
    </div>
    <NDataTable v-if="projects.length" :data="projects" :columns="[
      {title:'名称',key:'name'},{title:'说明',key:'description',ellipsis:{tooltip:true}},
      {title:'组队上限',key:'maxTeamSize',width:90},
      {title:'截止',key:'deadline',width:120,render:(r:Project)=>r.deadline?formatDate(r.deadline,'date'):'-'},
      {title:'权重',key:'weight',width:70},
      {title:'操作',key:'actions',width:100,render:(r:Project)=>h(NSpace,{size:2},()=>[
        h(NButton,{size:'tiny',quaternary:true,onClick:()=>{editingId=r.id;form={name:r.name,description:r.description||'',maxTeamSize:r.maxTeamSize,deadline:r.deadline||'',weight:r.weight};showModal=true}},()=>h(NIcon,{size:14},()=>h(CreateOutline))),
        h(NPopconfirm,{onPositiveClick:()=>handleDelete(r.id)},{trigger:()=>h(NButton,{size:'tiny',quaternary:true},()=>h(NIcon,{size:14},()=>h(TrashOutline))),default:()=>'确认删除？'})
      ])}
    ]" size="small" :row-key="(r:Project)=>r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无项目" />
    <NModal v-model:show="showModal" :title="editingId?'编辑项目':'创建项目'" preset="card" style="width:480px">
      <NForm label-placement="left" label-width="72">
        <NFormItem label="名称" required><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="说明"><NInput v-model:value="form.description" type="textarea" :autosize="{minRows:2,maxRows:4}" /></NFormItem>
        <NFormItem label="组队上限"><NInput v-model:value="form.maxTeamSize" /></NFormItem>
        <NFormItem label="截止时间"><NInput v-model:value="form.deadline" placeholder="2027-06-30T23:59:59" /></NFormItem>
        <NFormItem label="权重"><NInput v-model:value="form.weight" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal=false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<script lang="ts">import { h } from 'vue'</script>
<style scoped>.page{max-width:900px;margin:0 auto}.toolbar{margin:16px 0}</style>
