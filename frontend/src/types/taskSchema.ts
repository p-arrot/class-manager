export type QuestionType = 'blank' | 'single' | 'multiple' | 'true_false' | 'short'
export type CoreDimension = 'AWARENESS' | 'COMPUTING' | 'DIGITAL_LEARNING' | 'RESPONSIBILITY'
export type WorksheetAnswerValue = string | string[] | boolean | number | null
export type WorksheetAnswerMap = Record<string, WorksheetAnswerValue>

export interface DimensionScoreConfig {
  dimension: CoreDimension
  maxScore: number
}

export interface TaskQuestion {
  id: string
  type: QuestionType
  stem: string
  required: boolean
  imageUrl?: string
  options?: string[]
  answer?: string | string[] | boolean
  autoGrade?: boolean
  dimensionScores: DimensionScoreConfig[]
  title?: string
  markdown?: string
  score?: number
}

export interface ArtifactSchema {
  submitMode: 'file' | 'folder'
  allowedExtensions: string[]
}

export interface TaskFormSchema {
  version: 3
  questions?: TaskQuestion[]
  artifact?: ArtifactSchema
}

interface LegacyWorksheetField {
  id?: unknown
  type?: unknown
  label?: unknown
  required?: unknown
  imageUrl?: unknown
  options?: unknown
}

interface RawTaskQuestion {
  id?: unknown
  type?: unknown
  stem?: unknown
  title?: unknown
  markdown?: unknown
  required?: unknown
  imageUrl?: unknown
  options?: unknown
  answer?: unknown
  autoGrade?: unknown
  dimensionScores?: unknown
  score?: unknown
}

export const CORE_DIMENSIONS: Array<{ key: CoreDimension; label: string }> = [
  { key: 'AWARENESS', label: '信息意识' },
  { key: 'COMPUTING', label: '计算思维' },
  { key: 'DIGITAL_LEARNING', label: '数字化学习与创新' },
  { key: 'RESPONSIBILITY', label: '信息社会责任' },
]

export function parseTaskSchema(schema?: string | null): TaskFormSchema {
  if (!schema) return { version: 3, questions: [] }
  try {
    const parsed = JSON.parse(schema) as Record<string, unknown>
    if (parsed.version === 3) {
      return {
        ...parsed,
        version: 3,
        questions: asArray(parsed.questions).map(normalizeQuestion),
      }
    }
    if (parsed.version === 2) {
      return {
        ...parsed,
        version: 3,
        questions: asArray(parsed.questions).map(normalizeQuestion),
      }
    }
    return {
      version: 3,
      questions: asArray(parsed.fields).map((field) => normalizeLegacyField(field)),
    }
  } catch {
    return { version: 3, questions: [] }
  }
}

function normalizeLegacyField(field: unknown): TaskQuestion {
  const raw = asRecord(field) as LegacyWorksheetField
  const type = typeof raw.type === 'string' ? raw.type : ''
  return {
    id: String(raw.id ?? crypto.randomUUID()),
    type: type === 'radio' ? 'single' : type === 'checkbox' ? 'multiple' : type === 'textarea' ? 'short' : 'blank',
    stem: typeof raw.label === 'string' ? raw.label : '',
    required: raw.required !== false,
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : undefined,
    options: asStringArray(raw.options),
    dimensionScores: defaultDimensionScores(1),
  }
}

export function emptyQuestion(type: QuestionType): TaskQuestion {
  const id = crypto.randomUUID().slice(0, 8)
  if (type === 'single') return { id, type, stem: '', required: true, options: ['选项 1', '选项 2'], autoGrade: true, answer: '', dimensionScores: defaultDimensionScores(1) }
  if (type === 'multiple') return { id, type, stem: '', required: true, options: ['选项 1', '选项 2'], autoGrade: true, answer: [], dimensionScores: defaultDimensionScores(1) }
  if (type === 'true_false') return { id, type, stem: '', required: true, options: ['正确', '错误'], autoGrade: true, answer: true, dimensionScores: defaultDimensionScores(1) }
  return { id, type, stem: '', required: true, autoGrade: false, dimensionScores: defaultDimensionScores(1) }
}

export function questionStem(question: TaskQuestion) {
  return question.stem || [question.title, question.markdown].filter(Boolean).join('\n\n')
}

export function questionTotalScore(question: TaskQuestion) {
  return normalizeDimensionScores(question.dimensionScores, question.score).reduce((sum, item) => sum + item.maxScore, 0)
}

export function normalizeDimensionScores(scores?: unknown, legacyScore?: unknown): DimensionScoreConfig[] {
  const scoreList = Array.isArray(scores) ? scores : []
  const normalized = CORE_DIMENSIONS.map(dim => {
    const existing = scoreList.find(item => asRecord(item).dimension === dim.key)
    return { dimension: dim.key, maxScore: Number(asRecord(existing).maxScore ?? 0) }
  })
  if (normalized.some(item => item.maxScore > 0)) return normalized
  return defaultDimensionScores(Number(legacyScore ?? 1))
}

function normalizeQuestion(question: unknown): TaskQuestion {
  const raw = asRecord(question) as RawTaskQuestion
  const title = typeof raw.title === 'string' ? raw.title : ''
  const markdown = typeof raw.markdown === 'string' ? raw.markdown : ''
  const type = isQuestionType(raw.type) ? raw.type : 'blank'
  return {
    id: String(raw.id ?? crypto.randomUUID()),
    type,
    stem: typeof raw.stem === 'string' && raw.stem ? raw.stem : [title, markdown].filter(Boolean).join('\n\n'),
    required: raw.required !== false,
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : undefined,
    options: asStringArray(raw.options),
    answer: isAnswerValue(raw.answer) ? raw.answer : undefined,
    autoGrade: Boolean(raw.autoGrade),
    dimensionScores: normalizeDimensionScores(raw.dimensionScores, raw.score),
    title,
    markdown,
    score: typeof raw.score === 'number' ? raw.score : undefined,
  }
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' ? value as Record<string, unknown> : {}
}

function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : []
}

function asStringArray(value: unknown): string[] | undefined {
  return Array.isArray(value) ? value.map(String) : undefined
}

function isQuestionType(value: unknown): value is QuestionType {
  return value === 'blank' || value === 'single' || value === 'multiple' || value === 'true_false' || value === 'short'
}

function isAnswerValue(value: unknown): value is TaskQuestion['answer'] {
  return typeof value === 'string' || typeof value === 'boolean' || (Array.isArray(value) && value.every(item => typeof item === 'string'))
}

function defaultDimensionScores(score: number): DimensionScoreConfig[] {
  return CORE_DIMENSIONS.map(dim => ({
    dimension: dim.key,
    maxScore: dim.key === 'COMPUTING' ? score : 0,
  }))
}
