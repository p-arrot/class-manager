import { ref } from 'vue'
import http from '@/api/request'
import { validateFileSize } from '@/utils/validation'

export function useFileUpload() {
  const state = ref<{ uploading: boolean; progress: number; error: string | null }>({
    uploading: false, progress: 0, error: null,
  })

  let lastFile: File | null = null
  let lastCourseId = 0
  let lastParentId: number | null = null

  async function uploadFile(
    file: File, courseId: number, parentId: number | null = null
  ): Promise<number | null> {
    const sizeResult = validateFileSize(file)
    if (!sizeResult.valid) { state.value.error = sizeResult.message!; return null }

    lastFile = file; lastCourseId = courseId; lastParentId = parentId
    return doUpload(file, courseId, parentId)
  }

  async function retry(): Promise<number | null> {
    if (!lastFile) return null
    return doUpload(lastFile, lastCourseId, lastParentId)
  }

  async function doUpload(
    file: File, courseId: number, parentId: number | null
  ): Promise<number | null> {
    state.value = { uploading: true, progress: 0, error: null }
    try {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('courseId', String(courseId))
      if (parentId != null) fd.append('parentId', String(parentId))

      const result = await http.post('/files/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
        onUploadProgress: (e) => {
          if (e.total) state.value.progress = Math.round((e.loaded / e.total) * 100)
        },
      }) as unknown as { resourceId: number }
      state.value = { uploading: false, progress: 100, error: null }
      return result.resourceId
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : '上传失败'
      state.value = { uploading: false, progress: 0, error: message }
      return null
    }
  }

  function reset() { state.value = { uploading: false, progress: 0, error: null }; lastFile = null }

  return { state, uploadFile, retry, reset }
}
