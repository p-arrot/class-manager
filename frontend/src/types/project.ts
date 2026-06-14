import { normalizeDimensionScores } from '@/types/taskSchema'
import type { ProjectSubmissionVO, ProjectVO } from '@/types/api'
import type { ArtifactFile } from '@/types/grading'
import type { DimensionScoreConfig } from '@/types/taskSchema'

export type ProjectSubmitMode = 'file' | 'folder'

export interface ProjectArtifactConfig {
  submitMode: ProjectSubmitMode
  allowedExtensions: string[]
}

export interface ProjectDescriptionPayload {
  text: string
  artifact: ProjectArtifactConfig
  rubric: DimensionScoreConfig[]
}

export interface ProjectFormValue {
  name: string
  description: string
  maxTeamSize: number
  deadline: number | null
  submitMode: ProjectSubmitMode
  allowedExtensions: string
  dimensionScores: DimensionScoreConfig[]
}

export interface ProjectSubmissionContent {
  note: string
  files: ArtifactFile[]
}

export interface ProjectSubmissionRow extends ProjectSubmissionVO {
  parsed: ProjectSubmissionContent
}

export const defaultProjectArtifact: ProjectArtifactConfig = {
  submitMode: 'file',
  allowedExtensions: [],
}

export function createEmptyProjectForm(): ProjectFormValue {
  return {
    name: '',
    description: '',
    maxTeamSize: 1,
    deadline: null,
    submitMode: 'file',
    allowedExtensions: '',
    dimensionScores: normalizeDimensionScores(undefined),
  }
}

export function createProjectFormFromProject(project: ProjectVO): ProjectFormValue {
  const parsed = parseProjectDescription(project)
  return {
    name: project.name,
    description: parsed.text,
    maxTeamSize: project.maxTeamSize,
    deadline: project.deadline ? new Date(project.deadline).getTime() : null,
    submitMode: parsed.artifact.submitMode,
    allowedExtensions: parsed.artifact.allowedExtensions.join(', '),
    dimensionScores: normalizeDimensionScores(parsed.rubric),
  }
}

export function buildProjectDescription(form: ProjectFormValue) {
  const artifact = {
    submitMode: form.submitMode,
    allowedExtensions: form.allowedExtensions.split(/[\s,，]+/).map(value => value.replace(/^\./, '').toLowerCase()).filter(Boolean),
  }
  return JSON.stringify({
    text: form.description,
    artifact,
    rubric: normalizeDimensionScores(form.dimensionScores),
  })
}

export function parseProjectDescription(project?: ProjectVO | null): ProjectDescriptionPayload {
  if (!project) {
    return { text: '', artifact: defaultProjectArtifact, rubric: normalizeDimensionScores(undefined) }
  }
  try {
    const parsed = JSON.parse(project.description || '{}') as Partial<ProjectDescriptionPayload>
    return {
      text: typeof parsed.text === 'string' ? parsed.text : project.description || '',
      artifact: normalizeArtifactConfig(parsed.artifact),
      rubric: normalizeDimensionScores(parsed.rubric),
    }
  } catch {
    return {
      text: project.description || '',
      artifact: defaultProjectArtifact,
      rubric: normalizeDimensionScores(undefined),
    }
  }
}

export function parseProjectSubmissionContent(content?: string | null): ProjectSubmissionContent {
  try {
    const parsed = JSON.parse(content || '{}') as Partial<ProjectSubmissionContent>
    return {
      note: typeof parsed.note === 'string' ? parsed.note : '',
      files: Array.isArray(parsed.files) ? parsed.files.filter(isArtifactFile) : [],
    }
  } catch {
    return { note: content || '', files: [] }
  }
}

function normalizeArtifactConfig(config?: ProjectArtifactConfig): ProjectArtifactConfig {
  return {
    submitMode: config?.submitMode === 'folder' ? 'folder' : 'file',
    allowedExtensions: Array.isArray(config?.allowedExtensions) ? config.allowedExtensions.map(String) : [],
  }
}

function isArtifactFile(value: unknown): value is ArtifactFile {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value)
    && typeof (value as ArtifactFile).id === 'number'
    && typeof (value as ArtifactFile).name === 'string')
}
