import http from './request'
import type { SemesterVO, SemesterCreateDTO, SemesterUpdateDTO } from '@/types/api'

export function listSemesters(courseId: number): Promise<SemesterVO[]> {
  return http.get(`/courses/${courseId}/semesters`)
}

export function getSemester(id: number): Promise<SemesterVO> {
  return http.get(`/semesters/${id}`)
}

export function createSemester(courseId: number, data: SemesterCreateDTO): Promise<SemesterVO> {
  return http.post(`/courses/${courseId}/semesters`, data)
}

export function updateSemester(id: number, data: SemesterUpdateDTO): Promise<SemesterVO> {
  return http.put(`/semesters/${id}`, data)
}

export function deleteSemester(id: number): Promise<void> {
  return http.delete(`/semesters/${id}`)
}
