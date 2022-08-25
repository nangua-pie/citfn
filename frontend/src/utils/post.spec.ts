import { describe, expect, it } from 'vitest'

import { POST_STATUS, postStatusLabel, postStatusTone } from '@/utils/post'

describe('postStatusLabel', () => {
  it('returns null for a published post, so no badge is rendered', () => {
    expect(postStatusLabel(POST_STATUS.PUBLISHED)).toBeNull()
  })

  it('names each moderation state', () => {
    expect(postStatusLabel(POST_STATUS.PENDING_AUDIT)).toBe('Pending review')
    expect(postStatusLabel(POST_STATUS.REJECTED)).toBe('Rejected')
    expect(postStatusLabel(POST_STATUS.DELETED)).toBe('Removed')
  })

  it('falls back to no badge for an unknown or missing status', () => {
    // An unmapped code must not render a misleading "Unknown" pill.
    expect(postStatusLabel(99)).toBeNull()
    expect(postStatusLabel(undefined)).toBeNull()
    expect(postStatusLabel(null)).toBeNull()
  })
})

describe('postStatusTone', () => {
  it('separates the pending state from the terminal ones', () => {
    expect(postStatusTone(POST_STATUS.PENDING_AUDIT)).toBe('warning')
    expect(postStatusTone(POST_STATUS.REJECTED)).toBe('error')
    expect(postStatusTone(POST_STATUS.DELETED)).toBe('error')
  })

  it('uses the neutral tone for a published or unknown status', () => {
    expect(postStatusTone(POST_STATUS.PUBLISHED)).toBe('default')
    expect(postStatusTone(undefined)).toBe('default')
  })
})
