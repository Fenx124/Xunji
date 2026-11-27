import { getStoredToken, setStoredToken } from '@/utils/request'
import type { Result } from '@/types/api'

const API_BASE = '/api/v1'

export interface StreamPostSseCallbacks<T> {
  signal?: AbortSignal
  onEvent: (frame: T) => void
  onError: (message: string, code?: number) => void
  onComplete?: () => void
}

function handleUnauthorized(): void {
  setStoredToken(null)
  localStorage.removeItem('smart_study_user')
  const path = window.location.pathname
  if (!path.startsWith('/login') && !path.startsWith('/register')) {
    window.location.href = `/login?redirect=${encodeURIComponent(path)}`
  }
}

function processSseBuffer(buffer: string, onEvent: (payload: string) => void): string {
  const lines = buffer.split('\n')
  const remainder = lines.pop() ?? ''
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith(':')) continue
    if (trimmed.startsWith('data:')) {
      const payload = trimmed.slice(5).trimStart()
      if (payload && payload !== '[DONE]') {
        onEvent(payload)
      }
    }
  }
  return remainder
}

/**
 * POST 请求消费 SSE（text/event-stream），按行解析 data: JSON 帧。
 */
export async function streamPostSse<T>(
  path: string,
  body: unknown,
  callbacks: StreamPostSseCallbacks<T>,
): Promise<void> {
  const { signal, onEvent, onError, onComplete } = callbacks
  const token = getStoredToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal,
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onComplete?.()
      return
    }
    const message = err instanceof Error ? err.message : '网络异常'
    onError(message)
    return
  }

  const contentType = response.headers.get('content-type') ?? ''

  if (!response.ok || contentType.includes('application/json')) {
    try {
      const json = (await response.json()) as Result<unknown>
      if (json.code === 40101) {
        handleUnauthorized()
      }
      onError(json.message || '请求失败', json.code)
    } catch {
      onError(`请求失败（HTTP ${response.status}）`, response.status)
    }
    return
  }

  if (!response.body) {
    onError('响应体为空')
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let lineBuffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      lineBuffer += decoder.decode(value, { stream: true })
      lineBuffer = processSseBuffer(lineBuffer, (payload) => {
        try {
          const frame = JSON.parse(payload) as T
          onEvent(frame)
        } catch {
          // 忽略无法解析的单帧
        }
      })
    }
    if (lineBuffer.trim()) {
      processSseBuffer(`${lineBuffer}\n`, (payload) => {
        try {
          onEvent(JSON.parse(payload) as T)
        } catch {
          /* noop */
        }
      })
    }
    onComplete?.()
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onComplete?.()
      return
    }
    const message = err instanceof Error ? err.message : '流式读取失败'
    onError(message)
  }
}