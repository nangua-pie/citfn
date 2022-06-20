package com.forum.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Envelope pushed to {@code /user/queue/im}.
 *
 * <pre>
 * { "event": "NEW_MESSAGE", "data": { "messageId": 9521, ... } }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImEvent<T> {

    private String event;
    private T data;

    public static <T> ImEvent<T> of(ImEventType type, T data) {
        return new ImEvent<>(type.name(), data);
    }

    /** Payload of {@code UNREAD_COUNT}. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountData {
        private int unreadCount;
    }

    /** Payload of {@code USER_ONLINE} and {@code USER_OFFLINE}. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresenceData {
        private Long userId;
        private boolean online;
    }

    /** Payload of {@code MESSAGE_READ} and {@code MESSAGE_RECALLED}. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageAckData {
        private Long messageId;
        private Long conversationId;
        private LocalDateTime time;
    }
}
