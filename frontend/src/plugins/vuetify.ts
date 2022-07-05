import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

export default createVuetify({
  components,
  directives,
  theme: {
    themes: {
      light: {
        colors: {
          primary: '#2F6F4F',
          secondary: '#5A6472',
          accent: '#2F6F4F',
          error: '#C0392B',
          warning: '#B8860B',
          success: '#2F6F4F',
          info: '#3B6EA5',
          background: '#F6F7F9',
          surface: '#FFFFFF',
        },
      },
    },
  },
  defaults: {
    VCard: { rounded: 'lg' },
    VBtn: { rounded: 'md', style: 'text-transform: none; letter-spacing: 0;' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
    VTextarea: { variant: 'outlined', density: 'comfortable' },
    VSelect: { variant: 'outlined', density: 'comfortable' },
  },
})
