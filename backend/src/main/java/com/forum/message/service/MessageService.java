package com.forum.message.service;

import com.forum.message.dto.ConversationVO;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.dto.SendMessageRequest;

import java.util.List;

/**
 * One to one private messaging.
 *
 * <p>Every method takes the acting user explicitly so authorisation is decided
 * in the service layer and can never be skipped by a caller.
 */
public interface MessageService {

    /** Conversation list for the current user, pinned first then most recent. */
    List<ConversationVO> listConversations(Long currentUserId);

    /**
     * Opens or returns the conversation with a peer.
     *
     * <p>Both participants get their own row, so this returns the caller's view.
     */
    ConversationVO openConversation(Long currentUserId, Long targetUserId);

    /** A single conversation, rejected unless the caller participates in it. */
    ConversationVO getConversation(Long currentUserId, Long conversationId);

    /** Hides the conversation for the caller only; the peer keeps theirs. */
    void deleteConversation(Long currentUserId, Long conversationId);

    /**
     * History page, newest first, using an id cursor rather than an offset.
     *
     * @param beforeId exclusive upper bound; {@code null} starts from the newest
     * @param size page size, capped by the service
     */
    List<ImMessageVO> loadHistory(Long currentUserId, Long conversationId, Long beforeId, Integer size);

    /**
     * Persists and delivers a message.
     *
     * <p>Runs the ordered checks of AGENTS.md section 6.3: caller identity,
     * receiver existence, sender status, receiver block, payload validity.
     */
    ImMessageVO sendMessage(Long currentUserId, SendMessageRequest request);

    /** Marks one message read by its receiver. */
    void markMessageRead(Long currentUserId, Long messageId);

    /** Marks every message of a conversation read and clears the badge. */
    void markConversationRead(Long currentUserId, Long conversationId);

    /**
     * Withdraws a message.
     *
     * <p>Only the sender may recall, and only inside the configured window.
     */
    void recallMessage(Long currentUserId, Long messageId);

    /** Soft deletes a message for its sender. */
    void deleteMessage(Long currentUserId, Long messageId);

    /** Blocks a user so no messages can flow in either direction. */
    void blockUser(Long currentUserId, Long targetUserId, String reason);

    /** Lifts a block previously created by the caller. */
    void unblockUser(Long currentUserId, Long targetUserId);

    /** Ids the caller has blocked. */
    List<Long> listBlockedUsers(Long currentUserId);

    /** Total unread badge count, served from Redis with a database fallback. */
    int unreadCount(Long currentUserId);

    /** Whether a user currently has an open socket. */
    boolean isOnline(Long userId);
}
