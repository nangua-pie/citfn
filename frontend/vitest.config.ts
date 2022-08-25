import { defineConfig } from 'vitest/config'

import viteConfig from './vite.config'

// The component suite runs against the same Vite pipeline the app builds
// with, so the `@` alias and the SFC compiler behave identically in tests and
// in the browser. Only the test block is added here.
export default defineConfig({
  ...viteConfig,
  // vite.config.ts rewrites the bare `global` identifier to `globalThis` so
  // sockjs-client works in a browser bundle. Vitest's own runtime modules are
  // processed through the same pipeline and rely on `global` resolving, so the
  // shim is dropped here. Node and happy-dom both provide a real `global`, so
  // nothing needs rewriting under test.
  define: {},
  test: {
    // jsdom rather than happy-dom: DOMPurify walks the document with
    // `document.createNodeIterator`, which happy-dom 3.x does not implement,
    // so the sanitiser throws under the lighter environment. jsdom 19 is both
    // complete enough for that and inside the project's version window.
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
    // Component styles are not under test; skipping the CSS pipeline keeps the
    // suite fast and independent of the Sass build.
    css: false,
    // Vuetify ships per-component `.css` files. Externalised, Node is handed a
    // raw `.css` import it cannot load; inlining routes it through Vite, which
    // honours the `css: false` setting above.
    deps: {
      inline: ['vuetify'],
    },
    setupFiles: ['./src/test/setup.ts'],
  },
})
