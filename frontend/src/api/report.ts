import { get, patch, post } from './http'
import type { PageResult } from '@/types'

export interface ReportPayload {
  targetType: 'POST' | 'COMMENT'
  targetId: string
  reasonCode: string
  reasonDetail?: string
}

export interface ReportRecord {
  id: string
  targetType: string
  targetId: string
  reporterId: string
  reporterNickname: string | null
  reasonCode: string
  reasonDetail: string | null
  status: number
  handleUserId: string | null
  handleTime: string | null
  handleNote: string | null
  createTime: string
}

export function fileReport(payload: ReportPayload) {
  return post<void>('/api/reports', payload)
}

export function listReports(status?: number, page = 1, size = 20) {
  return get<PageResult<ReportRecord>>('/api/reports', {
    params: { status, page, size },
  })
}

export function handleReport(id: string, accept: boolean, note?: string) {
  return patch<void>(`/api/reports/${id}`, undefined, { params: { accept, note } })
}
