<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NButton, NTag, NSpace, NModal, NInput, useMessage } from 'naive-ui'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'

interface Project { id: number; name: string; description: string; maxTeamSize: number; deadline: string }

const message = useMessage()
const projects = ref<Project[]>([])
const showSubmit = ref(false)
const activeProject = ref<Project | null>(null)
const submitContent = ref('')
const showTeam = ref(false)
const teamName = ref('')

async function loadProjects() {
  try {
    const courses: any[] = await http.get('/courses?page=1&size=50')
    if (!courses.length) return
    const semesters: any[] = await http.get(`/courses/${courses[0].id}/semesters`)
    if (!semesters.length) return
    projects.value = await http.get(`/semesters/${semesters[0].id}/projects`)
  } catch { /* ignore */ }
}

function openSubmit(p: Project) { activeProject.value = p; submitContent.value = ''; showSubmit.value = true }
function openTeam(p: Project) { activeProject.value = p; teamName.value = ''; showTeam.value = true }

async function handleSubmit() {
  if (!activeProject.value) return
  try {
    await http.post(`/projects/${activeProject.value.id}/submit`, { content: submitContent.value })
    message.success('提交成功'); showSubmit.value = false
  } catch (e: any) { message.error(e.message || '提交失败') }
}

async function handleJoinTeam() {
  if (!activeProject.value) return
  try {
    await http.post(`/projects/${activeProject.value.id}/teams/join`, { name: teamName.value })
    message.success('组队成功'); showTeam.value = false
  } catch (e: any) { message.error(e.message || '组队失败') }
}

onMounted(loadProjects)
</script>

<template>
  <div class="page">
    <PageHeader title="项目化学习" subtitle="参与项目、组队并提交作品" />
    <div v-if="projects.length" class="list">
      <div v-for="p in projects" :key="p.id" class="card">
        <div class="info">
          <span class="name">{{ p.name }}</span>
          <span class="desc">{{ p.description }}</span>
          <span class="meta">组队上限 {{ p.maxTeamSize }} 人 · 截止 {{ p.deadline ? formatDate(p.deadline, 'date') : '未设置' }}</span>
        </div>
        <NSpace :size="8">
          <NButton size="small" @click="openTeam(p)">组队</NButton>
          <NButton size="small" type="primary" @click="openSubmit(p)">提交作品</NButton>
        </NSpace>
      </div>
    </div>
    <NEmpty v-else description="暂无项目" />

    <NModal v-model:show="showTeam" title="加入队伍" preset="card" style="width:400px">
      <p v-if="activeProject" style="margin-bottom:12px;font-size:13px;color:var(--n-text-color-2)">项目：{{ activeProject.name }}（最多 {{ activeProject.maxTeamSize }} 人）</p>
      <NInput v-model:value="teamName" placeholder="队伍名称" />
      <template #footer><NSpace justify="end"><NButton @click="showTeam = false">取消</NButton><NButton type="primary" @click="handleJoinTeam">加入/创建队伍</NButton></NSpace></template>
    </NModal>

    <NModal v-model:show="showSubmit" title="提交作品" preset="card" style="width:480px">
      <NInput v-model:value="submitContent" type="textarea" placeholder="作品说明或链接" :autosize="{ minRows: 3, maxRows: 8 }" />
      <template #footer><NSpace justify="end"><NButton @click="showSubmit = false">取消</NButton><NButton type="primary" @click="handleSubmit">提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.list { display: flex; flex-direction: column; gap: 12px; margin-top: 24px; }
.card { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 10px; }
.info { display: flex; flex-direction: column; gap: 4px; }
.name { font-size: 15px; font-weight: 600; }
.desc { font-size: 13px; color: var(--n-text-color-2); }
.meta { font-size: 11px; color: var(--n-text-color-3); }
</style>
