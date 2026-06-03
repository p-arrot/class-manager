<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NButton, NSelect, NEmpty, NTag, NCard, useMessage } from 'naive-ui'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { formatDate } from '@/utils/date'

const message = useMessage()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const preview = ref<any[]>([])
const loading = ref(false)

async function loadSemesters() {
  try { const cs: any[] = await http.get('/courses?page=1&size=50'); if(cs.length) semesters.value = await http.get(`/courses/${cs[0].id}/semesters`) } catch (e) { console.error("GradeExport.vue failed", e) }
}

async function loadPreview() {
  if (!activeSemesterId.value) return
  loading.value = true
  try { preview.value = await http.get('/stats/semester/' + activeSemesterId.value + '/preview') } catch { preview.value = [] }
  finally { loading.value = false }
}

async function handleExport() {
  if (!activeSemesterId.value) return
  try {
    const r: any = await http.get('/stats/semester/' + activeSemesterId.value + '/export', { responseType: 'blob' })
    const url = URL.createObjectURL(r)
    const a = document.createElement('a'); a.href = url; a.download = '学期总评.xlsx'; a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) { message.error(e.message || '导出失败') }
}

function gradeLabel(s: number): string { if (s >= 90) return 'A'; if (s >= 75) return 'B'; if (s >= 60) return 'C'; if (s >= 40) return 'D'; return 'E' }

onMounted(loadSemesters)
</script>

<template>
  <div class="page">
    <PageHeader title="成绩导出" subtitle="导出学期总评 Excel">
      <template #actions>
        <NButton size="small" type="primary" :disabled="!activeSemesterId" @click="handleExport">导出 Excel</NButton>
      </template>
    </PageHeader>
    <div class="toolbar">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" @update:value="loadPreview" />
    </div>
    <NEmpty v-if="!activeSemesterId" description="请选择一个学期" />
    <div v-else-if="preview.length" class="preview-list">
      <NCard v-for="row in preview" :key="row.studentId" size="small" class="preview-card">
        <div class="row-info">
          <span class="row-name">{{ row.studentName }}</span>
          <span class="row-no">{{ row.studentNo }}</span>
          <NTag size="tiny" :type="row.totalScore >= 60 ? 'success' : 'error'" :bordered="false">{{ gradeLabel(row.totalScore) }}</NTag>
          <span class="row-score">{{ row.totalScore?.toFixed(1) ?? '-' }} 分</span>
        </div>
      </NCard>
    </div>
    <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" />
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.toolbar { margin: 16px 0 24px; }
.preview-list { display: flex; flex-direction: column; gap: 8px; }
.preview-card { padding: 12px 16px; }
.row-info { display: flex; align-items: center; gap: 12px; }
.row-name { font-size: 14px; font-weight: 600; }
.row-no { font-size: 12px; color: var(--n-text-color-3); }
.row-score { font-size: 14px; font-weight: 600; margin-left: auto; }
</style>
