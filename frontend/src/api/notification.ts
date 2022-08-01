import { get, post } from './http'
import type { Notification, PageResult } from '@/types'

export function listNotifications(unreadOnly = false, page = 1, size = 20) {
  return get<PageResult<Notification>>('/api/notifications', {
    params: { unreadOnly, page, size },
  })
}

export function notificationUnreadCount() {
  return get<number>('/api/notifications/unread-count')
}

export function markNotificationRead(id: string) {
  return post<void>(`/api/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return post<void>('/api/notifications/read-all')
}
