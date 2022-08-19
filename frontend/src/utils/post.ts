/**
 * Presentation helpers for a post.
 *
 * `Post.status` is the moderation state defined by the backend's PostStatus
 * enum. The list renders a short label for anything that is not PUBLISHED, so
 * a pending or rejected post is never mistaken for a normal one.
 */

/** Mirrors com.forum.post.entity.PostStatus. */
export const POST_STATUS = {
  PUBLISHED: 1,
  PENDING_AUDIT: 2,
  REJECTED: 3,
  DELETED: 4,
} as const

/**
 * The moderation label for a post status code, or `null` when the post is
 * simply published and needs no badge.
 */
export function postStatusLabel(status: number | undefined | null): string | null {
  switch (status) {
    case POST_STATUS.PENDING_AUDIT:
      return 'Pending review'
    case POST_STATUS.REJECTED:
      return 'Rejected'
    case POST_STATUS.DELETED:
      return 'Removed'
    default:
      return null
  }
}

/**
 * The tone a status badge should use. Split out so the template never
 * inlines a colour decision.
 */
export function postStatusTone(status: number | undefined | null): 'warning' | 'error' | 'default' {
  if (status === POST_STATUS.PENDING_AUDIT) {
    return 'warning'
  }
  if (status === POST_STATUS.REJECTED || status === POST_STATUS.DELETED) {
    return 'error'
  }
  return 'default'
}
