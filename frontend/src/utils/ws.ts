import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

import { tokenStorage } from '@/api/http'
import type { ImEvent } from '@/types'

/** Private destination, fixed by AGENTS.md section 6.4. */
export const IM_QUEUE = '/user/queue/im'

type EventHandler = (event: ImEvent) => void
type StateHandler = (connected: boolean) => void

/**
 * STOMP over SockJS connection.
 *
 * SockJS is used because it falls back to HTTP streaming where a raw
 * WebSocket upgrade is blocked. The access token travels as a query
 * parameter because SockJS cannot set custom headers on the handshake.
 */
export class ImSocket {
  private client: Client | null = null
  private subscription: StompSubscription | null = null
  private eventHandler: EventHandler | null = null
  private stateHandler: StateHandler | null = null
  private reconnectAttempt = 0
  private manuallyClosed = false

  connect(baseUrl: string) {
    if (this.client) {
      return
    }
    const token = tokenStorage.accessToken()
    if (!token) {
      return
    }
    this.manuallyClosed = false
    const separator = baseUrl.includes('?') ? '&' : '?'
    const url = `${baseUrl}${separator}accessToken=${encodeURIComponent(token)}`

    this.client = new Client({
      webSocketFactory: () => new SockJS(url),
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.reconnectAttempt = 0
        this.subscription = this.client?.subscribe(IM_QUEUE, (frame: IMessage) => {
          this.handleFrame(frame)
        })
        this.stateHandler?.(true)
      },
      onWebSocketClose: () => {
        this.subscription = null
        this.stateHandler?.(false)
      },
      onStompError: () => {
        // The frame usually carries the server's error detail; log it rather
        // than surfacing raw broker text to the user.
        this.stateHandler?.(false)
      },
    })
    this.client.activate()
  }

  private handleFrame(frame: IMessage) {
    if (!frame.body) {
      return
    }
    try {
      const event = JSON.parse(frame.body) as ImEvent
      this.eventHandler?.(event)
    } catch {
      // A malformed frame is ignored: the REST history remains authoritative.
    }
  }

  /** Sends on the application destination used by the socket send endpoint. */
  send(destination: string, body: unknown) {
    if (!this.client?.connected) {
      return false
    }
    this.client.publish({ destination, body: JSON.stringify(body) })
    return true
  }

  onEvent(handler: EventHandler) {
    this.eventHandler = handler
  }

  onStateChange(handler: StateHandler) {
    this.stateHandler = handler
  }

  disconnect() {
    this.manuallyClosed = true
    this.subscription?.unsubscribe()
    this.subscription = null
    this.client?.deactivate()
    this.client = null
    this.reconnectAttempt = 0
  }

  isConnected() {
    return this.client?.connected ?? false
  }

  get attempts() {
    return this.reconnectAttempt
  }
}

export const imSocket = new ImSocket()
