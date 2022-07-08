import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { changePassword, currentProfile, login as loginApi, logout as logoutApi, register as registerApi } from '@/api/auth'
import { setUnauthorizedHandler, tokenStorage } from '@/api/http'
import type { LoginPayload, RegisterPayload } from '@/api/auth'
import type { TokenResponse, UserProfile } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => profile.value !== null && tokenStorage.accessToken() !== null)

  function applySession(tokens: TokenResponse) {
    tokenStorage.set(tokens.accessToken, tokens.refreshToken)
    profile.value = tokens.user
  }

  function clear() {
    tokenStorage.clear()
    profile.value = null
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      applySession(await loginApi(payload))
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    loading.value = true
    try {
      applySession(await registerApi(payload))
    } finally {
      loading.value = false
    }
  }

  /**
   * Restores a session after a reload using the stored access token.
   *
   * If the access token has expired the server answers 40100, the interceptor
   * clears the session and the user simply sees the signed-out state.
   */
  async function restoreSession() {
    if (!tokenStorage.accessToken()) {
      return
    }
    try {
      profile.value = await currentProfile()
    } catch {
      clear()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clear()
    }
  }

  async function updatePassword(currentPassword: string, newPassword: string) {
    // Every session is revoked server side, so drop the local tokens too.
    await changePassword(currentPassword, newPassword)
    clear()
  }

  // A 401 anywhere in the app drops the local session exactly once.
  setUnauthorizedHandler(clear)

  return {
    profile,
    loading,
    isAuthenticated,
    login,
    register,
    logout,
    restoreSession,
    updatePassword,
    clear,
  }
})
