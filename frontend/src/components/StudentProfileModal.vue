<script setup lang="ts">
import { ref, watch } from 'vue'
import { NModal, NSpin, NTag, NEmpty, NSelect, NButton } from 'naive-ui'
import http from '@/api/request'
import RadarChart from '@/components/RadarChart.vue'
import { formatDate } from '@/utils/date'

const props = defineProps<{ studentId: number | null; studentName?: string; semesterId: number | null }>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(false)
const radar = ref<any>(null)
const evaluations = ref<any[]>([])
const submissions = ref<any[]>([])
const courses = ref<any[]>([])
const semesters = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(props.semesterId)

async function loadCourses() {
  try { const r: any = await http.get('/courses?page=1&size=50'); courses.value = r.records || [] } catch (e) { console.error("StudentProfileModal.vue failed", e) }
}

async function loadData() {
  const sid = props.semesterId || selectedSemesterId.value
  if (!props.studentId || !sid) return
  loading.value = true
  try {
    const [r, evals, subs] = await Promise.all([
      http.get(`/students/${props.studentId}/radar`, { params: { semesterId: sid } }),
      http.get(`/students/${props.studentId}/evaluations`, { params: { semesterId: sid } }),
      http.get(`/students/${props.studentId}/submissions`, { params: { semesterId: sid } }),
    ])
    radar.value = r; evaluations.value = evals || []; submissions.value = subs || []
  } catch { radar.value = null; evaluations.value = []; submissions.value = [] }
  finally { loading.value = false }
}

watch(selectedCourseId, async (cid) => {
  semesters.value = []; selectedSemesterId.value = null
  if (!cid) return
  try { semesters.value = await http.get(`/courses/${cid}/semesters`) || [] } catch (e) { console.error("StudentProfileModal.vue failed", e) }
})

watch([() => props.studentId, () => props.semesterId, selectedSemesterId], () => { loadData() })

watch(() => props.studentId, (sid) => {
  if (sid && !props.semesterId) loadCourses()
})

const dimLabels: Record<string, string> = {
  AWARENESS: '信息意识', COMPUTING: '计算思维',
  DIGITAL_LEARNING: '数字化学习与创新', RESPONSIBILITY: '信息社会责任',
}
function gradeColor(g: string) { const m: Record<string,string> = {A:'#4CAF50',B:'#8BC34A',C:'#FF9800',D:'#F44336',E:'#9E9E9E',F:'#000'}; return m[g] || '#999' }
</script>

<template>
  <NModal :show="studentId !== null" preset="card" :title="`${studentName || '学生'} 的学习档案`" style="width:720px;max-height:85vh" @close="$emit('close')">
    <NSpin :show="loading">
      <!-- Course+semester selector (when no fixed semesterId) -->
      <div v-if="!props.semesterId && studentId" style="display:flex;gap:8px;margin-bottom:12px">
        <NSelect v-model:value="selectedCourseId" :options="courses.map((c:any)=>({label:c.name,value:c.id}))" placeholder="选择课程" style="width:180px" />
        <NSelect v-model:value="selectedSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:180px" :disabled="!semesters.length" />
      </div>

      <template v-if="studentId && (props.semesterId || selectedSemesterId)">
        <div v-if="radar" style="margin-bottom:16px">
          <RadarChart :current="radar.current" :previous="radar.previous" :has-previous="radar.hasPrevious" />
        </div>

        <div v-if="evaluations.length" style="margin-bottom:16px">
          <h4 style="font-size:14px;font-weight:600;margin-bottom:8px">评价记录</h4>
          <div style="display:flex;gap:4px;flex-wrap:wrap">
            <NTag v-for="(e,i) in evaluations" :key="i" size="small" :bordered="false" :color="{color:gradeColor(e.grade),textColor:'#fff'}">
              {{ dimLabels[e.dimension] || e.dimension }}: {{ e.grade }}
            </NTag>
          </div>
        </div>

        <div v-if="submissions.length">
          <h4 style="font-size:14px;font-weight:600;margin-bottom:8px">提交记录</h4>
          <div v-for="s in submissions" :key="s.id" style="display:flex;align-items:center;gap:10px;padding:6px 0;font-size:13px">
            <span style="font-weight:500">任务 #{{ s.taskId }}</span>
            <NTag size="tiny" :type="s.status==='graded'?'success':s.status==='submitted'?'warning':'default'" :bordered="false">
              {{ s.status==='graded'?'已评分':s.status==='submitted'?'已提交':s.status }}
            </NTag>
            <span v-if="s.submittedAt" style="color:var(--n-text-color-3);margin-left:auto">{{ formatDate(s.submittedAt, 'date') }}</span>
          </div>
        </div>

        <NEmpty v-if="!radar && !evaluations.length && !submissions.length" description="暂无该学生的评价数据" />
      </template>
      <NEmpty v-else-if="!selectedCourseId" description="请选择课程和学期查看学生档案" />
    </NSpin>
  </NModal>
</template>
