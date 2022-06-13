package com.forum.notification.entity;

/**
 * Categories of in-app notification.
 */
public enum NotificationType {

    COMMENT,
    REPLY,
    LIKE,
    SYSTEM,
    REPORT_RESULT;

    public static NotificationType of(String value) {
        if (value != null) {
            for (NotificationType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("unsupported notification type: " + value);
    }
}
