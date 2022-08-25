import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'

// Vuetify's theme is injected, so the composable is exercised against a stand
// in that exposes exactly the two members it touches: the current theme name
// and the theme definitions.
const current = ref('light')
const themes = ref({
  light: { dark: false },
  dark: { dark: true },
})

vi.mock('vuetify', () => ({
  useTheme: () => ({ current, themes }),
}))

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

async function loadComposable() {
  const module = await import('@/composables/useThemeToggle')
  return module.useThemeToggle()
}

describe('useThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear()
    current.value = 'light'
    themes.value = { light: { dark: false }, dark: { dark: true } }
  })

  afterEach(() => {
    restoreMatchMedia()
  })

  it('starts light when nothing is stored and the system expresses no preference', async () => {
    const { current: name, isDark } = await loadComposable()
    expect(name.value).toBe('light')
    expect(isDark.value).toBe(false)
  })

  it('flips the theme and reports the new state', async () => {
    const { current: name, isDark, toggle } = await loadComposable()
    toggle()
    await nextTick()
    expect(name.value).toBe('dark')
    expect(isDark.value).toBe(true)

    toggle()
    await nextTick()
    expect(name.value).toBe('light')
  })

  it('pushes the chosen theme into Vuetify so .v-theme--dark is applied', async () => {
    const { toggle } = await loadComposable()
    expect(current.value).toBe('light')
    toggle()
    await nextTick()
    expect(current.value).toBe('dark')
  })

  it('persists the choice so a reload does not flash the wrong theme', async () => {
    const { toggle } = await loadComposable()
    toggle()
    await nextTick()
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark')
  })

  it('restores a stored choice over the system preference', async () => {
    localStorage.setItem(STORAGE_KEY, 'dark')
    stubMatchMedia(false)

    const { current: name } = await loadComposable()
    expect(name.value).toBe('dark')
  })

  it('follows prefers-color-scheme on a first visit', async () => {
    stubMatchMedia(true)
    const { current: name } = await loadComposable()
    expect(name.value).toBe('dark')
  })

  it('ignores a corrupted stored value instead of trusting it', async () => {
    localStorage.setItem(STORAGE_KEY, 'chartreuse')
    const { current: name } = await loadComposable()
    expect(name.value).toBe('light')
  })
})
