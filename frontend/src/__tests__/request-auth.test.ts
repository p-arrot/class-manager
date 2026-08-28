import { describe, expect, it } from 'vitest'
import { shouldAttachAuthToken } from '@/api/request'

describe('request authentication', () => {
  it('does not attach a stale token to the login request', () => {
    expect(shouldAttachAuthToken('/auth/login')).toBe(false)
    expect(shouldAttachAuthToken('/courses')).toBe(true)
  })
})
