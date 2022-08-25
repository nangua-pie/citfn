import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'

import ConversationList from '@/views/message/ConversationList.vue'
import { mountWithApp } from '@/test/mount'
import type { Conversation } from '@/types'

function makeConversation(overrides: Partial<Conversation> = {}): Conversation {
  return {
    id: '5001',
    userId: '1',
    targetUserId: '2',
    targetNickname: 'grace',
    targetAvatarUrl: null,
    lastMessageId: '9001',
    lastMessagePreview: 'Are we still on for Thursday?',
    lastMessageTime: '2022-06-20 10:21:00',
    unreadCount: 0,
    isPinned: 0,
    targetOnline: false,
    ...overrides,
  }
}

describe('ConversationList selection', () => {
  it('emits a select on click', async () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: null },
    })
    await wrapper.find('.row').trigger('click')
    expect(wrapper.emitted('select')).toEqual([['5001']])
  })

  it('is reachable with Enter, not only with a pointer', async () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: null },
    })
    await wrapper.find('.row').trigger('keydown.enter')
    expect(wrapper.emitted('select')).toEqual([['5001']])
  })

  it('is reachable with Space, which the row previously ignored', async () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: null },
    })
    await wrapper.find('.row').trigger('keydown.space')
    expect(wrapper.emitted('select')).toEqual([['5001']])
  })

  it('marks the open conversation as current for assistive technology', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: '5001' },
    })
    expect(wrapper.find('.row').attributes('aria-current')).toBe('true')
  })

  it('leaves aria-current off for the rows that are not open', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: '9999' },
    })
    expect(wrapper.find('.row').attributes('aria-current')).toBeUndefined()
  })
})

describe('ConversationList accessibility', () => {
  it('spells out the unread count, which is otherwise a bare badge number', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: {
        conversations: [makeConversation({ unreadCount: 3 })],
        activeId: null,
      },
    })
    const label = wrapper.find('.row').attributes('aria-label') ?? ''
    expect(label).toContain('grace')
    expect(label).toContain('3 unread')
  })

  it('names the delete control per conversation rather than repeating "close"', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: null },
    })
    const label = wrapper.find('button[aria-label]').attributes('aria-label')
    expect(label).toContain('grace')
  })

  it('does not emit a select when the delete control is used', async () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [makeConversation()], activeId: null },
    })
    await wrapper.find('button[aria-label]').trigger('click')
    await nextTick()
    expect(wrapper.emitted('remove')).toEqual([['5001']])
    expect(wrapper.emitted('select')).toBeUndefined()
  })

  it('falls back to readable text when the API has no nickname', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: {
        conversations: [makeConversation({ targetNickname: null, lastMessagePreview: null })],
        activeId: null,
      },
    })
    expect(wrapper.text()).toContain('Unknown user')
  })

  it('shows a way forward when there are no conversations', () => {
    const wrapper = mountWithApp(ConversationList, {
      props: { conversations: [], activeId: null },
    })
    expect(wrapper.text()).toContain('No conversations yet')
    expect(wrapper.text()).toContain('Open a user profile')
  })
})
