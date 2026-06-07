import type { WorksheetAnswerValue } from '@/types/taskSchema'

export interface ArtifactFile {
  id: number
  name: string
  fileSize?: number | null
  type?: string
}

export interface ParsedSubmissionContent extends Record<string, WorksheetAnswerValue | ArtifactFile[] | undefined> {
  raw?: string
  note?: string
  files?: ArtifactFile[]
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

export function isArtifactFile(value: unknown): value is ArtifactFile {
  if (!isRecord(value)) return false
  return typeof value.id === 'number' && typeof value.name === 'string'
}
