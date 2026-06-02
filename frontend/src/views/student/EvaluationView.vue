<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NEmpty, NSelect, NSpin, NTag } from 'naive-ui'
import http from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/PageHeader.vue'
import RadarChart from '@/components/RadarChart.vue'

const auth = useAuthStore()
const loading = ref(false)
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const radar = ref<any>(null)

async function loadSemesters() {
  try { const r: any = await http.get('/courses?page=1&size=50'); semesters.value = r.records || [] }
  catch { /* ignore */ }
}

async function loadRadar() {
  if (!activeSemesterId.value) return
  loading.value = true
  try {
    radar.value = await http.get(`/students/${auth.userInfo?.userId}/radar`, { params: { semesterId: activeSemesterId.value } })
  } catch { radar.value = null }
  finally { loading.value = false }
}

const dimLabels: Record<string, string> = {
  AWARENESS: '信息意识', COMPUTING: '计算思维',
  DIGITAL_LEARNING: '数字化学习与创新', RESPONSIBILITY: '信息社会责任',
}

onMounted(loadSemesters)
</script>

<template>
  <div class="page">
    <PageHeader title="学习评价" subtitle="查看四维度能力雷达图" />
    <div class="toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any) => ({ label: s.name, value: s.id }))" placeholder="选择学期" style="width:220px" @update:value="loadRadar" />
    </div>
    <NSpin :show="loading">
      <div v-if="radar" class="radar-section">
        <RadarChart :current="radar.current" :previous="radar.previous" :has-previous="radar.hasPrevious" />
        <div class="dim-legend">
          <NTag v-for="d in radar.current" :key="d.dimension" size="small" :bordered="false">
            {{ dimLabels[d.dimension] || d.dimension }}: {{ d.avgScore.toFixed(0) }}
          </NTag>
        </div>
      </div>
      <NEmpty v-else-if="!loading" description="请选择学期查看评价数据" />
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.toolbar { margin-bottom: 20px; }
.radar-section { display: flex; flex-direction: column; align-items: center; }
.dim-legend { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; justify-content: center; }
</style>
