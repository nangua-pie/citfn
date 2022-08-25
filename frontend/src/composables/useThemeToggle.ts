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
  // matchMedia is missing in some non-browser environments. Resolve it to a
  // value first so an absent implementation reads as "no preference" rather
  // than throwing on the property access.
  const query = window.matchMedia?.('(prefers-color-scheme: dark)')
  return query?.matches ? 'dark' : 'light'
}

export function useThemeToggle() {
  const theme = useTheme()
  // A stored choice wins; otherwise follow the operating system. Vuetify's
  // own current theme is not consulted here because it is whatever the plugin
  // shipped with, which says nothing about what the visitor asked for.
  const current = ref<ThemeName>(readStoredTheme() ?? preferredTheme())

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
