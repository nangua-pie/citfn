package com.forum.message.entity;

/**
 * Delivery state of a private message (AGENTS.md section 6.2).
 */
public enum MessageStatus {

    /** Persisted and acknowledged to the sender. */
    SENT,
    /** The receiver was online and the push succeeded. */
    DELIVERED,
    /** The receiver opened the conversation. */
    READ,
    /** Withdrawn by its sender inside the recall window. */
    RECALLED;

    public static MessageStatus of(String value) {
        if (value != null) {
            for (MessageStatus status : values()) {
                if (status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("unsupported message status: " + value);
    }
}
