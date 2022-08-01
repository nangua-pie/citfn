import { get } from './http'
import type { Category } from '@/types'

export function listCategories() {
  return get<Category[]>('/api/categories')
}

export function getCategory(id: string) {
  return get<Category>(`/api/categories/${id}`)
}
