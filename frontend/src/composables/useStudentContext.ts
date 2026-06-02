import { ref, onMounted } from 'vue'
import http from '@/api/request'

export function useStudentContext() {
  const courses = ref<any[]>([])
  const semesters = ref<any[]>([])
  const loading = ref(false)

  async function loadCourses() {
    loading.value = true
    try {
      courses.value = (await http.get('/courses?page=1&size=50'))?.records || []
    } catch { courses.value = [] }
    finally { loading.value = false }
  }

  async function loadSemesters(courseId: number) {
    try { semesters.value = await http.get(`/courses/${courseId}/semesters`) }
    catch { semesters.value = [] }
  }

  onMounted(loadCourses)

  return { courses, semesters, loading, loadCourses, loadSemesters }
}
