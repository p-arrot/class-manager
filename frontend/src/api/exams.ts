import http from './request'
import type { ExamGradeDTO, ExamPaperVO, ExamSubmissionVO, ExamSubmitDTO, ExamVO } from '@/types/api'

export function listExams(semesterId: number): Promise<ExamVO[]> {
  return http.get(`/semesters/${semesterId}/exams`)
}

export function getExam(id: number): Promise<ExamVO> {
  return http.get(`/exams/${id}`)
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

export function startExam(id: number): Promise<ExamSubmissionVO> {
  return http.post(`/exams/${id}/start`)
}

export function saveExamDraft(id: number, data: ExamSubmitDTO): Promise<ExamSubmissionVO> {
  return http.put(`/exams/${id}/draft`, data)
}

export function getMyExamSubmission(id: number): Promise<ExamSubmissionVO | null> {
  return http.get(`/exams/${id}/my-submission`)
}

export function submitExam(id: number, data: ExamSubmitDTO): Promise<ExamSubmissionVO> {
  return http.post(`/exams/${id}/submit`, data)
}

export function listExamSubmissions(id: number): Promise<ExamSubmissionVO[]> {
  return http.get(`/exams/${id}/submissions`)
}

export function gradeExamSubmission(id: number, data: ExamGradeDTO): Promise<void> {
  return http.put(`/exam-submissions/${id}/grade`, data)
}

export function returnExamSubmission(id: number, reason: string): Promise<void> {
  return http.put(`/exam-submissions/${id}/return`, { reason })
}
