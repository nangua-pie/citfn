import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Where the dev server forwards API, file and socket traffic. Overridable so a
// second backend instance can be used without editing this file.
const apiTarget = process.env.VITE_DEV_API_TARGET || 'http://localhost:8080'

// Vuetify is registered wholesale in src/plugins/vuetify.ts rather than through
// vite-plugin-vuetify: the beta-era plugin's auto-import behaviour changes
// between releases, and an explicit import list keeps the build deterministic.
export default defineConfig({
  plugins: [vue()],
  // sockjs-client references the Node-only `global` identifier, which does not
  // exist in a browser. Without this the bundle throws
  // "ReferenceError: global is not defined" the moment the socket module loads,
  // which takes the whole page down. Mapping it to globalThis fixes both the dev
  // server and the production bundle.
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '@use "@/styles/variables.scss" as *;',
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Keeps the browser same-origin in development, so no CORS preflight.
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
      '/files': {
        target: apiTarget,
        changeOrigin: true,
      },
      '/ws': {
        target: apiTarget,
        changeOrigin: true,
        ws: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
  },
})
