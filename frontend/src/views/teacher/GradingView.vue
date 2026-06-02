<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NTag, NSelect, NRadio, NRadioGroup, NSpace, NEmpty, NSpin, NIcon, useMessage } from 'naive-ui'
import { ArrowBackOutline, ChevronBackOutline, ChevronForwardOutline, PersonOutline } from '@vicons/ionicons5'
import http from '@/api/request'
import PageHeader from '@/components/PageHeader.vue'
import StudentProfileModal from '@/components/StudentProfileModal.vue'

const route = useRoute(); const router = useRouter(); const message = useMessage()
const taskId = Number(route.params.taskId)
const loading = ref(false)
const submissions = ref<any[]>([])
const currentIdx = ref(0)
const grades = ref<Record<string, Record<string, string>>>({})
const semesterId = ref<number | null>(null)
const profileStudentId = ref<number | null>(null)
const profileStudentName = ref('')

const dims = [
  { key: 'AWARENESS', label: '信息意识' },
  { key: 'COMPUTING', label: '计算思维' },
  { key: 'DIGITAL_LEARNING', label: '数字化学习与创新' },
  { key: 'RESPONSIBILITY', label: '信息社会责任' },
]
const gradeOpts = ['A','B','C','D','E']

const current = computed(() => submissions.value[currentIdx.value] || null)

async function loadSubmissions() {
  loading.value = true
  try {
    submissions.value = await http.get(`/tasks/${taskId}/submissions`) || []
    const task: any = await http.get(`/tasks/${taskId}`)
    if (task?.lessonId) {
      const lesson: any = await http.get(`/lessons/${task.lessonId}`)
      if (lesson?.semesterId) semesterId.value = lesson.semesterId
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openProfile(studentId: number, name: string) {
  profileStudentId.value = studentId
  profileStudentName.value = name
}

async function submitGrade() {
  const sub = current.value; if (!sub) return
  const dimGrades = dims.filter(d => grades.value[sub.id]?.[d.key]).map(d => ({ dimension: d.key, grade: grades.value[sub.id][d.key] }))
  if (!dimGrades.length) { message.warning('请至少选择一个维度评分'); return }
  try {
    await http.post(`/submissions/${sub.id}/evaluate`, { dimensions: dimGrades })
    message.success('评分成功')
    if (currentIdx.value < submissions.value.length - 1) currentIdx.value++
  } catch (e: any) { message.error(e.message || '评分失败') }
}

async function markSpecial() {
  const sub = current.value; if (!sub) return
  try { await http.post(`/submissions/${sub.id}/evaluate`, { isSpecial: true, dimensions: [] }); message.success('已标记特殊情况'); if (currentIdx.value < submissions.value.length - 1) currentIdx.value++ }
  catch (e: any) { message.error(e.message || '操作失败') }
}

function setGrade(subId: number, dim: string, grade: string) {
  if (!grades.value[subId]) grades.value[subId] = {}
  grades.value[subId][dim] = grade
}

onMounted(loadSubmissions)
</script>

<template>
  <div class="page">
    <NButton text @click="router.back()"><template #icon><NIcon><ArrowBackOutline /></NIcon></template>返回</NButton>
    <PageHeader title="批量评分" :subtitle="`${submissions.length} 份提交 · ${currentIdx + 1}/${submissions.length}`" />

    <NSpin :show="loading">
      <div v-if="current" class="grading-area">
        <div class="student-bar">
          <span class="student-name">{{ current.studentName || '学生' }}</span>
          <span class="student-no">{{ current.studentNo }}</span>
          <NTag size="small" :type="current.status === 'submitted' ? 'warning' : 'success'" :bordered="false">{{ current.status === 'submitted' ? '待评分' : current.status === 'graded' ? '已评分' : current.status }}</NTag>
          <NButton size="tiny" quaternary @click="openProfile(current.studentId, current.studentName)" style="margin-left:auto"><template #icon><NIcon :size="14"><PersonOutline /></NIcon></template>查看档案</NButton>
        </div>

        <div class="content-preview">
          <div class="content-label">提交内容</div>
          <div class="content-text">{{ current.content || '无内容' }}</div>
        </div>

        <div class="grade-panel">
          <div class="grade-label">评分维度</div>
          <div v-for="d in dims" :key="d.key" class="dim-row">
            <span class="dim-name">{{ d.label }}</span>
            <NRadioGroup :value="grades[current.id]?.[d.key] || ''" @update:value="(v: string) => setGrade(current.id, d.key, v)" size="small">
              <NSpace :size="4">
                <NRadio v-for="g in gradeOpts" :key="g" :value="g">{{ g }}</NRadio>
              </NSpace>
            </NRadioGroup>
          </div>
        </div>

        <NSpace justify="center" :size="12" class="actions">
          <NButton :disabled="currentIdx === 0" @click="currentIdx--"><template #icon><NIcon><ChevronBackOutline /></NIcon></template>上一个</NButton>
          <NButton type="primary" @click="submitGrade">提交评分</NButton>
          <NButton @click="markSpecial">特殊标记</NButton>
          <NButton :disabled="currentIdx >= submissions.length - 1" @click="currentIdx++">下一个<template #icon><NIcon><ChevronForwardOutline /></NIcon></template></NButton>
        </NSpace>
      </div>
      <NEmpty v-else description="暂无提交需要评分">
        <template #extra><NButton size="small" @click="router.back()">返回</NButton></template>
      </NEmpty>
    </NSpin>
    <StudentProfileModal :student-id="profileStudentId" :student-name="profileStudentName" :semester-id="semesterId" @close="profileStudentId = null" />
  </div>
</template>

<style scoped>
.page { max-width: 700px; margin: 0 auto; }
.grading-area { margin-top: 16px; }
.student-bar { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border: 1px solid var(--n-border-color); border-radius: 10px; margin-bottom: 16px; }
.student-name { font-size: 16px; font-weight: 600; }
.student-no { font-size: 13px; color: var(--n-text-color-3); }
.content-preview { margin-bottom: 20px; }
.content-label { font-size: 13px; color: var(--n-text-color-3); margin-bottom: 6px; }
.content-text { padding: 12px 16px; border: 1px solid var(--n-border-color); border-radius: 8px; font-size: 14px; min-height: 60px; white-space: pre-wrap; background: var(--n-color-embedded); }
.grade-panel { margin-bottom: 24px; }
.grade-label { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.dim-row { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.dim-name { width: 130px; font-size: 13px; color: var(--n-text-color-2); }
.actions { margin-top: 24px; }
</style>
