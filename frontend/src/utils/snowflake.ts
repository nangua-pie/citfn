/**
 * Snowflake identifier helpers.
 *
 * Identifiers are 64 bit values that exceed JavaScript's safe integer range,
 * so the API emits them as JSON strings and they must stay strings end to end.
 *
 * Converting one with `Number()` silently rounds it: 2105906378254192641
 * becomes 2105906378254190000, which addresses a different row or no row at
 * all. Route parameters have the same problem, so `Number(route.params.id)`
 * turns every post, board and profile link into a 404.
 *
 * Ordering has the same trap. `a - b` on two ids coerces both to doubles
 * first, and consecutive snowflakes can collapse onto the same double, so the
 * comparator reports them as equal and the sort quietly misplaces messages.
 * Comparing digit strings is exact for values of the same length, and
 * `compareIds` falls back to length first so mixed widths still order.
 */

/** Matches a decimal identifier, with or without a sign. */
const DECIMAL = /^-?\d+$/

/** True when the value looks like a plain decimal identifier we can order. */
function isDecimal(value: string): boolean {
  return DECIMAL.test(value)
}

/**
 * Orders two identifiers oldest first.
 *
 * Returns a negative number when `a` sorts before `b`, positive when after,
 * and zero when they are the same identifier.
 */
export function compareIds(a: string, b: string): number {
  if (a === b) {
    return 0
  }
  if (isDecimal(a) && isDecimal(b)) {
    // Strip the sign and any leading zeros so the length is meaningful.
    const left = normalizeDecimal(a)
    const right = normalizeDecimal(b)
    if (left.length !== right.length) {
      return left.length - right.length
    }
  }
  return a < b ? -1 : a > b ? 1 : 0
}

/** Reduces a decimal string to a sign-free, non-padded digit run. */
function normalizeDecimal(value: string): string {
  const negative = value.startsWith('-')
  const digits = value.replace(/^[-+]/, '').replace(/^0+(?=\d)/, '')
  return negative ? `-${digits}` : digits
}

/**
 * Reads an identifier out of a route parameter.
 *
 * Router params are `string | string[]`, so the array case is flattened.
 * An absent parameter yields null rather than NaN, so callers can tell "no id
 * in the route" apart from "id zero".
 */
export function routeId(value: string | string[] | undefined): string | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (raw === undefined || raw === null || raw === '') {
    return null
  }
  return raw
}

/** Sorts a copy of the list oldest first, leaving the input untouched. */
export function sortById<T>(items: readonly T[], idOf: (item: T) => string): T[] {
  return [...items].sort((a, b) => compareIds(idOf(a), idOf(b)))
}
