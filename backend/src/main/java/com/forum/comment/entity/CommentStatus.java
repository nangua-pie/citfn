package com.forum.comment.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Moderation state stored in {@code forum_comment.status}.
 */
@Getter
@AllArgsConstructor
public enum CommentStatus {

    VISIBLE(1),
    PENDING_AUDIT(2),
    DELETED(3);

    private final int code;

    public static CommentStatus of(Integer code) {
        if (code != null) {
            for (CommentStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        return VISIBLE;
    }
}
