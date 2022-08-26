import { describe, expect, it } from 'vitest'

import { machineDate } from '@/utils/date'

describe('machineDate', () => {
  it('turns the backend format into a value valid for the datetime attribute', () => {
    // The exact shape the API sends for every LocalDateTime field.
    const iso = machineDate('10/02/2026 18:20:20')
    expect(iso).toBeDefined()
    expect(iso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
  })

  it('accepts an already machine-readable value unchanged in meaning', () => {
    expect(machineDate('2022-06-20T10:21:00Z')).toMatch(/^2022-06-20T10:21:00/)
  })

  it('returns undefined for missing input so the attribute is omitted', () => {
    expect(machineDate(null)).toBeUndefined()
    expect(machineDate(undefined)).toBeUndefined()
    expect(machineDate('')).toBeUndefined()
  })

  it('returns undefined rather than Invalid Date for unparseable input', () => {
    expect(machineDate('not a date at all')).toBeUndefined()
  })
})
