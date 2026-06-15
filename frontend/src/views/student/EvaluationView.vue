<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowForwardOutline } from '@vicons/ionicons5'
import { NButton, NEmpty, NIcon, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { listCourses } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import { getStudentRadar, listStudentEvaluations, listStudentSubmissions } from '@/api/students'
import PageHeader from '@/components/PageHeader.vue'
import RadarChart from '@/components/RadarChart.vue'
import { getErrorMessage } from '@/utils/error'
import { formatDate } from '@/utils/date'
import type { CourseVO, EvaluationVO, RadarVO, SemesterVO, SubmissionVO } from '@/types/api'

const auth = useAuthStore()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const courses = ref<CourseVO[]>([])
const semesters = ref<SemesterVO[]>([])
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const radar = ref<RadarVO | null>(null)
const evaluations = ref<EvaluationVO[]>([])
const submissions = ref<SubmissionVO[]>([])
const courseOptions = computed(() => courses.value.map(course => ({
  label: course.name,
  value: course.id,
})))
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

async function loadCourses() {
  try {
    const r = await listCourses({ page: 1, size: 50 })
    courses.value = r.records || []
    if (!activeCourseId.value && courses.value.length) {
      activeCourseId.value = courses.value[0].id
    }
  } catch (e) {
    courses.value = []
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
}

function pickCurrentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(s => new Date(s.startTime).getTime() <= now && now <= new Date(s.endTime).getTime())
}

watch(activeCourseId, async (cid) => {
  semesters.value = []
  activeSemesterId.value = null
  if (!cid) return
  try {
    semesters.value = await listSemesters(cid) || []
    activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
})

async function loadRadar() {
  if (!activeSemesterId.value || !auth.userInfo?.userId) {
    radar.value = null
    evaluations.value = []
    submissions.value = []
    return
  }
  loading.value = true
  try {
    const [radarData, evaluationData, submissionData] = await Promise.all([
      getStudentRadar(auth.userInfo.userId, activeSemesterId.value),
      listStudentEvaluations(auth.userInfo.userId, activeSemesterId.value),
      listStudentSubmissions(auth.userInfo.userId, activeSemesterId.value),
    ])
    radar.value = radarData
    evaluations.value = evaluationData || []
    submissions.value = submissionData || []
  } catch (e) {
    radar.value = null
    evaluations.value = []
    submissions.value = []
    message.error(getErrorMessage(e, '加载学习评价失败'))
  } finally {
    loading.value = false
  }
}
watch(activeSemesterId, loadRadar)

const dimLabels: Record<string, string> = {
  AWARENESS: '信息意识',
  COMPUTING: '计算思维',
  DIGITAL_LEARNING: '数字化学习与创新',
  RESPONSIBILITY: '信息社会责任',
}

const hasRadarData = computed(() => {
  return !!radar.value?.current?.some(item => item.avgScore > 0)
})

const visibleSubmissions = computed(() => {
  return submissions.value
    .filter(item => ['submitted', 'graded', 'special'].includes(item.status))
    .sort((a, b) => {
      const at = a.submittedAt ? new Date(a.submittedAt).getTime() : 0
      const bt = b.submittedAt ? new Date(b.submittedAt).getTime() : 0
      return bt - at
    })
})

function taskEvaluations(taskId: number) {
  return evaluations.value.filter(item => item.taskId === taskId)
}

function taskTitle(submission: SubmissionVO) {
  return submission.taskTitle || `任务 #${submission.taskId}`
}

function statusText(status: string) {
  if (status === 'graded') return '已批改'
  if (status === 'submitted') return '待批改'
  if (status === 'special') return '特殊处理'
  return status
}

function statusType(status: string) {
  if (status === 'graded') return 'success'
  if (status === 'submitted') return 'warning'
  if (status === 'special') return 'error'
  return 'default'
}

function actionText(status: string) {
  return status === 'submitted' ? '查看提交状态' : '查看批改详情'
}

function goTaskResult(taskId: number) {
  router.push(`/student/tasks/${taskId}/result`)
}

onMounted(loadCourses)
</script>

<template>
  <div class="page">
    <PageHeader title="学习评价" subtitle="查看四维度能力雷达图与任务批改明细" />
    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" :disabled="!semesters.length" />
    </div>
    <NSpin :show="loading">
      <div v-if="hasRadarData && radar" class="radar-section">
        <RadarChart :current="radar.current" :previous="radar.previous" :has-previous="radar.hasPrevious" />
        <div class="dim-legend">
          <NTag v-for="d in radar.current" :key="d.dimension" size="small" :bordered="false">
            {{ dimLabels[d.dimension] || d.dimension }}: {{ d.avgScore.toFixed(0) }}
          </NTag>
        </div>
      </div>

      <section v-if="visibleSubmissions.length" class="result-section" aria-label="任务批改明细">
        <div class="section-head">
          <h3>任务批改明细</h3>
          <span>{{ visibleSubmissions.length }} 条记录</span>
        </div>
        <div class="task-list">
          <div v-for="submission in visibleSubmissions" :key="submission.id" class="task-row">
            <div class="task-main">
              <div class="task-title">{{ taskTitle(submission) }}</div>
              <div class="task-meta">
                <NTag size="small" :type="statusType(submission.status)" :bordered="false">
                  {{ statusText(submission.status) }}
                </NTag>
                <span v-if="submission.submittedAt">提交于 {{ formatDate(submission.submittedAt, 'datetime') }}</span>
              </div>
              <div v-if="taskEvaluations(submission.taskId).length" class="score-tags">
                <NTag v-for="item in taskEvaluations(submission.taskId)" :key="`${submission.id}-${item.dimension}`" size="small" :bordered="false">
                  {{ item.label || dimLabels[item.dimension] || item.dimension }} {{ item.grade }}
                </NTag>
              </div>
            </div>
            <NButton secondary type="primary" class="result-action" @click="goTaskResult(submission.taskId)">
              <template #icon><NIcon><ArrowForwardOutline /></NIcon></template>
              {{ actionText(submission.status) }}
            </NButton>
          </div>
        </div>
      </section>

      <NEmpty v-if="!loading && activeSemesterId && !hasRadarData && !visibleSubmissions.length" description="暂无评价数据" />
      <NEmpty v-else-if="!loading && !activeSemesterId" description="选择课程和学期查看评价" />
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.toolbar-select { width: 200px; }
.radar-section { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; }
.dim-legend { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; justify-content: center; }
.result-section {
  border-top: 1px solid var(--n-border-color);
  padding-top: 18px;
}
.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section-head h3 {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  margin: 0;
}
.section-head span {
  color: var(--n-text-color-3);
  font-size: 13px;
}
.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-row {
  align-items: center;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  display: flex;
  gap: 14px;
  justify-content: space-between;
  padding: 14px 16px;
}
.task-main {
  min-width: 0;
}
.task-title {
  color: var(--n-text-color-1);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}
.task-meta,
.score-tags {
  align-items: center;
  color: var(--n-text-color-3);
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.result-action {
  min-height: 36px;
  flex-shrink: 0;
}
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
  .task-row {
    align-items: stretch;
    flex-direction: column;
  }
  .result-action {
    width: 100%;
  }
}
</style>
