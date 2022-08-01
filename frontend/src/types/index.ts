/**
 * Shared API types.
 *
 * These mirror the unified response envelope from AGENTS.md section 7 and the
 * projections returned by the backend, so a change on either side shows up as
 * a type error rather than a runtime surprise.
 *
 * Identifiers are strings, not numbers: they are 64 bit snowflake values that
 * exceed JavaScript's safe integer range, so the backend emits them as JSON
 * strings to keep them exact. Paging counters stay numbers.
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

export interface UserProfile {
  id: string
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  level: number
  points: number
  status: number
  createTime: string
}

export interface Category {
  id: string
  name: string
  description: string | null
  icon: string | null
  sortOrder: number
  postCount: number
  status: number
  createTime: string
}

export interface Post {
  id: string
  categoryId: string
  categoryName?: string | null
  userId: string
  authorNickname?: string | null
  authorAvatarUrl?: string | null
  title: string
  summary: string | null
  coverUrl: string | null
  contentMd?: string
  viewCount: number
  commentCount: number
  likeCount: number
  collectCount: number
  reportCount: number
  isTop: number
  isEssential: number
  status: number
  liked: boolean
  collected: boolean
  tags: string[]
  createTime: string
  updateTime: string
}

export interface Comment {
  id: string
  postId: string
  userId: string
  authorNickname: string | null
  authorAvatarUrl: string | null
  parentId: string | null
  rootId: string | null
  replyUserId: string | null
  replyToNickname: string | null
  content: string
  likeCount: number
  replyCount: number
  liked: boolean
  createTime: string
  replies: Comment[]
}

export interface LikeResult {
  active: boolean
  count: number
}

export interface Conversation {
  id: string
  userId: string
  targetUserId: string
  targetNickname: string | null
  targetAvatarUrl: string | null
  lastMessageId: string | null
  lastMessagePreview: string | null
  lastMessageTime: string | null
  unreadCount: number
  isPinned: number
  targetOnline: boolean
}

export interface MessageAttachment {
  objectKey: string
  url: string
  fileName: string | null
  contentType: string | null
  size: number
}

export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ' | 'RECALLED'
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'

export interface ImMessage {
  messageId: string
  conversationId: string
  senderId: string
  receiverId: string
  messageType: MessageType
  content: string
  extra: string | null
  status: MessageStatus
  sendTime: string
  readTime: string | null
  recallTime: string | null
  attachments: MessageAttachment[] | null
}

export interface Notification {
  id: string
  type: string
  title: string
  content: string | null
  targetType: string | null
  targetId: string | null
  isRead: number
  createTime: string
}

export interface UploadedFile {
  id: string
  originalName: string
  url: string
  contentType: string | null
  size: number
  createTime: string
}

export type ImEventType =
  | 'NEW_MESSAGE'
  | 'MESSAGE_READ'
  | 'MESSAGE_RECALLED'
  | 'UNREAD_COUNT'
  | 'USER_ONLINE'
  | 'USER_OFFLINE'

export interface ImEvent<T = unknown> {
  event: ImEventType
  data: T
}
