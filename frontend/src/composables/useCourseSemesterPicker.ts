import { computed } from 'vue'
import { useSemesterStore } from '@/stores/semester'

export function useCourseSemesterPicker() {
  const semesterStore = useSemesterStore()

  const activeCourseId = computed({
    get: () => semesterStore.activeCourseId,
    set: value => { void semesterStore.selectCourse(value) },
  })
  const activeSemesterId = computed({
    get: () => semesterStore.activeSemesterId,
    set: value => semesterStore.selectSemester(value),
  })
  const courseOptions = computed(() => semesterStore.courses.map(course => ({ label: course.name, value: course.id })))
  const semesterOptions = computed(() => semesterStore.semesters.map(semester => ({ label: semester.name, value: semester.id })))

  return {
    courses: computed(() => semesterStore.courses),
    semesters: computed(() => semesterStore.semesters),
    activeCourseId,
    activeSemesterId,
    loading: computed(() => semesterStore.loading),
    courseOptions,
    semesterOptions,
    loadCourses: semesterStore.loadCourses,
  }
}
