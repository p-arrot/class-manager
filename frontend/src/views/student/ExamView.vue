<script setup lang="ts">
import { ref, watch } from 'vue'
import { NEmpty, NButton, NSelect, NModal, NSpace, NInput, NSpin, useMessage } from 'naive-ui'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import { useStudentContext } from '@/composables/useStudentContext'
import { formatDate } from '@/utils/date'

interface Exam { id: number; name: string; startTime: string; endTime: string; weight: number }

const { courses, semesters, loading: ctxLoading, loadSemesters } = useStudentContext()
const message = useMessage()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const exams = ref<Exam[]>([])
const loading = ref(false)
const showExam = ref(false)
const activeExam = ref<Exam | null>(null)
const answers = ref('')

watch(activeCourseId, async (cid) => { if (cid) { await loadSemesters(cid); activeSemesterId.value = semesters.value[0]?.id || null } })
watch(activeSemesterId, async (sid) => { if (sid) { loading.value = true; try { exams.value = await http.get(`/semesters/${sid}/exams`) } catch { exams.value = [] } finally { loading.value = false } } })

function startExam(exam: Exam) { activeExam.value = exam; answers.value = ''; showExam.value = true }

async function submitExam() {
  if (!activeExam.value) return
  try {
    await http.post(`/exams/${activeExam.value.id}/submit`, { answers: answers.value })
    message.success('提交成功')
    showExam.value = false
    if (activeSemesterId.value) {
      loading.value = true
      try { exams.value = await http.get(`/semesters/${activeSemesterId.value}/exams`) } catch { exams.value = [] } finally { loading.value = false }
    }
  } catch (e: any) { message.error(e.message || '提交失败') }
}
</script>

<template>
  <div class="page">
    <PageHeader title="考试" subtitle="参加学期考试" />
    <NSpin :show="ctxLoading">
      <div class="filters">
        <NSelect v-model:value="activeCourseId" :options="courses.map((c:any)=>({label:c.name,value:c.id}))" placeholder="选择课程" style="width:200px" />
        <NSelect v-model:value="activeSemesterId" :options="semesters.map((s:any)=>({label:s.name,value:s.id}))" placeholder="选择学期" style="width:200px" />
      </div>
      <div v-if="exams.length" class="exam-list">
        <div v-for="e in exams" :key="e.id" class="exam-row">
          <div class="exam-info">
            <span class="exam-name">{{ e.name }}</span>
            <span class="exam-time">{{ formatDate(e.startTime, 'datetime') }} - {{ formatDate(e.endTime, 'datetime') }}</span>
          </div>
          <NButton size="small" @click="startExam(e)">进入考试</NButton>
        </div>
      </div>
      <NEmpty v-else-if="activeSemesterId" description="暂无考试" />
    </NSpin>
    <NModal v-model:show="showExam" :title="activeExam?.name" preset="card" style="width:640px">
      <NInput v-model:value="answers" type="textarea" placeholder="请输入答案" :autosize="{ minRows: 8, maxRows: 20 }" />
      <template #footer><NSpace justify="end"><NButton @click="showExam = false">取消</NButton><NButton type="primary" @click="submitExam">提交</NButton></NSpace></template>
    </NModal>
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.filters { display: flex; gap: 12px; margin: 16px 0 24px; }
.exam-list { display: flex; flex-direction: column; gap: 12px; }
.exam-row { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border: 1px solid var(--n-border-color); border-radius: 10px; }
.exam-info { display: flex; flex-direction: column; gap: 4px; }
.exam-name { font-size: 15px; font-weight: 600; }
.exam-time { font-size: 12px; color: var(--n-text-color-3); }
</style>
