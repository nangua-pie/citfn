import { get, post, put } from './http'
import type { TokenResponse, UserProfile } from '@/types'

export interface LoginPayload {
  username: string
  password: string
  code?: string
}

export interface RegisterPayload {
  username: string
  password: string
  email: string
  code: string
  nickname?: string
}

export function login(payload: LoginPayload) {
  return post<TokenResponse>('/api/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return post<TokenResponse>('/api/auth/register', payload)
}

export function sendCode(email: string, scene: 'REGISTER' | 'LOGIN' | 'RESET_PASSWORD') {
  return post<void>('/api/auth/code', { email, scene })
}

export function logout() {
  return post<void>('/api/auth/logout')
}

export function changePassword(currentPassword: string, newPassword: string) {
  return put<void>('/api/auth/password', { currentPassword, newPassword })
}

export function currentProfile() {
  return get<UserProfile>('/api/users/me')
}
