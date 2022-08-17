import { computed, ref, watch } from 'vue'
import { useTheme } from 'vuetify'

/**
 * Light / dark theme switch.
 *
 * MDesign.md section 3 requires both themes. The chosen value is kept in
 * localStorage so a reload does not flash the wrong theme, and the first visit
 * follows the operating system's `prefers-color-scheme`.
 */

const STORAGE_KEY = 'forum-theme'

export type ThemeName = 'light' | 'dark'

function readStoredTheme(): ThemeName | null {
  const stored = localStorage.getItem(STORAGE_KEY)
  return stored === 'light' || stored === 'dark' ? stored : null
}

function preferredTheme(): ThemeName {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function useThemeToggle() {
  const theme = useTheme()
  const current = ref<ThemeName>(
    readStoredTheme() ?? (theme.themes.value[theme.current.value]?.dark ? 'dark' : 'light'),
  )

  // Vuetify only stamps `.v-theme--dark` when its own current theme has the
  // `dark` flag. `current` is the *name* of that theme, which is what the
  // local ref below writes to.
  watch(
    current,
    (name) => {
      theme.current.value = name
      localStorage.setItem(STORAGE_KEY, name)
    },
    { immediate: true },
  )

  const isDark = computed(() => current.value === 'dark')

  function toggle() {
    current.value = current.value === 'dark' ? 'light' : 'dark'
  }

  return { current, isDark, toggle }
}
