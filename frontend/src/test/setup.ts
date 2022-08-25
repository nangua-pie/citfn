/**
 * Test environment shims.
 *
 * jsdom 19 does not implement the CSS object interface, but Vuetify reads
 * `CSS.supports` at module load to decide whether `:focus-visible` is
 * available. Without this the very first import of a Vuetify component throws
 * a ReferenceError before any test runs.
 */
if (typeof globalThis.CSS === 'undefined') {
  Object.defineProperty(globalThis, 'CSS', {
    configurable: true,
    writable: true,
    value: {
      // Only the query Vuetify actually asks about is answered truthfully;
      // everything else is reported as unsupported, which is what a browser
      // from this project's era would do.
      supports: (query: string) => query === 'selector(:focus-visible)',
      escape: (value: string) => value,
    },
  })
}

// Marks this file as a module, so `--isolatedModules` does not treat the
// shim above as a global script.
export {}
