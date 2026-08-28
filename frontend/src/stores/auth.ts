import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '@/types/api'
import { login as loginApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<LoginResponse | null>(null)

  // Restore user info from localStorage on init
  const saved = localStorage.getItem('userInfo')
  if (saved) {
    try {
      userInfo.value = JSON.parse(saved)
    } catch {
      localStorage.removeItem('userInfo')
    }
  }

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role ?? null)

  async function login(account: string, password: string) {
    const data = await loginApi({ account, password })
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  function logout() {
    token.value = null
    userInfo.value = null
    clearAuthStorage()
  }

  return { token, userInfo, isLoggedIn, role, login, logout }
})

export function clearAuthStorage() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}
