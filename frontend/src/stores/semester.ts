import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listCourses } from '@/api/courses'
import { listSemesters } from '@/api/semesters'
import type { CourseVO, SemesterVO } from '@/types/api'

export const useSemesterStore = defineStore('semester', () => {
  const courses = ref<CourseVO[]>([])
  const semesters = ref<SemesterVO[]>([])
  const activeCourseId = ref<number | null>(null)
  const activeSemesterId = ref<number | null>(null)
  const loading = ref(false)

  function pickCurrentSemester(list: SemesterVO[]) {
    const now = Date.now()
    return list.find(semester => new Date(semester.startTime).getTime() <= now && now <= new Date(semester.endTime).getTime())
  }

  function pickCourse(records: CourseVO[]) {
    const current = records.find(course => course.id === activeCourseId.value)
    return current?.id ?? records[0]?.id ?? null
  }

  function pickSemester(records: SemesterVO[], preserveCurrent: boolean) {
    if (preserveCurrent) {
      const current = records.find(semester => semester.id === activeSemesterId.value)
      if (current) return current.id
    }
    return pickCurrentSemester(records)?.id ?? records[0]?.id ?? null
  }

  async function loadCourses() {
    loading.value = true
    try {
      const result = await listCourses({ page: 1, size: 100 })
      courses.value = result.records
      const nextCourseId = pickCourse(result.records)
      activeCourseId.value = nextCourseId
      await loadSemesters(nextCourseId, true)
    } finally {
      loading.value = false
    }
  }

  async function loadSemesters(courseId = activeCourseId.value, preserveCurrent = true) {
    semesters.value = []
    activeSemesterId.value = null
    if (!courseId) return
    const result = await listSemesters(courseId)
    semesters.value = result
    activeSemesterId.value = pickSemester(result, preserveCurrent)
  }

  async function selectCourse(courseId: number | null) {
    if (activeCourseId.value === courseId) return
    activeCourseId.value = courseId
    loading.value = true
    try {
      await loadSemesters(courseId, false)
    } finally {
      loading.value = false
    }
  }

  function selectSemester(semesterId: number | null) {
    activeSemesterId.value = semesterId
  }

  function reset() {
    courses.value = []
    semesters.value = []
    activeCourseId.value = null
    activeSemesterId.value = null
    loading.value = false
  }

  return {
    courses,
    semesters,
    activeCourseId,
    activeSemesterId,
    loading,
    loadCourses,
    loadSemesters,
    selectCourse,
    selectSemester,
    reset,
  }
})
