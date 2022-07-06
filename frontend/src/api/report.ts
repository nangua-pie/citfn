import { get, patch, post } from './http'
import type { PageResult } from '@/types'

export interface ReportPayload {
  targetType: 'POST' | 'COMMENT'
  targetId: number
  reasonCode: string
  reasonDetail?: string
}

export interface ReportRecord {
  id: number
  targetType: string
  targetId: number
  reporterId: number
  reporterNickname: string | null
  reasonCode: string
  reasonDetail: string | null
  status: number
  handleUserId: number | null
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

export function handleReport(id: number, accept: boolean, note?: string) {
  return patch<void>(`/api/reports/${id}`, undefined, { params: { accept, note } })
}
