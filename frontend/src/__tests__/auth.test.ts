import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: () => { store = {} },
  }
})()
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock })

// Mock the auth API module
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
}))

import { login as loginApi } from '@/api/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('starts unauthenticated', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.role).toBeNull()
    expect(auth.token).toBeNull()
  })

  it('login succeeds and stores token', async () => {
    const mockResponse = {
      token: 'jwt-test-token-123',
      userId: 1,
      username: 'admin',
      name: '管理员',
      role: 'admin',
      classId: null,
    }
    vi.mocked(loginApi).mockResolvedValueOnce(mockResponse)

    const auth = useAuthStore()
    const result = await auth.login('admin', 'admin123')

    expect(result).toEqual(mockResponse)
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.role).toBe('admin')
    expect(auth.token).toBe('jwt-test-token-123')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('token', 'jwt-test-token-123')
    expect(localStorageMock.setItem).toHaveBeenCalledWith('userInfo', JSON.stringify(mockResponse))
  })

  it('logout clears state', () => {
    const auth = useAuthStore()
    // Manually set state
    auth.$patch({
      token: 'some-token',
      userInfo: { token: 'x', userId: 1, username: 'a', name: 'A', role: 'admin', classId: null },
    })

    auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.token).toBeNull()
    expect(auth.userInfo).toBeNull()
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('token')
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('userInfo')
  })

  it('restores userInfo from localStorage on create', () => {
    const saved = { token: 'saved-token', userId: 2, username: 'teacher1', name: '张老师', role: 'teacher', classId: 1 }
    localStorageMock.setItem('token', 'saved-token')
    localStorageMock.setItem('userInfo', JSON.stringify(saved))

    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.role).toBe('teacher')
    expect(auth.userInfo?.name).toBe('张老师')
  })

  it('handles corrupt localStorage gracefully', () => {
    localStorageMock.setItem('userInfo', '{bad json')

    // Should not throw during construction
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
  })
})
