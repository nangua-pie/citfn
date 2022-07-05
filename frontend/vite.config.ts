import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Vuetify is registered wholesale in src/plugins/vuetify.ts rather than through
// vite-plugin-vuetify: the beta-era plugin's auto-import behaviour changes
// between releases, and an explicit import list keeps the build deterministic.
export default defineConfig({
  plugins: [vue()],
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
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
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
