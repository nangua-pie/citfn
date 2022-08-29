import { computed, ref } from 'vue'

/**
 * Light / dark theme state.
 *
 * Vuetify 3.0.0-beta.0 offers no way to set the theme from inside the app.
 * `useTheme()` returns the object `VApp` re-provides to its children, in
 * which `current` has been replaced by a getter-only computed:
 *
 *   const newTheme = { ...theme, current, themeClasses }   // computed, read only
 *   provide(ThemeSymbol, newTheme)
 *
 * Writing to it throws "computed value is readonly", so the earlier toggle
 * silently did nothing. The supported route is the `theme` prop on `v-app`:
 * `provideTheme` reads `props.theme` when it is set, so binding it to reactive
 * state drives the `.v-theme--*` class on the shell.
 *
 * This module owns that state; `App.vue` binds it and the header buttons
 * change it. MDesign.md section 3 requires both themes.
 */

export type ThemeName = 'light' | 'dark'

const STORAGE_KEY = 'forum-theme'

function preferredTheme(): ThemeName {
  // matchMedia is missing in some non-browser environments; resolve it to a
  // value first so an absent implementation reads as "no preference".
  const query = window.matchMedia?.('(prefers-color-scheme: dark)')
  return query?.matches ? 'dark' : 'light'
}

function initialTheme(): ThemeName {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'light' || stored === 'dark') {
    return stored
  }
  return preferredTheme()
}

/*
 * Module scope on purpose: the app shell and every header button must observe
 * one value, and there is exactly one shell per document.
 */
const themeName = ref<ThemeName>(initialTheme())

export function useThemeToggle() {
  function set(name: ThemeName) {
    themeName.value = name
    localStorage.setItem(STORAGE_KEY, name)
  }

  function toggle() {
    set(themeName.value === 'dark' ? 'light' : 'dark')
  }

  return {
    /** Bind to `<v-app :theme="themeName">`. */
    themeName,
    isDark: computed(() => themeName.value === 'dark'),
    set,
    toggle,
  }
}