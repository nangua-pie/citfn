import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useAuthStore } from '@/stores/auth'
import { currentProfile } from '@/api/auth'
import type { UserProfile } from '@/types'

vi.mock('@/api/auth', () => ({
  currentProfile: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  changePassword: vi.fn(),
}))

const PROFILE: UserProfile = {
  id: '5',
  username: 'ada',
  nickname: 'Ada',
  avatarUrl: null,
  bio: null,
  level: 1,
  points: 0,
  status: 1,
  createTime: '2022-06-20 10:21:00',
}

describe('auth session restore', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(currentProfile).mockReset()
    setActivePinia(createPinia())
  })

  it('asks the server once however many callers wait on it', async () => {
    // The route guard and the app shell both need this, and a reload must not
    // turn into two requests for the same profile.
    localStorage.setItem('forum.accessToken', 'token')
    vi.mocked(currentProfile).mockResolvedValue(PROFILE)
    const auth = useAuthStore()

    await Promise.all([auth.ensureSession(), auth.ensureSession()])
    await auth.ensureSession()

    expect(currentProfile).toHaveBeenCalledTimes(1)
    expect(auth.isAuthenticated).toBe(true)
  })

  it('asks nothing when there is no token to restore', async () => {
    const auth = useAuthStore()

    await auth.ensureSession()

    expect(currentProfile).not.toHaveBeenCalled()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('drops the session when the stored token is no longer accepted', async () => {
    localStorage.setItem('forum.accessToken', 'stale')
    vi.mocked(currentProfile).mockRejectedValue(new Error('40100'))
    const auth = useAuthStore()

    await auth.ensureSession()

    expect(auth.isAuthenticated).toBe(false)
    expect(localStorage.getItem('forum.accessToken')).toBeNull()
  })
})
