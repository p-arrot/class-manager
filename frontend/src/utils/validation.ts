export const ALLOWED_FILE_EXTENSIONS = [
  'doc', 'docx', 'ppt', 'pptx', 'pdf', 'xls', 'xlsx', 'txt',
  'html', 'htm', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'mp3', 'mp4', 'zip', 'rar',
]

export const MAX_FILE_SIZE = 200 * 1024 * 1024 // 200 MB

export interface FileValidationResult {
  valid: boolean
  message?: string
}

export function validateFileSize(file: File, maxBytes: number = MAX_FILE_SIZE): FileValidationResult {
  if (file.size <= 0) {
    return { valid: false, message: '文件为空' }
  }
  if (file.size > maxBytes) {
    const maxMB = Math.round(maxBytes / 1024 / 1024)
    return { valid: false, message: `文件大小超过 ${maxMB}MB 限制` }
  }
  return { valid: true }
}

export function validateFileType(file: File, allowed: string[] = ALLOWED_FILE_EXTENSIONS): FileValidationResult {
  const ext = getFileExtension(file.name)
  if (!ext) {
    return { valid: false, message: '无法识别文件类型' }
  }
  if (!allowed.includes(ext.toLowerCase())) {
    return { valid: false, message: `不支持的文件类型 .${ext}` }
  }
  return { valid: true }
}

export function getFileExtension(fileName: string): string {
  const dot = fileName.lastIndexOf('.')
  return dot === -1 ? '' : fileName.substring(dot + 1)
}

export function formatFileSize(bytes?: number | null): string {
  if (!bytes || bytes < 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
