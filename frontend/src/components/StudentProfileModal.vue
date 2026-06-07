<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NModal, NSpin, NTag, NEmpty, NSelect, useMessage } from 'naive-ui'
import { listCourses } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import { getStudentRadar, listStudentEvaluations, listStudentSubmissions } from '@/api/students'
import RadarChart from '@/components/RadarChart.vue'
import { formatDate } from '@/utils/date'
import { getErrorMessage } from '@/utils/error'
import type { CourseVO, EvaluationVO, RadarVO, SemesterVO, SubmissionVO } from '@/types/api'

const props = defineProps<{ studentId: number | null; studentName?: string; semesterId: number | null }>()
const emit = defineEmits<{ close: [] }>()
const message = useMessage()

const loading = ref(false)
const radar = ref<RadarVO | null>(null)
const evaluations = ref<EvaluationVO[]>([])
const submissions = ref<SubmissionVO[]>([])
const courses = ref<CourseVO[]>([])
const semesters = ref<SemesterVO[]>([])
const selectedCourseId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(props.semesterId)

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
  } catch (e) {
    courses.value = []
    message.error(getErrorMessage(e, '加载课程列表失败'))
  }
}

async function loadData() {
  const sid = props.semesterId || selectedSemesterId.value
  if (!props.studentId || !sid) return
  loading.value = true
  try {
    const [r, evals, subs] = await Promise.all([
      getStudentRadar(props.studentId, sid),
      listStudentEvaluations(props.studentId, sid),
      listStudentSubmissions(props.studentId, sid),
    ])
    radar.value = r
    evaluations.value = evals || []
    submissions.value = subs || []
  } catch (e) {
    radar.value = null
    evaluations.value = []
    submissions.value = []
    message.error(getErrorMessage(e, '加载学生档案失败'))
  } finally {
    loading.value = false
  }
}

watch(selectedCourseId, async (cid) => {
  semesters.value = []
  selectedSemesterId.value = null
  if (!cid) return
  try {
    semesters.value = await listSemesters(cid) || []
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
})

watch([() => props.studentId, () => props.semesterId, selectedSemesterId], () => {
  loadData()
})

watch(() => props.studentId, (sid) => {
  if (sid && !props.semesterId) loadCourses()
})

const dimLabels: Record<string, string> = {
  AWARENESS: '信息意识',
  COMPUTING: '计算思维',
  DIGITAL_LEARNING: '数字化学习与创新',
  RESPONSIBILITY: '信息社会责任',
}

const gradeColors: Record<string, string> = {
  A: '#4CAF50',
  B: '#8BC34A',
  C: '#FF9800',
  D: '#F44336',
  E: '#9E9E9E',
  F: '#000',
}

function gradeColor(grade: string) {
  return gradeColors[grade] || '#999'
}

function submissionStatusText(status: string) {
  if (status === 'graded') return '已评分'
  if (status === 'submitted') return '已提交'
  return status
}

function submissionStatusType(status: string) {
  if (status === 'graded') return 'success'
  if (status === 'submitted') return 'warning'
  return 'default'
}
</script>

<template>
  <NModal :show="studentId !== null" preset="card" :title="`${studentName || '学生'} 的学习档案`" class="profile-modal" @close="emit('close')">
    <NSpin :show="loading">
      <div v-if="!props.semesterId && studentId" class="selector-row">
        <NSelect v-model:value="selectedCourseId" :options="courseOptions" placeholder="选择课程" class="selector" />
        <NSelect v-model:value="selectedSemesterId" :options="semesterOptions" placeholder="选择学期" class="selector" :disabled="!semesters.length" />
      </div>

      <template v-if="studentId && (props.semesterId || selectedSemesterId)">
        <div v-if="radar" class="section">
          <RadarChart :current="radar.current" :previous="radar.previous" :has-previous="radar.hasPrevious" />
        </div>

        <div v-if="evaluations.length" class="section">
          <h4 class="section-title">评价记录</h4>
          <div class="tag-list">
            <NTag v-for="(e,i) in evaluations" :key="i" size="small" :bordered="false" :color="{color:gradeColor(e.grade),textColor:'#fff'}">
              {{ dimLabels[e.dimension] || e.dimension }}: {{ e.grade }}
            </NTag>
          </div>
        </div>

        <div v-if="submissions.length">
          <h4 class="section-title">提交记录</h4>
          <div v-for="submission in submissions" :key="submission.id" class="submission-row">
            <span class="submission-title">任务 #{{ submission.taskId }}</span>
            <NTag size="tiny" :type="submissionStatusType(submission.status)" :bordered="false">
              {{ submissionStatusText(submission.status) }}
            </NTag>
            <span v-if="submission.submittedAt" class="submission-date">{{ formatDate(submission.submittedAt, 'date') }}</span>
          </div>
        </div>

        <NEmpty v-if="!radar && !evaluations.length && !submissions.length" description="暂无该学生的评价数据" />
      </template>
      <NEmpty v-else-if="!selectedCourseId" description="请选择课程和学期查看学生档案" />
    </NSpin>
  </NModal>
</template>

<style scoped>
.profile-modal {
  width: min(720px, calc(100vw - 32px));
  max-height: 85vh;
}
.selector-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.selector {
  width: 180px;
}
.section {
  margin-bottom: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.tag-list {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.submission-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
}
.submission-title {
  font-weight: 500;
}
.submission-date {
  color: var(--n-text-color-3);
  margin-left: auto;
}
@media (max-width: 640px) {
  .selector {
    width: 100%;
  }
  .submission-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
  .submission-date {
    margin-left: 0;
  }
}
</style>
