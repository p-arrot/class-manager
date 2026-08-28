import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

function source(path: string) {
  return readFileSync(new URL(path, import.meta.url), 'utf8')
}

describe('theme and responsive regression guards', () => {
  it('keeps grading surfaces on theme variables', () => {
    const content = [
      source('../views/teacher/GradingView.vue'),
      source('../views/teacher/TaskAnalytics.vue'),
      source('../components/grading/WorksheetSubmissionPanel.vue'),
    ].join('\n')

    expect(content).not.toMatch(/background:\s*(#fafaf9|#f5f4f1|rgba\(255,\s*255,\s*255)/i)
    expect(content).toContain('var(--n-card-color)')
    expect(content).toContain('var(--n-text-color')
  })

  it('retains a single-column mobile roster and horizontal table scrolling', () => {
    const roster = source('../components/grading/SubmissionRoster.vue')
    const shell = source('../components/AppShell.vue')

    expect(roster).toContain(':scroll-x="614"')
    expect(roster).toMatch(/@media \(max-width: 720px\)[\s\S]*grid-template-columns: 1fr/)
    expect(shell.match(/(?:width|height): 44px/g)?.length ?? 0).toBeGreaterThanOrEqual(2)
  })

  it('keeps student assessment actions touch friendly on mobile', () => {
    const project = source('../views/student/ProjectView.vue')
    const exam = source('../views/student/ExamView.vue')

    expect(project).toMatch(/\.card-actions\s+:deep\(\.n-button\)\s*\{\s*min-height:\s*44px/)
    expect(exam).toMatch(/\.exam-row\s+:deep\(\.n-button\)\s*\{\s*min-height:\s*44px/)
  })
})
