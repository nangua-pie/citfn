package com.forum.message.service.impl;

import com.forum.message.dto.ImEvent;
import com.forum.message.dto.ImEventType;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.service.MessagePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * STOMP push implementation.
 *
 * <p>Each push targets {@code /user/queue/im}, which Spring resolves to the
 * session whose principal matches the user id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushServiceImpl implements MessagePushService {

    /** Personal destination, fixed by AGENTS.md section 6.4. */
    private static final String DESTINATION = "/queue/im";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void pushNewMessage(Long receiverId, ImMessageVO message) {
        push(receiverId, ImEvent.of(ImEventType.NEW_MESSAGE, message));
    }

    @Override
    public void pushMessageRead(Long targetUserId, Long messageId, Long conversationId) {
        push(targetUserId, ImEvent.of(ImEventType.MESSAGE_READ,
                new ImEvent.MessageAckData(messageId, conversationId, LocalDateTime.now())));
    }

    @Override
    public void pushMessageRecalled(Long userId, Long messageId, Long conversationId) {
        push(userId, ImEvent.of(ImEventType.MESSAGE_RECALLED,
                new ImEvent.MessageAckData(messageId, conversationId, LocalDateTime.now())));
    }

    @Override
    public void pushUnreadCount(Long userId, int unreadCount) {
        push(userId, ImEvent.of(ImEventType.UNREAD_COUNT, new ImEvent.UnreadCountData(unreadCount)));
    }

    @Override
    public void pushOnlineState(Long userId, boolean online) {
        ImEventType type = online ? ImEventType.USER_ONLINE : ImEventType.USER_OFFLINE;
        push(userId, ImEvent.of(type, new ImEvent.PresenceData(userId, online)));
    }

    @Override
    public void push(Long userId, ImEvent<?> event) {
        if (userId == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), DESTINATION, event);
        } catch (RuntimeException ex) {
            // A push failure must not surface to the caller: the message is
            // already persisted and the client can recover it by polling.
            log.warn("failed to push {} event to user {}: {}", event.getEvent(), userId, ex.getMessage());
        }
    }
}
