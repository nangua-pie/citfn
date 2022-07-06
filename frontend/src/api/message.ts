import { del, get, post } from './http'
import type { Conversation, ImMessage, MessageType } from '@/types'

export interface SendMessagePayload {
  conversationId?: number
  receiverId: number
  messageType: MessageType
  content: string
  extra?: string
}

export function listConversations() {
  return get<Conversation[]>('/api/im/conversations')
}

export function openConversation(targetUserId: number) {
  return post<Conversation>(`/api/im/conversations/${targetUserId}`)
}

export function getConversation(conversationId: number) {
  return get<Conversation>(`/api/im/conversations/${conversationId}`)
}

export function deleteConversation(conversationId: number) {
  return del<void>(`/api/im/conversations/${conversationId}`)
}

/**
 * History page using an id cursor.
 *
 * `beforeId` is exclusive; pass the smallest id already loaded to walk
 * backwards. Offset paging is deliberately not offered by the API.
 */
export function listMessages(conversationId: number, beforeId?: number | null, size = 30) {
  return get<ImMessage[]>(`/api/im/conversations/${conversationId}/messages`, {
    params: { beforeId: beforeId ?? undefined, size },
  })
}

export function sendMessage(payload: SendMessagePayload) {
  return post<ImMessage>('/api/im/messages', payload)
}

export function markMessageRead(messageId: number) {
  return post<void>(`/api/im/messages/${messageId}/read`)
}

export function recallMessage(messageId: number) {
  return post<void>(`/api/im/messages/${messageId}/recall`)
}

export function deleteMessage(messageId: number) {
  return del<void>(`/api/im/messages/${messageId}`)
}

export function blockUser(userId: number, reason?: string) {
  return post<void>(`/api/im/users/${userId}/block`, undefined, { params: { reason } })
}

export function unblockUser(userId: number) {
  return del<void>(`/api/im/users/${userId}/block`)
}

export function listBlockedUserIds() {
  return get<number[]>('/api/im/users/blocked')
}

export function unreadCount() {
  return get<{ unreadCount: number }>('/api/im/unread-count')
}

export function onlineStatus(userId: number) {
  return get<{ userId: number; online: boolean }>(`/api/im/online/${userId}`)
}
