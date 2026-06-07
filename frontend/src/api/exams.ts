import http from './request'
import type { ExamPaperVO, ExamSubmitDTO, ExamVO } from '@/types/api'

export function listExams(semesterId: number): Promise<ExamVO[]> {
  return http.get(`/semesters/${semesterId}/exams`)
}

export function listExamPapers(): Promise<ExamPaperVO[]> {
  return http.get('/exam-papers')
}

export function createExamPaper(data: { title: string; content: string; totalScore: number }): Promise<ExamPaperVO> {
  return http.post('/exam-papers', data)
}

export function createExam(semesterId: number, data: Partial<ExamVO> & { paperId?: number | null }): Promise<ExamVO> {
  return http.post(`/semesters/${semesterId}/exams`, data)
}

export function updateExam(id: number, data: Partial<ExamVO> & { paperId?: number | null }): Promise<ExamVO> {
  return http.put(`/exams/${id}`, data)
}

export function deleteExam(id: number): Promise<void> {
  return http.delete(`/exams/${id}`)
}

export function submitExam(id: number, data: ExamSubmitDTO): Promise<void> {
  return http.post(`/exams/${id}/submit`, data)
}
