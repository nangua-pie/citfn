import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

export default createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'light',
    themes: {
      // These values are kept in step with the semantic roles in
      // styles/tokens.scss. Vuetify needs concrete hex values here to build its
      // own `--v-*` scale; the app's own styles read the token layer directly.
      light: {
        colors: {
          primary: '#2F6F4F',
          'on-primary': '#FFFFFF',
          secondary: '#4E6354',
          accent: '#2F6F4F',
          error: '#B3261E',
          warning: '#8A5000',
          success: '#2F6F4F',
          info: '#3B6373',
          background: '#F6F8F6',
          surface: '#FFFFFF',
        },
      },
      dark: {
        dark: true,
        colors: {
          primary: '#8ED6A8',
          'on-primary': '#00391C',
          secondary: '#B4CCBB',
          accent: '#8ED6A8',
          error: '#FFB4AB',
          warning: '#FFB868',
          success: '#8ED6A8',
          info: '#A3CCDF',
          background: '#0F1310',
          surface: '#171B18',
        },
      },
    },
  },
  defaults: {
    // MD3 shape scale: 8px controls, 12px containers (section 9).
    VCard: { rounded: 'md', elevation: 0 },
    VBtn: { rounded: 'sm', style: 'text-transform: none; letter-spacing: 0;' },
    VChip: { rounded: 'sm' },
    VAlert: { rounded: 'md' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
    VTextarea: { variant: 'outlined', density: 'comfortable' },
    // 3.0.0-beta.0's VSelect has no `density` prop; setting one here would
    // fall through to the DOM as a stray attribute on every select.
    VSelect: { variant: 'outlined' },
  },
})
