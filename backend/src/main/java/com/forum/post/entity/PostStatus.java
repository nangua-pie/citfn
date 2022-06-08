package com.forum.post.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Moderation state stored in {@code forum_post.status}.
 */
@Getter
@AllArgsConstructor
public enum PostStatus {

    /** Visible to everyone. */
    PUBLISHED(1),
    /** Waiting for an administrator decision, hidden from the public list. */
    PENDING_AUDIT(2),
    /** Audit failed; only the author and staff can see it. */
    REJECTED(3),
    /** Soft deleted by its author or by staff. */
    DELETED(4);

    private final int code;

    public static PostStatus of(Integer code) {
        if (code != null) {
            for (PostStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        return PUBLISHED;
    }

    public boolean isVisibleToPublic() {
        return this == PUBLISHED;
    }
}
