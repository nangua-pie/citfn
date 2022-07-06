import { get, upload } from './http'
import type { UploadedFile } from '@/types'

/** Image upload, used for avatars and in-article images. */
export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return upload<UploadedFile>('/api/files/image', form)
}

/** Generic attachment upload, used by file messages. */
export function uploadFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  return upload<UploadedFile>('/api/files', form)
}

export function listMyFiles(limit = 20) {
  return get<UploadedFile[]>('/api/files/mine', { params: { limit } })
}
