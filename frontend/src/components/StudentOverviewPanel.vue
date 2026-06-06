<script setup lang="ts">
import { ref, watch } from 'vue'
import { NSelect, NDataTable, NTag, NEmpty, NSpin, NButton, NIcon, useMessage } from 'naive-ui'
import { PersonOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import StudentProfileModal from '@/components/StudentProfileModal.vue'

const props = defineProps<{ courseId: number }>()
const semesters = ref<any[]>([])
const activeSemesterId = ref<number | null>(null)
const rows = ref<any[]>([])
const loading = ref(false)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')

const message = useMessage()

watch(() => props.courseId, async () => { try { semesters.value = await http.get(`/courses/${props.courseId}/semesters`) || [] } catch (e) { message.error('加载学期列表失败'); console.error("StudentOverviewPanel.vue failed", e) } }, { immediate: true })
watch(activeSemesterId, async (sid) => {
  if (!sid) return
  loading.value = true
  try { rows.value = await http.get(`/stats/semester/${sid}/preview`) || [] } catch { rows.value = [] }
  finally { loading.value = false }
})

function openProfile(row: any) {
  profileStudentId.value = row.studentId
  profileStudentName.value = row.studentName
}
</script>

<template>
  <div>
    <div style="margin-bottom:16px">
      <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
    </div>
    <NSpin :show="loading">
      <NDataTable v-if="rows.length" :data="rows" size="small" :columns="[
        {title:'学号',key:'studentNo',width:100},
        {title:'姓名',key:'studentName',width:100},
        {title:'过程分',key:'processScore',width:80,render:(r:any)=>r.processScore!=null?r.processScore.toFixed(1):'-'},
        {title:'考试',key:'examScore',width:70,render:(r:any)=>r.examScore!=null?r.examScore.toFixed(1):'-'},
        {title:'项目',key:'projectScore',width:70,render:(r:any)=>r.projectScore!=null?r.projectScore.toFixed(1):'-'},
        {title:'总评',key:'totalScore',width:90,render:(r:any)=>r.totalScore!=null?r.totalScore.toFixed(1):'-'},
        {title:'等级',key:'totalGrade',width:70,render:(r:any)=>r.totalGrade&&r.totalGrade!=='暂无数据'?h(NTag,{size:'tiny',color:{color:r.totalScore>=60?'#4CAF50':'#F44336',textColor:'#fff'},bordered:false},()=>r.totalGrade):h('span',{},'-')},
        {title:'',key:'actions',width:70,render:(r:any)=>h(NButton,{size:'tiny',quaternary:true,onClick:()=>openProfile(r)},()=>[h(NIcon,{size:14},()=>h(PersonOutline)),' 雷达'])}
      ]" :row-key="(r:any)=>r.studentNo" />
      <NEmpty v-else-if="activeSemesterId" description="暂无评价数据" style="padding:40px 0" />
    </NSpin>
    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="activeSemesterId" @close="profileStudentId = null" />
  </div>
</template>

<script lang="ts">import { h } from 'vue'</script>
