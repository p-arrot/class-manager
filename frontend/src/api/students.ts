import http from './request'
import type {
  StudentVO, StudentImportResultVO, StudentPageQuery,
  PasswordResetDTO, PageResult
} from '@/types/api'

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
