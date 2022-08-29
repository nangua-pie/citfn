import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

import { useThemeToggle } from '@/composables/useThemeToggle'

const STORAGE_KEY = 'forum-theme'

// vitest 0.14 has no `unstubAllGlobals`, so the media query shim is installed
// and removed by hand.
let originalMatchMedia: typeof window.matchMedia | undefined

function stubMatchMedia(matches: boolean) {
  originalMatchMedia = window.matchMedia
  window.matchMedia = (() => ({ matches })) as unknown as typeof window.matchMedia
}

function restoreMatchMedia() {
  if (originalMatchMedia) {
    window.matchMedia = originalMatchMedia
    originalMatchMedia = undefined
  }
}

describe('useThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('flips between light and dark and reports the state', async () => {
    const { themeName, isDark, toggle, set } = useThemeToggle()
    set('light')

    toggle()
    await nextTick()
    expect(themeName.value).toBe('dark')
    expect(isDark.value).toBe(true)

    toggle()
    await nextTick()
    expect(themeName.value).toBe('light')
    expect(isDark.value).toBe(false)
  })

  it('persists the choice so a reload does not flash the wrong theme', async () => {
    const { set } = useThemeToggle()
    set('light')

    useThemeToggle().toggle()
    await nextTick()
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark')
  })

  it('round-trips through the stored value', async () => {
    const { themeName, set } = useThemeToggle()
    set('dark')
    await nextTick()
    expect(themeName.value).toBe('dark')
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark')
  })

  it('follows prefers-color-scheme on a first visit', async () => {
    // The module reads storage once when imported, so the preference has to be
    // in place before the module is loaded.
    restoreMatchMedia()
    stubMatchMedia(true)
    vi.resetModules()
    try {
      const fresh = await import('@/composables/useThemeToggle')
      expect(fresh.useThemeToggle().themeName.value).toBe('dark')
    } finally {
      restoreMatchMedia()
      vi.resetModules()
    }
  })

  it('prefers a stored choice over the system preference', async () => {
    localStorage.setItem(STORAGE_KEY, 'light')
    restoreMatchMedia()
    stubMatchMedia(true)
    vi.resetModules()
    try {
      const fresh = await import('@/composables/useThemeToggle')
      expect(fresh.useThemeToggle().themeName.value).toBe('light')
    } finally {
      restoreMatchMedia()
      vi.resetModules()
    }
  })

  it('ignores a corrupted stored value instead of trusting it', async () => {
    localStorage.setItem(STORAGE_KEY, 'chartreuse')
    restoreMatchMedia()
    stubMatchMedia(false)
    vi.resetModules()
    try {
      const fresh = await import('@/composables/useThemeToggle')
      expect(fresh.useThemeToggle().themeName.value).toBe('light')
    } finally {
      restoreMatchMedia()
      vi.resetModules()
    }
  })
})