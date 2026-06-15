import http from './request'
import type { ProjectSubmissionVO, ProjectSubmitDTO, ProjectVO, QuestionDimensionScoreDTO } from '@/types/api'

export function listProjects(semesterId: number): Promise<ProjectVO[]> {
  return http.get(`/semesters/${semesterId}/projects`)
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

export function listProjectSubmissions(id: number): Promise<ProjectSubmissionVO[]> {
  return http.get(`/projects/${id}/submissions`)
}

export function scoreProjectSubmission(id: number, data: QuestionDimensionScoreDTO[]): Promise<void> {
  return http.post(`/project-submissions/${id}/score`, data)
}

export function createProjectTeam(id: number, name: string): Promise<{ id: number; projectId: number; teamName: string; status: string }> {
  return http.post(`/projects/${id}/teams`, { name })
}
