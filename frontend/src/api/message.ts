import { del, get, post } from './http'
import type { Conversation, ImMessage, MessageType } from '@/types'

export interface SendMessagePayload {
  conversationId?: string
  receiverId: string
  messageType: MessageType
  content: string
  extra?: string
}

export function listConversations() {
  return get<Conversation[]>('/api/im/conversations')
}

export function openConversation(targetUserId: string) {
  return post<Conversation>(`/api/im/conversations/${targetUserId}`)
}

export function getConversation(conversationId: string) {
  return get<Conversation>(`/api/im/conversations/${conversationId}`)
}

export function deleteConversation(conversationId: string) {
  return del<void>(`/api/im/conversations/${conversationId}`)
}

/**
 * History page using an id cursor.
 *
 * `beforeId` is exclusive; pass the smallest id already loaded to walk
 * backwards. Offset paging is deliberately not offered by the API.
 */
export function listMessages(conversationId: string, beforeId?: string | null, size = 30) {
  return get<ImMessage[]>(`/api/im/conversations/${conversationId}/messages`, {
    params: { beforeId: beforeId ?? undefined, size },
  })
}

export function sendMessage(payload: SendMessagePayload) {
  return post<ImMessage>('/api/im/messages', payload)
}

export function markMessageRead(messageId: string) {
  return post<void>(`/api/im/messages/${messageId}/read`)
}

export function recallMessage(messageId: string) {
  return post<void>(`/api/im/messages/${messageId}/recall`)
}

export function deleteMessage(messageId: string) {
  return del<void>(`/api/im/messages/${messageId}`)
}

export function blockUser(userId: string, reason?: string) {
  return post<void>(`/api/im/users/${userId}/block`, undefined, { params: { reason } })
}

export function unblockUser(userId: string) {
  return del<void>(`/api/im/users/${userId}/block`)
}

export function listBlockedUserIds() {
  return get<string[]>('/api/im/users/blocked')
}

export function unreadCount() {
  return get<{ unreadCount: number }>('/api/im/unread-count')
}

export function onlineStatus(userId: string) {
  return get<{ userId: string; online: boolean }>(`/api/im/online/${userId}`)
}
