import http from './request'
import type { TaskAnalyticsVO, TaskVO, TaskDetailVO, TaskCreateDTO, TaskUpdateDTO, SubmissionVO, SubmissionDTO, SubmissionEvaluationDTO } from '@/types/api'

export function listTasks(lessonId: number): Promise<TaskVO[]> {
  return http.get(`/lessons/${lessonId}/tasks`)
}

export function getTask(id: number): Promise<TaskDetailVO> {
  return http.get(`/tasks/${id}`)
}

export function createTask(lessonId: number, data: TaskCreateDTO): Promise<TaskVO> {
  return http.post(`/lessons/${lessonId}/tasks`, data)
}

export function updateTask(id: number, data: TaskUpdateDTO): Promise<TaskVO> {
  return http.put(`/tasks/${id}`, data)
}

export function deleteTask(id: number): Promise<void> {
  return http.delete(`/tasks/${id}`)
}

export function submitTask(taskId: number, data: SubmissionDTO): Promise<SubmissionVO> {
  return http.post(`/tasks/${taskId}/submit`, data)
}

export function listSubmissions(taskId: number, classId?: number): Promise<SubmissionVO[]> {
  return http.get(`/tasks/${taskId}/submissions`, { params: classId ? { classId } : undefined })
}

export function getTaskAnalytics(taskId: number, classId?: number): Promise<TaskAnalyticsVO> {
  return http.get(`/tasks/${taskId}/analytics`, { params: classId ? { classId } : undefined })
}

export function evaluateSubmission(submissionId: number, data: SubmissionEvaluationDTO): Promise<void> {
  return http.post(`/submissions/${submissionId}/evaluate`, data)
}
