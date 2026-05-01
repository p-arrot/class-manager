import http from './request'
import type { ClassVO, ClassCreateDTO, ClassUpdateDTO, ClassPageQuery, PageResult } from '@/types/api'

export function createClass(data: ClassCreateDTO): Promise<ClassVO> {
  return http.post('/classes', data)
}

export function updateClass(id: number, data: ClassUpdateDTO): Promise<ClassVO> {
  return http.put(`/classes/${id}`, data)
}

export function deleteClass(id: number): Promise<void> {
  return http.delete(`/classes/${id}`)
}

export function getClass(id: number): Promise<ClassVO> {
  return http.get(`/classes/${id}`)
}

export function listClasses(params: ClassPageQuery): Promise<PageResult<ClassVO>> {
  return http.get('/classes', { params })
}

export function listAllClasses(): Promise<ClassVO[]> {
  return http.get('/classes/list-all')
}
