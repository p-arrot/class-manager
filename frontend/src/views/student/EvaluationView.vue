<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { NEmpty, NSelect, NSpin, NTag, useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { listCourses } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import { getStudentRadar } from '@/api/students'
import PageHeader from '@/components/PageHeader.vue'
import RadarChart from '@/components/RadarChart.vue'
import { getErrorMessage } from '@/utils/error'
import type { CourseVO, RadarVO, SemesterVO } from '@/types/api'

const auth = useAuthStore()
const message = useMessage()
const loading = ref(false)
const courses = ref<CourseVO[]>([])
const semesters = ref<SemesterVO[]>([])
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const radar = ref<RadarVO | null>(null)
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
    return
  }
  loading.value = true
  try {
    radar.value = await getStudentRadar(auth.userInfo.userId, activeSemesterId.value)
  } catch (e) {
    radar.value = null
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

onMounted(loadCourses)
</script>

<template>
  <div class="page">
    <PageHeader title="学习评价" subtitle="查看四维度能力雷达图" />
    <div class="toolbar">
      <NSelect v-model:value="activeCourseId" :options="courseOptions" placeholder="选择课程" class="toolbar-select" />
      <NSelect v-model:value="activeSemesterId" :options="semesterOptions" placeholder="选择学期" class="toolbar-select" :disabled="!semesters.length" />
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
      <NEmpty v-else-if="!loading && activeSemesterId" description="暂无评价数据" />
      <NEmpty v-else-if="!loading" description="选择课程和学期查看雷达图" />
    </NSpin>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.toolbar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.toolbar-select { width: 200px; }
.radar-section { display: flex; flex-direction: column; align-items: center; }
.dim-legend { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; justify-content: center; }
@media (max-width: 640px) {
  .toolbar-select { width: 100%; }
}
</style>
