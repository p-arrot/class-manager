import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { listCourses } from '@/api/courses'
import { listSemesters as listCourseSemesters } from '@/api/semesters'
import { getErrorMessage } from '@/utils/error'
import type { CourseVO, SemesterVO } from '@/types/api'

export function useStudentContext() {
  const message = useMessage()
  const courses = ref<CourseVO[]>([])
  const semesters = ref<SemesterVO[]>([])
  const loading = ref(false)

  async function loadCourses() {
    loading.value = true
    try {
      courses.value = (await listCourses({ page: 1, size: 50 })).records || []
    } catch (e) {
      courses.value = []
      message.error(getErrorMessage(e, '加载课程列表失败'))
    } finally {
      loading.value = false
    }
  }

  async function loadSemesters(courseId: number) {
    try {
      semesters.value = await listCourseSemesters(courseId)
    } catch (e) {
      semesters.value = []
      message.error(getErrorMessage(e, '加载学期列表失败'))
    }
  }

  onMounted(loadCourses)

  return { courses, semesters, loading, loadCourses, loadSemesters }
}
