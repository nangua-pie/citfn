import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  deleteConversation as deleteConversationApi,
  listConversations,
  listMessages,
  markConversationRead as markConversationReadApi,
  markMessageRead as markMessageReadApi,
  openConversation as openConversationApi,
  recallMessage as recallMessageApi,
  sendMessage as sendMessageApi,
  unreadCount,
} from '@/api/message'
import { imSocket } from '@/utils/ws'
import { sortById } from '@/utils/snowflake'
import type { SendMessagePayload } from '@/api/message'
import type { Conversation, ImEvent, ImMessage, MessageType } from '@/types'

/** Sorted oldest first so a transcript reads top to bottom. */
const sortByTime = (messages: ImMessage[]): ImMessage[] => sortById(messages, (m) => m.messageId)

export const useMessageStore = defineStore('message', () => {
  const conversations = ref<Conversation[]>([])
  const messages = ref<ImMessage[]>([])
  const activeConversationId = ref<string | null>(null)
  const unread = ref(0)
  const connected = ref(false)
  const loading = ref(false)
  /** Smallest id currently loaded; the cursor for the next older page. */
  const oldestLoadedId = ref<string | null>(null)
  const hasMore = ref(true)

  const activeConversation = computed(() => conversations.value.find((c) => c.id === activeConversationId.value) ?? null)

  function upsertMessage(message: ImMessage) {
    if (activeConversationId.value !== message.conversationId) {
      return
    }
    if (messages.value.some((m) => m.messageId === message.messageId)) {
      return
    }
    messages.value = sortByTime([...messages.value, message])
    oldestLoadedId.value = messages.value[0]?.messageId ?? null
  }

  function applyEvent(event: ImEvent) {
    switch (event.event) {
      case 'NEW_MESSAGE': {
        upsertMessage(event.data as ImMessage)
        // The pushed badge value is a prompt; the REST total stays authoritative.
        void refreshUnread()
        void loadConversations()
        break
      }
      case 'MESSAGE_RECALLED': {
        const payload = event.data as { messageId: string }
        const target = messages.value.find((m) => m.messageId === payload.messageId)
        if (target) {
          target.status = 'RECALLED'
          target.content = ''
        }
        // The conversation list keeps its own copy of the last message as a
        // preview, so it is reloaded here too. Otherwise a recall arriving
        // over the socket leaves the retracted text in the sidebar until the
        // page happens to be reloaded.
        void loadConversations()
        break
      }
      case 'MESSAGE_READ': {
        const payload = event.data as { messageId: string }
        const target = messages.value.find((m) => m.messageId === payload.messageId)
        if (target) {
          target.status = 'READ'
        }
        break
      }
      case 'UNREAD_COUNT':
        unread.value = (event.data as { unreadCount: number }).unreadCount
        break
      case 'USER_ONLINE':
      case 'USER_OFFLINE': {
        const payload = event.data as { userId: string; online: boolean }
        const peer = conversations.value.find((c) => c.targetUserId === payload.userId)
        if (peer) {
          peer.targetOnline = payload.online
        }
        break
      }
      default:
        break
    }
  }

  function connect(baseUrl: string) {
    imSocket.onEvent(applyEvent)
    imSocket.onStateChange((value) => {
      connected.value = value
    })
    imSocket.connect(baseUrl)
    void refreshUnread()
    void loadConversations()
  }

  function disconnect() {
    imSocket.disconnect()
    connected.value = false
  }

  async function loadConversations() {
    loading.value = true
    try {
      conversations.value = await listConversations()
    } finally {
      loading.value = false
    }
  }

  async function selectConversation(conversationId: string) {
    activeConversationId.value = conversationId
    messages.value = []
    oldestLoadedId.value = null
    hasMore.value = true
    loading.value = true
    try {
      // Opening a thread is what marks it read, so the call is part of
      // selecting it. Zeroing the local row on its own left the server, and
      // so every later reload, still reporting the old count.
      const [firstPage] = await Promise.all([
        listMessages(conversationId, null, 30),
        markConversationReadApi(conversationId),
      ])
      messages.value = sortByTime(firstPage)
      oldestLoadedId.value = messages.value[0]?.messageId ?? null
      // A full page implies older history may sit behind the cursor.
      hasMore.value = firstPage.length === 30
      const conversation = conversations.value.find((c) => c.id === conversationId)
      if (conversation) {
        conversation.unreadCount = 0
      }
      await refreshUnread()
    } finally {
      loading.value = false
    }
  }

  /** Loads one older page using the id cursor. */
  async function loadOlderMessages() {
    if (!activeConversationId.value || !hasMore.value || oldestLoadedId.value === null) {
      return
    }
    loading.value = true
    try {
      const older = await listMessages(activeConversationId.value, oldestLoadedId.value, 30)
      hasMore.value = older.length === 30
      if (older.length > 0) {
        messages.value = sortByTime([...older, ...messages.value])
        oldestLoadedId.value = messages.value[0]?.messageId ?? null
      }
    } finally {
      loading.value = false
    }
  }

  async function openWith(targetUserId: string) {
    const conversation = await openConversationApi(targetUserId)
    const index = conversations.value.findIndex((c) => c.id === conversation.id)
    if (index >= 0) {
      conversations.value[index] = conversation
    } else {
      conversations.value = [conversation, ...conversations.value]
    }
    await selectConversation(conversation.id)
    return conversation
  }

  async function send(receiverId: string, content: string, messageType: MessageType = 'TEXT', extra?: string) {
    const payload: SendMessagePayload = { receiverId, messageType, content, extra }
    // Prefer the socket when connected; fall back to REST so a dropped
    // connection never blocks sending.
    const sentOverSocket = imSocket.send('/app/im.send', payload)
    if (!sentOverSocket) {
      const message = await sendMessageApi(payload)
      messages.value = sortByTime([...messages.value, message])
      oldestLoadedId.value = messages.value[0]?.messageId ?? null
    }
    void loadConversations()
  }

  async function recall(messageId: string) {
    await recallMessageApi(messageId)
    const target = messages.value.find((m) => m.messageId === messageId)
    if (target) {
      target.status = 'RECALLED'
      target.content = ''
    }
  }

  async function markRead(messageId: string) {
    await markMessageReadApi(messageId)
    const target = messages.value.find((m) => m.messageId === messageId)
    if (target) {
      target.status = 'READ'
    }
  }

  async function removeConversation(conversationId: string) {
    await deleteConversationApi(conversationId)
    conversations.value = conversations.value.filter((c) => c.id !== conversationId)
    if (activeConversationId.value === conversationId) {
      activeConversationId.value = null
      messages.value = []
    }
  }

  async function refreshUnread() {
    const result = await unreadCount()
    unread.value = result.unreadCount
  }

  return {
    conversations,
    messages,
    activeConversationId,
    activeConversation,
    unread,
    connected,
    loading,
    hasMore,
    connect,
    disconnect,
    loadConversations,
    selectConversation,
    loadOlderMessages,
    openWith,
    send,
    recall,
    markRead,
    removeConversation,
    refreshUnread,
  }
})
