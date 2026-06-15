<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { formatDate } from '@/utils/date'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NTag, NTabs, NTabPane, NSpace, NIcon, NSelect, NEmpty, useMessage } from 'naive-ui'
import { ArrowBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { getCourse } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import { listLessons } from '@/api/lessons'
import { listAllClasses } from '@/api/classes'
import { listTasks } from '@/api/tasks'
import CourseResourcePanel from '@/components/CourseResourcePanel.vue'
import LessonTaskPanel from '@/components/LessonTaskPanel.vue'
import { getErrorMessage } from '@/utils/error'
import type { CourseDetailVO, SemesterVO, LessonVO, ClassVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)
const semesters = ref<SemesterVO[]>([])
const lessons = ref<LessonVO[]>([])
const activeTab = ref('semesters')
const activeSemesterId = ref<number | null>(null)
const allClasses = ref<ClassVO[]>([])
const classMap = computed(() => new Map(allClasses.value.map(c => [c.id, c])))
const semesterOptions = computed(() => semesters.value.map(semester => ({
  label: semester.name,
  value: semester.id,
})))

const expandedLessonId = ref<number | null>(null)
function toggleLesson(id: number) {
  expandedLessonId.value = expandedLessonId.value === id ? null : id
}

async function loadCourse() {
  try {
    course.value = await getCourse(courseId)
  } catch (e) {
    message.error(getErrorMessage(e, '加载失败'))
    router.push('/student/home')
  }
}

async function loadSemesters() {
  try {
    semesters.value = await listSemesters(courseId)
  } catch (e) {
    semesters.value = []
    message.error(getErrorMessage(e, '加载学期列表失败'))
  }
}

const lessonTaskCounts = ref<Record<number, number>>({})

async function loadLessons() {
  if (!activeSemesterId.value) {
    lessons.value = []
    return
  }
  try {
    lessons.value = await listLessons(activeSemesterId.value)
  } catch (e) {
    lessons.value = []
    message.error(getErrorMessage(e, '加载课时列表失败'))
  }
  const countEntries = await Promise.all(lessons.value.map(async (l) => {
    try {
      const tasks = await listTasks(l.id)
      return [l.id, (tasks || []).length] as const
    } catch {
      return [l.id, 0] as const
    }
  }))
  lessonTaskCounts.value = Object.fromEntries(countEntries)
}

function goBack() {
  router.push('/student/home')
}

function openLessonTab(semesterId: number) {
  activeSemesterId.value = semesterId
  activeTab.value = 'lessons'
}

function pickCurrentSemester(list: SemesterVO[]) {
  const now = Date.now()
  return list.find(s => new Date(s.startTime).getTime() <= now && now <= new Date(s.endTime).getTime())
}

watch(semesters, (val) => {
  if (!val.length) {
    activeSemesterId.value = null
    return
  }
  activeSemesterId.value = pickCurrentSemester(val)?.id ?? val[0].id
})

watch(activeSemesterId, () => { if (activeSemesterId.value) loadLessons() })

onMounted(async () => {
  await loadCourse()
  await loadSemesters()
  try {
    allClasses.value = await listAllClasses()
  } catch (e) {
    allClasses.value = []
    message.error(getErrorMessage(e, '加载班级信息失败'))
  }
})
</script>

<template>
  <div class="page">
    <div class="back-bar">
      <NButton text size="small" @click="goBack">
        <template #icon><NIcon :size="16"><ArrowBackOutline /></NIcon></template>返回课程列表
      </NButton>
    </div>

    <div v-if="course" class="course-header">
      <h2 class="course-name">{{ course.name }}</h2>
      <p v-if="course.description" class="course-desc">{{ course.description }}</p>
      <div class="course-meta">
        <span class="meta-label">授课教师：</span>
        <span class="meta-value">{{ course.teacherName }}</span>
        <span class="meta-sep">|</span>
        <NSpace :size="4">
          <NTag v-for="(id, i) in course.classIds" :key="i" size="tiny" :bordered="false">
            {{ classMap.get(id)?.grade }}级{{ classMap.get(id)?.name || id }}
          </NTag>
          <span v-if="!course.classIds?.length" class="unbound-class">未绑定班级</span>
        </NSpace>
      </div>
    </div>

    <NTabs type="line" animated v-model:value="activeTab">
      <NTabPane name="semesters" tab="学期">
        <div v-if="semesters.length" class="sem-list">
          <div v-for="s in semesters" :key="s.id" class="sem-card" @click="openLessonTab(s.id)">
            <div class="sem-left">
              <div class="sem-year">{{ s.startTime?.split('-')[0] || '' }}</div>
              <div class="sem-range">{{ formatDate(s.startTime,'date')?.slice(5) }} - {{ formatDate(s.endTime,'date')?.slice(5) }}</div>
            </div>
            <div class="sem-body">
              <span class="sem-title">{{ s.name }}</span>
              <span class="sem-count">{{ s.lessonCount }} 课时</span>
            </div>
            <NIcon :size="16" color="var(--n-text-color-3)"><ChevronForwardOutline /></NIcon>
          </div>
        </div>
        <NEmpty v-else description="暂无学期" class="empty-hint" />
      </NTabPane>

      <NTabPane name="lessons" tab="课时">
        <div class="tab-head">
          <NSelect v-if="semesters.length" v-model:value="activeSemesterId" :options="semesterOptions" size="small" class="semester-select" />
        </div>
        <div v-if="activeSemesterId && lessons.length" class="lesson-list">
          <div v-for="row in lessons" :key="row.id">
            <div class="lesson-row" @click="toggleLesson(row.id)">
              <NIcon :size="14" class="lesson-chevron" :class="{ expanded: expandedLessonId === row.id }"><ChevronForwardOutline /></NIcon>
              <span class="lesson-index">{{ row.sortOrder }}</span>
              <span class="lesson-name">{{ row.name }}</span>
              <span v-if="lessonTaskCounts[row.id]" class="lesson-task-count">{{ lessonTaskCounts[row.id] }}个任务</span>
            </div>
            <div v-if="expandedLessonId === row.id" class="lesson-expand">
              <LessonTaskPanel :lesson-id="row.id" readonly />
            </div>
          </div>
        </div>
        <NEmpty v-else-if="activeSemesterId" description="该学期暂无课时" />
        <NEmpty v-else description="请先选择一个学期查看课时" />
      </NTabPane>

      <NTabPane name="resources" tab="课程资源">
        <CourseResourcePanel :course-id="courseId" readonly />
      </NTabPane>
    </NTabs>
  </div>
</template>

<style scoped>
.page { max-width: 900px; animation: fadein 200ms ease; }
@keyframes fadein { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
.back-bar { margin-bottom: 12px; }
.course-header { margin-bottom: 24px; }
.course-name { font-size: 22px; font-weight: 600; margin: 0 0 6px; }
.course-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0 0 10px; line-height: 1.5; }
.course-meta { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--n-text-color-2); flex-wrap: wrap; }
.meta-label { color: var(--n-text-color-3); }
.meta-sep { color: var(--n-border-color); margin: 0 4px; }
.unbound-class { font-size: 12px; color: var(--n-text-color-3); }
.tab-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-subtitle { font-size: 13px; color: var(--n-text-color-3); }
.semester-select { width: 220px; }
.sem-list { display: flex; flex-direction: column; gap: 8px; }
.sem-card { display: flex; align-items: center; gap: 16px; padding: 14px 18px; border: 1px solid var(--n-border-color); border-radius: 10px; cursor: pointer; transition: border-color 0.15s; }
.sem-card:hover { border-color: var(--n-primary-color-hover); }
.sem-left { width: 48px; height: 48px; border-radius: 10px; background: var(--n-color-embedded); display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; }
.sem-year { font-size: 13px; font-weight: 700; color: var(--n-text-color); line-height: 1.2; }
.sem-range { font-size: 10px; color: var(--n-text-color-3); line-height: 1.2; }
.sem-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.sem-title { font-size: 15px; font-weight: 600; }
.sem-count { font-size: 12px; color: var(--n-text-color-3); }
.empty-hint { padding: 40px 0; }
.resource-tab { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 40px 0; }
.resource-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0; }

.lesson-list { display: flex; flex-direction: column; gap: 0; }
.lesson-row { display: flex; align-items: center; gap: 8px; padding: 10px 12px; cursor: pointer; border-radius: 6px; transition: background 0.15s; }
.lesson-row:hover { background: var(--n-color-embedded); transform: translateX(2px); }
.lesson-chevron { transition: transform 0.15s; }
.lesson-chevron.expanded { transform: rotate(90deg); }
.lesson-index { width: 24px; font-size: 13px; color: var(--n-text-color-3); text-align: center; }
.lesson-name { font-size: 14px; font-weight: 500; flex: 1; }
.lesson-task-count { font-size: 11px; color: #7C3AED; background: rgba(124,58,237,0.08); padding: 1px 8px; border-radius: 10px; }
.lesson-expand { padding: 0 12px 12px 36px; animation: slideDown 0.15s ease; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
</style>
