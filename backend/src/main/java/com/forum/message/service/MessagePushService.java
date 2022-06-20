package com.forum.message.service;

import com.forum.message.dto.ImEvent;
import com.forum.message.dto.ImMessageVO;

/**
 * Real-time delivery of private message events.
 *
 * <p>Everything here is best effort: a failure to push never fails the REST
 * call that produced the event, because MariaDB remains the source of truth
 * and the client can recover the same state by polling.
 */
public interface MessagePushService {

    /** Pushes {@code NEW_MESSAGE} to the receiver's private queue. */
    void pushNewMessage(Long receiverId, ImMessageVO message);

    /** Pushes {@code MESSAGE_READ} to the other participant. */
    void pushMessageRead(Long targetUserId, Long messageId, Long conversationId);

    /** Pushes {@code MESSAGE_RECALLED} to both participants. */
    void pushMessageRecalled(Long userId, Long messageId, Long conversationId);

    /** Pushes {@code UNREAD_COUNT} so the badge can update immediately. */
    void pushUnreadCount(Long userId, int unreadCount);

    /** Pushes {@code USER_ONLINE} or {@code USER_OFFLINE}. */
    void pushOnlineState(Long userId, boolean online);

    /** Pushes an arbitrary event, used by tests and system notices. */
    void push(Long userId, ImEvent<?> event);
}
