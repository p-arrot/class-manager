import http from './request'
import type { LoginRequest, LoginResponse } from '@/types/api'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return http.post('/auth/login', data)
}
