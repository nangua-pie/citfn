import dayjs from 'dayjs'

/**
 * Machine-readable form of a timestamp from the API.
 *
 * The backend serialises its LocalDateTime fields as `MM/dd/yyyy HH:mm:ss`.
 * That is a valid *display* format but not a valid value for the HTML
 * `datetime` attribute, which must be a valid date string, so it cannot be
 * passed through unchanged. Anything that cannot be parsed returns
 * `undefined`, which is what makes Vue omit the attribute entirely rather
 * than emit a wrong one.
 */
export function machineDate(value: string | null | undefined): string | undefined {
  if (!value) {
    return undefined
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.toISOString() : undefined
}
