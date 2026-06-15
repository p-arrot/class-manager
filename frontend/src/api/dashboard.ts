import http from './request'
import type { StudentDashboardVO, TeacherDashboardVO } from '@/types/api'

export function getStudentDashboard(): Promise<StudentDashboardVO> {
  return http.get('/dashboard/student')
}

export function getTeacherDashboard(): Promise<TeacherDashboardVO> {
  return http.get('/dashboard/teacher')
}
