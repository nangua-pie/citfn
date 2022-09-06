import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import type { Conversation } from '@/types'

const listConversations = vi.fn()
const listMessages = vi.fn()
const markConversationRead = vi.fn()
const unreadCount = vi.fn()

vi.mock('@/api/message', () => ({
  listConversations: () => listConversations(),
  listMessages: (...args: unknown[]) => listMessages(...args),
  markConversationRead: (...args: unknown[]) => markConversationRead(...args),
  unreadCount: () => unreadCount(),
  deleteConversation: vi.fn(),
  openConversation: vi.fn(),
  markMessageRead: vi.fn(),
  recallMessage: vi.fn(),
  sendMessage: vi.fn(),
  blockUser: vi.fn(),
  unblockUser: vi.fn(),
  listBlockedUserIds: vi.fn(),
}))

vi.mock('@/utils/ws', () => ({
  imSocket: { connect: vi.fn(), disconnect: vi.fn() },
}))

function conversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: '5001',
    userId: '1',
    targetUserId: '2',
    targetNickname: 'bob',
    targetAvatarUrl: null,
    lastMessageId: '9001',
    lastMessagePreview: 'hello',
    lastMessageTime: '2022-06-20 10:21:00',
    unreadCount: 0,
    isPinned: 0,
    targetOnline: false,
    ...overrides,
  }
}

/**
 * Loads the store fresh for each test.
 *
 * The store keeps its state in the closure created by defineStore, so a new
 * Pinia per test gives a new instance rather than inheriting the previous
 * one's selection.
 */
async function freshStore() {
  setActivePinia(createPinia())
  const { useMessageStore } = await import('@/stores/message')
  return useMessageStore()
}

describe('loadConversations selection', () => {
  beforeEach(() => {
    vi.resetModules()
    listConversations.mockReset()
    // The cursor endpoint returns a bare array, not a page envelope: the store
    // reads firstPage.length to decide whether older history exists.
    listMessages.mockReset().mockResolvedValue([])
    markConversationRead.mockReset().mockResolvedValue(undefined)
    unreadCount.mockReset().mockResolvedValue(0)
  })

  it('opens the first conversation so the page is usable on arrival', async () => {
    listConversations.mockResolvedValue([conversation(), conversation({ id: '5002' })])
    const store = await freshStore()

    await store.loadConversations()

    // Without this the chat pane renders its empty placeholder, so there is
    // no transcript and no composer at all.
    expect(store.activeConversationId).toBe('5001')
    expect(store.activeConversation?.targetNickname).toBe('bob')
  })

  it('keeps the open conversation when a reload still returns it', async () => {
    listConversations.mockResolvedValue([
      conversation({ id: '5001' }),
      conversation({ id: '5002' }),
    ])
    const store = await freshStore()

    await store.selectConversation('5002')
    await store.loadConversations()

    expect(store.activeConversationId).toBe('5002')
  })

  it('falls back to the first conversation when the open one has gone', async () => {
    listConversations.mockResolvedValue([conversation({ id: '5002' }), conversation({ id: '5003' })])
    const store = await freshStore()

    await store.selectConversation('9999')
    await store.loadConversations()

    expect(store.activeConversationId).toBe('5002')
  })

  it('selects nothing when the inbox is empty', async () => {
    listConversations.mockResolvedValue([])
    const store = await freshStore()

    await store.loadConversations()

    expect(store.activeConversationId).toBeNull()
    expect(store.activeConversation).toBeNull()
  })
})
