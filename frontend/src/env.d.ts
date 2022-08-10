/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_WS_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// sockjs-client ships no types for its pre-built browser bundle, which is the
// entry the app imports on purpose; see src/utils/ws.ts.
declare module 'sockjs-client/dist/sockjs' {
  interface SockJSOptions {
    url: string
    protocols?: string[] | null
    sessionId?: string | null
    [key: string]: unknown
  }

  class SockJS {
    constructor(url: string, options?: SockJSOptions)
    static readonly CONNECTING: 0
    static readonly OPEN: 1
    static readonly CLOSING: 2
    static readonly CLOSED: 3
    readonly readyState: 0 | 1 | 2 | 3
    onopen: ((event: unknown) => void) | null
    onmessage: ((event: { data: unknown }) => void) | null
    onclose: ((event: unknown) => void) | null
    onerror: ((event: unknown) => void) | null
    send(data: string): void
    close(code?: number, reason?: string): void
  }

  export default SockJS
}

// marked 4.0.16 and dompurify 2.3.10 ship no type declarations of their own,
// and the DefinitelyTyped packages for them postdate this project's dependency
// cutoff. Only the surface utils/markdown.ts actually uses is declared here.
declare module 'marked' {
  export interface MarkedOptions {
    gfm?: boolean
    breaks?: boolean
    headerIds?: boolean
    mangle?: boolean
    baseUrl?: string | null
    renderer?: unknown
  }

  export const marked: {
    setOptions(options: MarkedOptions): void
    parse(src: string): string
    parseInline(src: string): string
  }
}

declare module 'dompurify' {
  export interface DomPurifyConfig {
    ALLOWED_TAGS?: string[]
    ALLOWED_ATTR?: string[]
    ALLOW_DATA_ATTR?: boolean
    ALLOW_ARIA_ATTR?: boolean
    KEEP_CONTENT?: boolean
    RETURN_TRUSTED_TYPE?: boolean
  }

  const DOMPurify: {
    sanitize(dirty: string, config?: DomPurifyConfig): string
    addHook(...args: unknown[]): void
    removeAllHooks(): void
    isValidAttribute(tag: string, attribute: string, value: string): boolean
  }

  export default DOMPurify
}
