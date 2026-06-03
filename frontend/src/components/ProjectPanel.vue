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
const projects = ref<any[]>([])
const showModal = ref(false)

watch(() => props.courseId, async () => { try { semesters.value = await http.get(`/courses/${props.courseId}/semesters`) || [] } catch (e) { console.error("ProjectPanel.vue failed", e) } }, { immediate: true })

async function loadProjects() {
  if (!activeSemesterId.value) { projects.value = []; return }
  try { projects.value = await http.get(`/semesters/${activeSemesterId.value}/projects`) || [] } catch (e) { console.error("ProjectPanel.vue failed", e) }
}
watch(activeSemesterId, loadProjects)

const form = ref({ name: '', description: '', maxTeamSize: '1', deadline: '', weight: '1.0' })

function openCreate() { form.value = { name: '', description: '', maxTeamSize: '1', deadline: '', weight: '1.0' }; showModal.value = true }
async function handleSubmit() {
  try {
    const body: any = { name: form.value.name, description: form.value.description, maxTeamSize: parseInt(form.value.maxTeamSize) || 1, deadline: form.value.deadline || undefined, weight: parseFloat(form.value.weight) || 1.0 }
    await http.post(`/semesters/${activeSemesterId.value}/projects`, body)
    message.success('已创建'); showModal.value = false; loadProjects()
  } catch (e: any) { message.error(e.message || '操作失败') }
}
async function handleDelete(id: number) { try { await http.delete(`/projects/${id}`); message.success('已删除'); loadProjects() } catch (e: any) { message.error(e.message || '删除失败') } }
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
      <NButton v-if="activeSemesterId" size="small" @click="openCreate"><template #icon><NIcon :size="14"><AddOutline /></NIcon></template>创建项目</NButton>
    </div>
    <NDataTable v-if="projects.length" :data="projects" :columns="[
      {title:'名称',key:'name'},{title:'说明',key:'description',ellipsis:{tooltip:true}},{title:'组队上限',key:'maxTeamSize',width:80},{title:'截止',key:'deadline',width:100,render:(r:any)=>r.deadline?formatDate(r.deadline,'date'):'-'},{title:'权重',key:'weight',width:60},
      {title:'',key:'actions',width:60,render:(r:any)=>h(NPopconfirm,{onPositiveClick:()=>handleDelete(r.id)},{trigger:()=>h(NButton,{size:'tiny',quaternary:true},()=>h(NIcon,{size:14},()=>h(TrashOutline))),default:()=>'确认删除？'})}
    ]" size="small" :row-key="(r:any)=>r.id" />
    <NEmpty v-else-if="activeSemesterId" description="暂无项目" style="padding:40px 0" />
    <NModal v-model:show="showModal" title="创建项目" preset="card" style="width:480px">
      <NForm label-placement="left" label-width="64">
        <NFormItem label="名称"><NInput v-model:value="form.name" /></NFormItem>
        <NFormItem label="说明"><NInput v-model:value="form.description" type="textarea" :autosize="{minRows:2}" /></NFormItem>
        <NFormItem label="组队上限"><NInput v-model:value="form.maxTeamSize" /></NFormItem>
        <NFormItem label="截止"><NInput v-model:value="form.deadline" placeholder="2027-06-30T23:59" /></NFormItem>
        <NFormItem label="权重"><NInput v-model:value="form.weight" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal=false">取消</NButton><NButton type="primary" @click="handleSubmit">确定</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<script lang="ts">import { h } from 'vue'</script>
