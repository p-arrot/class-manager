import http from './request'
import type { SemesterStatsPreviewRow } from '@/types/api'

export function getSemesterStatsPreview(semesterId: number): Promise<SemesterStatsPreviewRow[]> {
  return http.get(`/stats/semester/${semesterId}/preview`)
}

export function exportSemesterStats(semesterId: number): Promise<Blob> {
  return http.get(`/stats/semester/${semesterId}/export`, { responseType: 'blob' })
}
