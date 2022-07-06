import { del, get, post, put } from './http'
import type { Comment, LikeResult } from '@/types'

export interface CreateCommentPayload {
  postId: number
  parentId?: number | null
  content: string
}

export function listComments(postId: number) {
  return get<Comment[]>(`/api/comments/post/${postId}`)
}

export function createComment(payload: CreateCommentPayload) {
  return post<Comment>('/api/comments', payload)
}

export function updateComment(id: number, content: string) {
  return put<Comment>(`/api/comments/${id}`, { content })
}

export function deleteComment(id: number) {
  return del<void>(`/api/comments/${id}`)
}

export function toggleCommentLike(id: number) {
  return post<LikeResult>(`/api/comments/${id}/like`)
}
