import { describe, expect, it } from 'vitest'
import { toLocalDateTime } from '@/utils/date'

describe('date utilities', () => {
  it('serializes date picker timestamps without UTC conversion', () => {
    const timestamp = new Date(2026, 5, 30, 23, 59, 30).getTime()

    expect(toLocalDateTime(timestamp)).toBe('2026-06-30T23:59:30')
  })

  it('returns undefined for empty picker values', () => {
    expect(toLocalDateTime(null)).toBeUndefined()
    expect(toLocalDateTime(undefined)).toBeUndefined()
  })
})
