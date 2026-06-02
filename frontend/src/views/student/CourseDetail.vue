<script setup lang="ts">
import { ref, onMounted, computed, watch, h } from 'vue'
import { formatDate } from '@/utils/date'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NDataTable, NTag, NTabs, NTabPane, NSpace, NIcon, NSelect, NEmpty, useMessage } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { getCourse } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import { listLessons } from '@/api/lessons'
import { listAllClasses } from '@/api/classes'
import CourseResourcePanel from '@/components/CourseResourcePanel.vue'
import LessonTaskPanel from '@/components/LessonTaskPanel.vue'
import type { CourseDetailVO, SemesterVO, LessonVO, ClassVO } from '@/types/api'
import type { DataTableColumns } from 'naive-ui'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const courseId = Number(route.params.courseId)
const course = ref<CourseDetailVO | null>(null)
const semesters = ref<SemesterVO[]>([])
const lessons = ref<LessonVO[]>([])
const activeSemesterId = ref<number | null>(null)
const allClasses = ref<ClassVO[]>([])
const classMap = computed(() => new Map(allClasses.value.map(c => [c.id, c])))

const selectedLessonId = ref<number | null>(null)
const lesColumns: DataTableColumns<LessonVO> = [
  { title: '序号', key: 'sortOrder', width: 60 },
  { title: '课时名称', key: 'name', render(row: LessonVO) { return h('span', { style: 'cursor:pointer;font-weight:' + (selectedLessonId.value === row.id ? '600' : '400'), onClick: () => { selectedLessonId.value = selectedLessonId.value === row.id ? null : row.id } }, row.name) } },
]

async function loadCourse() {
  try {
    course.value = await getCourse(courseId)
  } catch (e: any) {
    message.error(e.message || '加载失败')
    router.push('/student/home')
  }
}

async function loadSemesters() {
  try { semesters.value = await listSemesters(courseId) } catch { /* ignore */ }
}

async function loadLessons() {
  if (!activeSemesterId.value) { lessons.value = []; return }
  try { lessons.value = await listLessons(activeSemesterId.value) } catch { /* ignore */ }
}

function goBack() { router.push('/student/home') }

watch(semesters, (val) => {
  if (val.length && !activeSemesterId.value) activeSemesterId.value = val[0].id
  if (!val.length) activeSemesterId.value = null
})

watch(activeSemesterId, () => { if (activeSemesterId.value) loadLessons() })

onMounted(async () => {
  await loadCourse()
  await loadSemesters()
  try { allClasses.value = await listAllClasses() } catch { /* ignore */ }
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
          <span v-if="!course.classIds?.length" style="font-size:12px;color:var(--n-text-color-3)">未绑定班级</span>
        </NSpace>
      </div>
    </div>

    <NTabs type="line" animated>
      <NTabPane name="semesters" tab="学期">
        <div class="tab-head">
          <span class="tab-subtitle">{{ semesters.length }} 个学期</span>
        </div>
        <div v-if="semesters.length" class="sem-list">
          <NCard v-for="s in semesters" :key="s.id" size="small" class="sem-card">
            <div class="sem-info">
              <h4 class="sem-name">{{ s.name }}</h4>
              <p class="sem-time">{{ formatDate(s.startTime, 'date') }} — {{ formatDate(s.endTime, 'date') }}</p>
              <NTag size="tiny" :bordered="false">{{ s.lessonCount }} 课时</NTag>
            </div>
          </NCard>
        </div>
        <NEmpty v-else description="暂无学期" class="empty-hint" />
      </NTabPane>

      <NTabPane name="lessons" tab="课时">
        <div class="tab-head">
          <NSelect
            v-if="semesters.length"
            v-model:value="activeSemesterId"
            :options="semesters.map(s => ({ label: s.name, value: s.id }))"
            size="small"
            style="width:220px"
          />
        </div>
        <NDataTable
          v-if="activeSemesterId && lessons.length"
          :columns="lesColumns"
          :data="lessons"
          size="small"
          :row-key="(r: LessonVO) => r.id"
        />
        <div v-if="selectedLessonId" style="margin-top:12px;padding:12px 16px;border:1px solid var(--n-border-color);border-radius:8px">
          <LessonTaskPanel :lesson-id="selectedLessonId" readonly />
        </div>
        <NEmpty v-else-if="activeSemesterId" description="该学期暂无课时" class="empty-hint" />
        <NEmpty v-else description="请先选择一个学期" class="empty-hint" />
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
.course-name { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; margin: 0 0 6px; }
.course-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0 0 10px; line-height: 1.5; }
.course-meta { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--n-text-color-2); flex-wrap: wrap; }
.meta-label { color: var(--n-text-color-3); }
.meta-sep { color: var(--n-border-color); margin: 0 4px; }
.tab-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-subtitle { font-size: 13px; color: var(--n-text-color-3); }
.sem-list { display: flex; flex-direction: column; gap: 10px; }
.sem-card { display: flex; flex-direction: row; justify-content: space-between; align-items: center; }
.sem-info { display: flex; flex-direction: column; gap: 2px; }
.sem-name { font-size: 15px; font-weight: 600; margin: 0; }
.sem-time { font-size: 12px; color: var(--n-text-color-3); margin: 0; }
.empty-hint { padding: 40px 0; }
.resource-tab { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 40px 0; }
.resource-desc { font-size: 14px; color: var(--n-text-color-2); margin: 0; }
</style>
