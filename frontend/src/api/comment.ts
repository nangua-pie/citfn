import { del, get, post, put } from './http'
import type { Comment, LikeResult } from '@/types'

export interface CreateCommentPayload {
  postId: string
  parentId?: string | null
  content: string
}

export function listComments(postId: string) {
  return get<Comment[]>(`/api/comments/post/${postId}`)
}

export function createComment(payload: CreateCommentPayload) {
  return post<Comment>('/api/comments', payload)
}

export function updateComment(id: string, content: string) {
  return put<Comment>(`/api/comments/${id}`, { content })
}

export function deleteComment(id: string) {
  return del<void>(`/api/comments/${id}`)
}

export function toggleCommentLike(id: string) {
  return post<LikeResult>(`/api/comments/${id}/like`)
}
