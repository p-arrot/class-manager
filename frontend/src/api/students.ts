import http from './request'
import type {
  StudentVO, StudentImportResultVO, StudentPageQuery,
  PasswordResetDTO, StudentCreateDTO, StudentUpdateDTO, StudentBatchDTO, PageResult,
  EvaluationVO, RadarVO, SubmissionVO
} from '@/types/api'

export function createStudent(data: StudentCreateDTO): Promise<StudentVO> {
  return http.post('/students', data)
}

export function updateStudent(id: number, data: StudentUpdateDTO): Promise<StudentVO> {
  return http.put(`/students/${id}`, data)
}

export function deleteStudent(id: number): Promise<void> {
  return http.delete(`/students/${id}`)
}

export function listStudents(params: StudentPageQuery): Promise<PageResult<StudentVO>> {
  return http.get('/students', { params })
}

export function importStudents(file: File): Promise<StudentImportResultVO> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/students/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

export function resetPassword(id: number, data: PasswordResetDTO): Promise<void> {
  return http.put(`/students/${id}/password`, data)
}

export function batchDeleteStudents(data: StudentBatchDTO): Promise<void> {
  return http.post('/students/batch/delete', data)
}

export function batchResetPassword(data: StudentBatchDTO): Promise<void> {
  return http.post('/students/batch/password', data)
}

export function getStudentRadar(studentId: number, semesterId: number): Promise<RadarVO> {
  return http.get(`/students/${studentId}/radar`, { params: { semesterId } })
}

export function listStudentEvaluations(studentId: number, semesterId: number): Promise<EvaluationVO[]> {
  return http.get(`/students/${studentId}/evaluations`, { params: { semesterId } })
}

export function listStudentSubmissions(studentId: number, semesterId: number): Promise<SubmissionVO[]> {
  return http.get(`/students/${studentId}/submissions`, { params: { semesterId } })
}
