import { del, get, patch, post, put } from './http'
import type { LikeResult, PageResult, Post } from '@/types'

export interface PostQuery {
  categoryId?: string
  userId?: string
  keyword?: string
  tag?: string
  onlyEssential?: boolean
  page?: number
  size?: number
}

export interface CreatePostPayload {
  categoryId: string
  title: string
  content: string
  coverUrl?: string
  tags?: string[]
}

export function listPosts(query: PostQuery = {}) {
  return get<PageResult<Post>>('/api/posts', { params: query })
}

export function getPost(id: string) {
  return get<Post>(`/api/posts/${id}`)
}

export function createPost(payload: CreatePostPayload) {
  return post<Post>('/api/posts', payload)
}

export function updatePost(id: string, payload: Partial<CreatePostPayload>) {
  return put<Post>(`/api/posts/${id}`, payload)
}

export function deletePost(id: string) {
  return del<void>(`/api/posts/${id}`)
}

export function toggleLike(id: string) {
  return post<LikeResult>(`/api/posts/${id}/like`)
}

export function toggleCollect(id: string) {
  return post<LikeResult>(`/api/posts/${id}/collect`)
}

export function listMyCollections(page = 1, size = 20) {
  return get<PageResult<Post>>('/api/posts/mine/collected', { params: { page, size } })
}

export function setTop(id: string, top: boolean) {
  return patch<void>(`/api/posts/${id}/top`, undefined, { params: { top } })
}

export function setEssential(id: string, essential: boolean) {
  return patch<void>(`/api/posts/${id}/essential`, undefined, { params: { essential } })
}

export function auditPost(id: string, approved: boolean, reason?: string) {
  return patch<void>(`/api/posts/${id}/audit`, undefined, { params: { approved, reason } })
}

export function popularTags(limit = 20) {
  return get<Array<{ tag_name: string; total: number }>>('/api/posts/tags/popular', {
    params: { limit },
  })
}
