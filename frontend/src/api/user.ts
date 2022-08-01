import { get, put } from './http'
import type { UserProfile } from '@/types'

export function getProfile(userId: string) {
  return get<UserProfile>(`/api/users/${userId}/profile`)
}

export function updateMyProfile(payload: {
  nickname?: string
  avatarUrl?: string
  bio?: string
}) {
  return put<UserProfile>('/api/users/me', payload)
}
