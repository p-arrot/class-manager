import http from './request'
import type { CourseVO, CourseDetailVO, CourseCreateDTO, CourseUpdateDTO, CoursePageQuery, PageResult, CourseResourceVO, FileUploadVO } from '@/types/api'

export function listCourses(params: CoursePageQuery): Promise<PageResult<CourseVO>> {
  return http.get('/courses', { params })
}

export function getCourse(id: number): Promise<CourseDetailVO> {
  return http.get(`/courses/${id}`)
}

export function createCourse(data: CourseCreateDTO): Promise<CourseVO> {
  return http.post('/courses', data)
}

export function updateCourse(id: number, data: CourseUpdateDTO): Promise<CourseVO> {
  return http.put(`/courses/${id}`, data)
}

export function deleteCourse(id: number): Promise<void> {
  return http.delete(`/courses/${id}`)
}

export function uploadCourseCover(file: File): Promise<FileUploadVO> {
  const data = new FormData()
  data.append('file', file)
  return http.post('/files/course-cover/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

// ========== Course Resources ==========

export function listCourseResources(courseId: number, parentId?: number | null): Promise<CourseResourceVO[]> {
  if (parentId === undefined || parentId === null) {
    return http.get(`/courses/${courseId}/resources/tree`)
  }
  return http.get(`/courses/${courseId}/resources`, { params: { parentId } })
}

export function createResourceFolder(courseId: number, data: { name: string; parentId?: number | null }): Promise<CourseResourceVO> {
  return http.post(`/courses/${courseId}/resources`, data)
}

export function renameResource(id: number, data: { name: string }): Promise<void> {
  return http.put(`/resources/${id}`, data)
}

export function deleteResource(id: number): Promise<void> {
  return http.delete(`/resources/${id}`)
}
