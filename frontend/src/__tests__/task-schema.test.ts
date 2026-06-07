import { describe, expect, it } from 'vitest'
import {
  normalizeDimensionScores,
  parseTaskSchema,
  questionStem,
  questionTotalScore,
  type TaskQuestion,
} from '@/types/taskSchema'
import { formatFileSize } from '@/utils/validation'

describe('task schema utilities', () => {
  it('normalizes legacy question title and markdown into one stem', () => {
    const schema = parseTaskSchema(JSON.stringify({
      version: 2,
      questions: [
        {
          id: 'q1',
          type: 'short',
          title: '阅读代码',
          markdown: '说明输出结果',
          score: 3,
        },
      ],
    }))

    expect(schema.version).toBe(3)
    expect(schema.questions?.[0].stem).toBe('阅读代码\n\n说明输出结果')
    expect(questionStem(schema.questions![0])).toBe('阅读代码\n\n说明输出结果')
  })

  it('keeps four dimensions and falls back to computing score for legacy data', () => {
    const scores = normalizeDimensionScores(undefined, 4)

    expect(scores).toHaveLength(4)
    expect(scores.find(item => item.dimension === 'COMPUTING')?.maxScore).toBe(4)
    expect(scores.filter(item => item.dimension !== 'COMPUTING').every(item => item.maxScore === 0)).toBe(true)
  })

  it('calculates total score from dimension scores', () => {
    const question: TaskQuestion = {
      id: 'q1',
      type: 'single',
      stem: '选择正确说法',
      required: true,
      options: ['A', 'B'],
      answer: 'A',
      autoGrade: true,
      dimensionScores: [
        { dimension: 'AWARENESS', maxScore: 2 },
        { dimension: 'COMPUTING', maxScore: 5 },
      ],
    }

    expect(questionTotalScore(question)).toBe(7)
  })

  it('formats nullable file sizes consistently', () => {
    expect(formatFileSize(null)).toBe('-')
    expect(formatFileSize(0)).toBe('-')
    expect(formatFileSize(512)).toBe('512 B')
    expect(formatFileSize(1536)).toBe('1.5 KB')
  })
})
