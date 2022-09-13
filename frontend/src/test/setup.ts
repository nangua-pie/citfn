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

// Vuetify's layout service observes the app shell with ResizeObserver to work
// out the size of the header and drawers. jsdom 19 has no such API, so the
// first VApp mount throws before any assertion runs.
if (typeof globalThis.ResizeObserver === 'undefined') {
  class ResizeObserverStub {
    observe() {}

    unobserve() {}

    disconnect() {}
  }
  Object.defineProperty(globalThis, 'ResizeObserver', {
    configurable: true,
    writable: true,
    value: ResizeObserverStub,
  })
}

/**
 * jsdom 19 leaves `document.scrollingElement` undefined. Vuetify's overlay
 * walks up to the scroll parent to position a menu and reads `clientWidth` off
 * whatever it finds, so an open menu turns into an unhandled rejection. The
 * document element is what a browser reports here.
 */
if (typeof document !== 'undefined' && !document.scrollingElement) {
  Object.defineProperty(document, 'scrollingElement', {
    configurable: true,
    get: () => document.documentElement,
  })
}

/**
 * jsdom 19 has no Web Animations API. Vuetify's field calls `el.animate`
 * from a watcher when it mounts, so without this every component using a text
 * field throws during mount and the failure surfaces far from its cause.
 */
if (typeof Element !== 'undefined' && !Element.prototype.animate) {
  // Typed as a stub rather than a full Animation: the fields Vuetify reads are
  // present, and nothing under test asserts on the rest.
  Element.prototype.animate = function animate() {
    return {
      finished: Promise.resolve(),
      cancel() {},
      finish() {},
      onfinish: null,
    } as unknown as Animation
  }
}

// Marks this file as a module, so `--isolatedModules` does not treat the
// shim above as a global script.
export {}
