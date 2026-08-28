import http from './request'
import type { ProjectSubmissionVO, ProjectSubmitDTO, ProjectVO, QuestionDimensionScoreDTO } from '@/types/api'

export function listProjects(semesterId: number): Promise<ProjectVO[]> {
  return http.get(`/semesters/${semesterId}/projects`)
}

export function getProject(id: number): Promise<ProjectVO> {
  return http.get(`/projects/${id}`)
}

export function createProject(semesterId: number, data: Partial<ProjectVO>): Promise<ProjectVO> {
  return http.post(`/semesters/${semesterId}/projects`, data)
}

export function updateProject(id: number, data: Partial<ProjectVO>): Promise<ProjectVO> {
  return http.put(`/projects/${id}`, data)
}

export function deleteProject(id: number): Promise<void> {
  return http.delete(`/projects/${id}`)
}

export function submitProject(id: number, data: ProjectSubmitDTO): Promise<ProjectSubmissionVO> {
  return http.post(`/projects/${id}/submit`, data)
}

export function getMyProjectSubmission(id: number): Promise<ProjectSubmissionVO | null> {
  return http.get(`/projects/${id}/my-submission`)
}

export function listProjectSubmissions(id: number): Promise<ProjectSubmissionVO[]> {
  return http.get(`/projects/${id}/submissions`)
}

export function scoreProjectSubmission(id: number, data: QuestionDimensionScoreDTO[]): Promise<void> {
  return http.post(`/project-submissions/${id}/score`, data)
}

export function returnProjectSubmission(id: number, reason: string): Promise<void> {
  return http.put(`/project-submissions/${id}/return`, { reason })
}
