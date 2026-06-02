<script setup lang="ts">
import { ref, watch } from 'vue'
import { NSelect, NDataTable, NTag, NEmpty, NSpin } from 'naive-ui'
import http from '@/api/request'
import { formatDate } from '@/utils/date'

const props = defineProps<{ courseId: number }>()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const rows = ref<any[]>([])
const loading = ref(false)

watch(() => props.courseId, async () => { try { semesters.value = await http.get(`/courses/${props.courseId}/semesters`) || [] } catch { /* */ } }, { immediate: true })
watch(activeSemesterId, async (sid) => {
  if (!sid) return
  loading.value = true
  try { rows.value = await http.get(`/stats/semester/${sid}/preview`) || [] } catch { rows.value = [] }
  finally { loading.value = false }
})

function gradeLabel(s: number): string { if (s >= 90) return 'A'; if (s >= 75) return 'B'; if (s >= 60) return 'C'; if (s >= 40) return 'D'; return 'E' }
function gradeColor(s: number): string { if (s >= 90) return '#4CAF50'; if (s >= 75) return '#8BC34A'; if (s >= 60) return '#FF9800'; if (s >= 40) return '#F44336'; return '#9E9E9E' }
</script>

<template>
  <div>
    <div style="margin-bottom:16px">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
    </div>
    <NSpin :show="loading">
      <NDataTable v-if="rows.length" :data="rows" size="small" :columns="[
        {title:'学号',key:'studentNo',width:100},{title:'姓名',key:'studentName',width:100},
        {title:'过程分',key:'processScore',width:80,render:(r:any)=>r.processScore!=null?r.processScore.toFixed(1):'-'},
        {title:'考试',key:'examScore',width:70,render:(r:any)=>r.examScore!=null?r.examScore.toFixed(1):'-'},
        {title:'项目',key:'projectScore',width:70,render:(r:any)=>r.projectScore!=null?r.projectScore.toFixed(1):'-'},
        {title:'总评',key:'totalScore',width:90,render:(r:any)=>r.totalScore!=null?r.totalScore.toFixed(1):r.totalGrade||'-'},
        {title:'等级',key:'totalGrade',width:70,render:(r:any)=>r.totalGrade!=='暂无数据'?h(NTag,{size:'tiny',color:{color:gradeColor(r.totalScore||0),textColor:'#fff'},bordered:false},()=>r.totalGrade):h('span',{style:'color:var(--n-text-color-3)'},'-')},
        {title:'备注',key:'remark',ellipsis:{tooltip:true},width:120},
      ]" :row-key="(r:any)=>r.studentNo" />
      <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" style="padding:40px 0" />
    </NSpin>
  </div>
</template>

<script lang="ts">import { h } from 'vue'</script>
