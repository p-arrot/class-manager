import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: attach token
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: unwrap R<T>, handle flat errors from SecurityConfig
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data

    // Handle flat JSON errors from Spring Security (401/403)
    // These come as { code: number, msg: string } without the R<T> wrapper
    if (body && typeof body.code === 'number' && body.code !== 0 && body.data === undefined) {
      if (body.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
        return Promise.reject(new Error(body.msg || '未登录'))
      }
      return Promise.reject(new Error(body.msg || '请求失败'))
    }

    // Normal R<T> response
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      return Promise.reject(new Error(body.msg || '请求失败'))
    }

    // Fallback: return raw data
    return body
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
        return Promise.reject(new Error('未登录或登录已过期'))
      }
      if (status === 403) {
        return Promise.reject(new Error('权限不足'))
      }
      if (data && data.msg) {
        return Promise.reject(new Error(data.msg))
      }
    }
    return Promise.reject(error)
  }
)

export default http
