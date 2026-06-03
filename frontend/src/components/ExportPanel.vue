<script setup lang="ts">
import { ref, watch } from 'vue'
import { NEmpty, NButton, NSelect, NTag, NCard, useMessage } from 'naive-ui'
import http from '@/api/request'

const props = defineProps<{ courseId: number }>()
const message = useMessage()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const preview = ref<any[]>([])

watch(() => props.courseId, async () => { try { semesters.value = await http.get(`/courses/${props.courseId}/semesters`) || [] } catch (e) { console.error("ExportPanel.vue failed", e) } }, { immediate: true })
watch(activeSemesterId, async () => {
  if (!activeSemesterId.value) return
  try { preview.value = await http.get(`/stats/semester/${activeSemesterId.value}/preview`) || [] } catch { preview.value = [] }
})

async function handleExport() {
  if (!activeSemesterId.value) return
  try {
    const r: any = await http.get(`/stats/semester/${activeSemesterId.value}/export`, { responseType: 'blob' })
    const url = URL.createObjectURL(r); const a = document.createElement('a'); a.href = url; a.download = '学期总评.xlsx'; a.click()
    URL.revokeObjectURL(url); message.success('导出成功')
  } catch (e: any) { message.error(e.message || '导出失败') }
}

function gradeLabel(s: number): string { if (s >= 90) return 'A'; if (s >= 75) return 'B'; if (s >= 60) return 'C'; if (s >= 40) return 'D'; return 'E' }
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
      <NButton v-if="activeSemesterId" size="small" type="primary" @click="handleExport">导出 Excel</NButton>
    </div>
    <div v-if="preview.length" style="display:flex;flex-direction:column;gap:8px">
      <NCard v-for="row in preview" :key="row.studentId" size="small" style="padding:12px 16px">
        <div style="display:flex;align-items:center;gap:12px">
          <span style="font-size:14px;font-weight:600">{{ row.studentName }}</span>
          <span style="font-size:12px;color:var(--n-text-color-3)">{{ row.studentNo }}</span>
          <NTag size="tiny" :type="row.totalScore >= 60 ? 'success' : 'error'" :bordered="false">{{ gradeLabel(row.totalScore || 0) }}</NTag>
          <span style="font-size:14px;font-weight:600;margin-left:auto">{{ row.totalScore?.toFixed(1) || '-' }} 分</span>
        </div>
      </NCard>
    </div>
    <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" style="padding:40px 0" />
  </div>
</template>
