import axios, { AxiosError, AxiosInstance, AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

/**
 * Application code values from AGENTS.md section 7.
 */
export const ResultCode = {
  SUCCESS: 0,
  BAD_REQUEST: 40000,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
  NOT_FOUND: 40400,
  CONFLICT: 40900,
  TOO_MANY_REQUESTS: 42900,
  INTERNAL_ERROR: 50000,
} as const

const TOKEN_KEY = 'forum.accessToken'
const REFRESH_KEY = 'forum.refreshToken'

export const tokenStorage = {
  accessToken: () => localStorage.getItem(TOKEN_KEY),
  refreshToken: () => localStorage.getItem(REFRESH_KEY),
  set(accessToken: string, refreshToken: string) {
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_KEY, refreshToken)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}

/** Error carrying the application code so callers can branch on it. */
export class ApiError extends Error {
  readonly code: number
  readonly traceId: string | null

  constructor(code: number, message: string, traceId: string | null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

type UnauthorizedHandler = () => void

let onUnauthorized: UnauthorizedHandler = () => undefined

/** Registered by the auth store so a 401 clears the session exactly once. */
export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  onUnauthorized = handler
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 20000,
})

instance.interceptors.request.use((config) => {
  const token = tokenStorage.accessToken()
  if (token) {
    // Axios types `headers` as optional on an outgoing config, so the token is
    // merged into a fresh object rather than assigned through it.
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` }
  }
  return config
})

instance.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const payload = error.response?.data
    if (error.response) {
      const code = payload?.code ?? ResultCode.INTERNAL_ERROR
      const message = payload?.message ?? error.message
      if (code === ResultCode.UNAUTHORIZED) {
        onUnauthorized()
      }
      return Promise.reject(new ApiError(code, message, payload?.traceId ?? null))
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError(ResultCode.INTERNAL_ERROR, 'the request timed out', null))
    }
    return Promise.reject(new ApiError(ResultCode.INTERNAL_ERROR, 'the server is unreachable', null))
  },
)

/**
 * Unwraps the unified envelope so callers receive `data` directly.
 *
 * A non-zero code is treated as a failure even on HTTP 200, because the backend
 * always answers with the same shape.
 */
async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  const body = response.data
  if (body.code !== ResultCode.SUCCESS) {
    throw new ApiError(body.code, body.message, body.traceId)
  }
  return body.data
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(instance.get(url, config))
}

export function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(instance.post(url, body, config))
}

export function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(instance.put(url, body, config))
}

export function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(instance.patch(url, body, config))
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return unwrap<T>(instance.delete(url, config))
}

/** Multipart upload; the envelope is unwrapped the same way. */
export function upload<T>(url: string, form: FormData): Promise<T> {
  return unwrap<T>(
    instance.post(url, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  )
}

export default instance
