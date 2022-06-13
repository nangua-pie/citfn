package com.forum.message.entity;

/**
 * Private message payload kinds (AGENTS.md section 6.2).
 */
public enum MessageType {

    TEXT,
    IMAGE,
    FILE,
    /** Emitted by the server, for example when a recall happened. */
    SYSTEM;

    public static MessageType of(String value) {
        if (value != null) {
            for (MessageType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("unsupported message type: " + value);
    }
}
