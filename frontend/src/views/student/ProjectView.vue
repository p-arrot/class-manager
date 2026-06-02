<script setup lang="ts">
import { ref, watch } from 'vue'
import { NEmpty, NButton, NSelect, NSpace, NModal, NInput, useMessage, NSpin } from 'naive-ui'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { formatDate } from '@/utils/date'

interface Project { id: number; name: string; description: string; maxTeamSize: number; deadline: string }

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const projects = ref<Project[]>([])
const showSubmit = ref(false); const activeProject = ref<Project | null>(null); const submitContent = ref('')
const showTeam = ref(false); const teamName = ref('')

watch(activeCourseId, async (cid) => { if (cid) { await loadSemesters(cid); activeSemesterId.value = semesters.value[0]?.id || null } })
watch(activeSemesterId, async (sid) => { if (sid) { try { projects.value = await http.get(`/semesters/${sid}/projects`) } catch { projects.value = [] } } })

async function handleSubmit() {
  if (!activeProject.value) return
  try { await http.post(`/projects/${activeProject.value.id}/submit`, { content: submitContent.value }); message.success('提交成功'); showSubmit.value = false }
  catch (e: any) { message.error(e.message || '提交失败') }
}
</script>

<template>
  <div class="page">
    <PageHeader title="项目化学习" subtitle="参与项目、组队并提交作品" />
    <NSpin :show="ctxLoading">
      <div class="filters">
        <NSelect v-model:value="activeCourseId" :options="courses.map((c:any)=>({label:c.name,value:c.id}))" placeholder="选择课程" style="width:200px" />
        <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
      </div>
      <div v-if="projects.length" class="list">
        <div v-for="p in projects" :key="p.id" class="card">
          <div class="info">
            <span class="name">{{ p.name }}</span>
            <span class="desc">{{ p.description }}</span>
            <span class="meta">组队上限 {{ p.maxTeamSize }} 人 · 截止 {{ p.deadline ? formatDate(p.deadline, 'date') : '未设置' }}</span>
          </div>
          <NSpace :size="8">
            <NButton size="small" @click="activeProject = p; teamName = ''; showTeam = true">组队</NButton>
            <NButton size="small" type="primary" @click="activeProject = p; submitContent = ''; showSubmit = true">提交作品</NButton>
          </NSpace>
        </div>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无项目" />
    </NSpin>
    <NModal v-model:show="showTeam" title="加入队伍" preset="card" style="width:400px">
      <NInput v-model:value="teamName" placeholder="队伍名称" />
      <template #footer><NSpace justify="end"><NButton @click="showTeam = false">取消</NButton><NButton type="primary" @click="showTeam = false; message.success('组队成功')">加入</NButton></NSpace></template>
    </NModal>
    <NModal v-model:show="showSubmit" title="提交作品" preset="card" style="width:480px">
      <NInput v-model:value="submitContent" type="textarea" placeholder="作品说明或链接" :autosize="{ minRows: 3, maxRows: 8 }" />
      <template #footer><NSpace justify="end"><NButton @click="showSubmit = false">取消</NButton><NButton type="primary" @click="handleSubmit">提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.filters { display: flex; gap: 12px; margin: 16px 0 24px; }
.list { display: flex; flex-direction: column; gap: 12px; }
.card { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 10px; }
.info { display: flex; flex-direction: column; gap: 4px; }
.name { font-size: 15px; font-weight: 600; }
.desc { font-size: 13px; color: var(--n-text-color-2); }
.meta { font-size: 11px; color: var(--n-text-color-3); }
</style>
