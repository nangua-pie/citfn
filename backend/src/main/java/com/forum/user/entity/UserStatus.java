package com.forum.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Account state stored in {@code sys_user.status}.
 *
 * <p>A muted account can still read and post is restricted: see
 * {@link #canPost()} and {@link #canSendPrivateMessage()}.
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    /** Fully active. */
    ACTIVE(1),
    /** Cannot publish new posts or send private messages, existing content stays visible. */
    MUTED(2),
    /** Cannot log in. */
    BANNED(3);

    private final int code;

    public static UserStatus of(Integer code) {
        if (code != null) {
            for (UserStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        return ACTIVE;
    }

    public boolean canPost() {
        return this == ACTIVE;
    }

    public boolean canSendPrivateMessage() {
        // A muted account is muted from messaging as well as publishing; only a
        // fully active account may send, matching the javadoc above.
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this != BANNED;
    }
}
