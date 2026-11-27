import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

const TOKEN_KEY = 'smart_study_token'

export const getStoredToken = (): string | null => localStorage.getItem(TOKEN_KEY)

export const setStoredToken = (token: string | null): void => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function handleUnauthorized(): void {
  setStoredToken(null)
  localStorage.removeItem('smart_study_user')
  const path = window.location.pathname
  if (!path.startsWith('/login') && !path.startsWith('/register')) {
    window.location.href = `/login?redirect=${encodeURIComponent(path)}`
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data as Result<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body
      }
      if (body.code === 40101) {
        ElMessage.error(body.message || '登录已失效，请重新登录')
        handleUnauthorized()
        return Promise.reject(body)
      }
      if (body.code === 40301) {
        ElMessage.warning(body.message || '无权限访问')
        return Promise.reject(body)
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return response.data
  },
  (error: AxiosError<Result<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401 || body?.code === 40101) {
      ElMessage.error(body?.message || '未登录或 Token 无效')
      handleUnauthorized()
      return Promise.reject(error)
    }
    if (status === 403 || body?.code === 40301) {
      const raw = typeof body === 'string' ? body : body?.message
      const message = raw === 'Invalid CORS request' ? '页面来源未被后端允许，请用 localhost:5173 或 127.0.0.1:5173 打开' : raw
      ElMessage.warning(message || '无权限访问')
      return Promise.reject(error)
    }
    ElMessage.error(body?.message || error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

export default request