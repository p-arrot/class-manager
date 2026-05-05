import http from './request'
import type { FileUploadVO, FileUploadDTO } from '@/types/api'

export function getPresignedUploadUrl(data: FileUploadDTO): Promise<FileUploadVO> {
  return http.post('/files/upload/presigned', data)
}

export function getDownloadUrl(resourceId: number): Promise<{ url: string }> {
  return http.get(`/files/${resourceId}/download`)
}

export function getPreviewUrl(resourceId: number): Promise<{ url: string }> {
  return http.get(`/files/${resourceId}/preview`)
}

export function getStreamUrl(resourceId: number): Promise<{ url: string }> {
  return http.get(`/files/${resourceId}/stream`)
}
