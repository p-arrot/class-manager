import http from './request'
import type { LessonVO, LessonCreateDTO, LessonUpdateDTO, LessonSortDTO } from '@/types/api'

export function listLessons(semesterId: number): Promise<LessonVO[]> {
  return http.get(`/semesters/${semesterId}/lessons`)
}

export function getLesson(id: number): Promise<LessonVO> {
  return http.get(`/lessons/${id}`)
}

export function createLesson(semesterId: number, data: LessonCreateDTO): Promise<LessonVO> {
  return http.post(`/semesters/${semesterId}/lessons`, data)
}

export function updateLesson(id: number, data: LessonUpdateDTO): Promise<LessonVO> {
  return http.put(`/lessons/${id}`, data)
}

export function deleteLesson(id: number): Promise<void> {
  return http.delete(`/lessons/${id}`)
}

export function reorderLesson(id: number, data: LessonSortDTO): Promise<void> {
  return http.put(`/lessons/${id}/sort`, data)
}
