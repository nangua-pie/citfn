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
