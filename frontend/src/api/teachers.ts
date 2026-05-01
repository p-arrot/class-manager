import http from './request'
import type {
  TeacherVO, TeacherCreateDTO, TeacherUpdateDTO, TeacherClassVO,
  BatchBindDTO, PageResult, PageQuery
} from '@/types/api'

export function createTeacher(data: TeacherCreateDTO): Promise<TeacherVO> {
  return http.post('/teachers', data)
}

export function updateTeacher(id: number, data: TeacherUpdateDTO): Promise<TeacherVO> {
  return http.put(`/teachers/${id}`, data)
}

export function getTeacher(id: number): Promise<TeacherVO> {
  return http.get(`/teachers/${id}`)
}

export function listTeachers(params: PageQuery): Promise<PageResult<TeacherVO>> {
  return http.get('/teachers', { params })
}

export function getTeacherClasses(id: number): Promise<TeacherClassVO[]> {
  return http.get(`/teachers/${id}/classes`)
}

export function bindClasses(id: number, data: BatchBindDTO): Promise<number> {
  return http.post(`/teachers/${id}/classes`, data)
}

export function unbindClasses(id: number, data: BatchBindDTO): Promise<number> {
  return http.delete(`/teachers/${id}/classes`, { data })
}
