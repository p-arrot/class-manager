import { computed, ref, watch } from 'vue'
import { listCourses } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import type { CourseVO, SemesterVO } from '@/types/api'

export function useCourseSemesterPicker() {
  const courses = ref<CourseVO[]>([])
  const semesters = ref<SemesterVO[]>([])
  const activeCourseId = ref<number | null>(null)
  const activeSemesterId = ref<number | null>(null)
  const loading = ref(false)

  const courseOptions = computed(() => courses.value.map(course => ({ label: course.name, value: course.id })))
  const semesterOptions = computed(() => semesters.value.map(semester => ({ label: semester.name, value: semester.id })))

  async function loadCourses() {
    loading.value = true
    try {
      const result = await listCourses({ page: 1, size: 100 })
      courses.value = result.records
      activeCourseId.value = result.records[0]?.id ?? null
    } finally {
      loading.value = false
    }
  }

  async function loadSemesters(courseId: number | null) {
    activeSemesterId.value = null
    semesters.value = []
    if (!courseId) return
    semesters.value = await listSemesters(courseId)
    activeSemesterId.value = pickCurrentSemester(semesters.value)?.id ?? semesters.value[0]?.id ?? null
  }

  function pickCurrentSemester(list: SemesterVO[]) {
    const now = Date.now()
    return list.find(semester => new Date(semester.startTime).getTime() <= now && now <= new Date(semester.endTime).getTime())
  }

  watch(activeCourseId, loadSemesters)

  return {
    courses,
    semesters,
    activeCourseId,
    activeSemesterId,
    loading,
    courseOptions,
    semesterOptions,
    loadCourses,
  }
}
