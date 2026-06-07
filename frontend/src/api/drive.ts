import http from './request'
import type { DriveFolderCreateDTO, DriveItemVO, DriveTreeQuery, PreviewUrlVO } from '@/types/api'

export function listDriveItems(params: DriveTreeQuery = {}): Promise<DriveItemVO[]> {
  return http.get('/drive/tree', { params })
}

export function createDriveFolder(data: DriveFolderCreateDTO): Promise<DriveItemVO> {
  return http.post('/drive/folders', data)
}

export function deleteDriveItem(id: number): Promise<void> {
  return http.delete(`/drive/${id}`)
}

export function uploadDriveFile(data: FormData): Promise<DriveItemVO> {
  return http.post('/drive/upload', data)
}

export function getDriveRaw(id: number): Promise<Blob> {
  return http.get(`/drive/${id}/raw`, { responseType: 'blob' })
}

export function getDrivePreview(id: number): Promise<PreviewUrlVO> {
  return http.get(`/drive/${id}/preview`)
}
