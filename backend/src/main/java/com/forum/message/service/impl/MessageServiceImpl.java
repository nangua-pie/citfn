package com.forum.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.forum.common.exception.BusinessException;
import com.forum.common.util.RedisKeys;
import com.forum.message.dto.ConversationVO;
import com.forum.message.dto.ImMessageAttachmentVO;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.dto.SendMessageRequest;
import com.forum.message.entity.ImBlock;
import com.forum.message.entity.ImConversation;
import com.forum.message.entity.ImMessage;
import com.forum.message.entity.ImMessageAttachment;
import com.forum.message.entity.ImUserRestriction;
import com.forum.message.entity.MessageStatus;
import com.forum.message.entity.MessageType;
import com.forum.message.mapper.ImBlockMapper;
import com.forum.message.mapper.ImConversationMapper;
import com.forum.message.mapper.ImMessageAttachmentMapper;
import com.forum.message.mapper.ImMessageMapper;
import com.forum.message.mapper.ImUserRestrictionMapper;
import com.forum.message.service.MessagePushService;
import com.forum.message.service.MessageService;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Private messaging.
 *
 * <p>Authorisation is enforced here rather than in the controller: every
 * method takes the acting user id and derives the conversation from the
 * sender/receiver pair, so a caller can never address someone else's thread.
 * Message bodies are never written to a log.
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    /** Window in which an identical message is treated as a client retry. */
    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(10);

    /** Preview length stored on the conversation row. */
    private static final int PREVIEW_LENGTH = 60;

    /** Conversation list cache lifetime; short, because presence changes often. */
    private static final Duration CONVERSATION_CACHE_TTL = Duration.ofSeconds(60);

    /** Used only to (de)serialize the conversation cache. */
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .findAndRegisterModules();

    private final ImConversationMapper conversationMapper;
    private final ImMessageMapper messageMapper;
    private final ImMessageAttachmentMapper attachmentMapper;
    private final ImBlockMapper blockMapper;
    private final ImUserRestrictionMapper restrictionMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final MessagePushService pushService;
    private final SensitiveWordService sensitiveWordService;

    private final int maxTextLength;
    private final int sendLimitPerMinute;
    private final long recallWindowSeconds;

    public MessageServiceImpl(ImConversationMapper conversationMapper,
                              ImMessageMapper messageMapper,
                              ImMessageAttachmentMapper attachmentMapper,
                              ImBlockMapper blockMapper,
                              ImUserRestrictionMapper restrictionMapper,
                              UserMapper userMapper,
                              StringRedisTemplate redisTemplate,
                              MessagePushService pushService,
                              SensitiveWordService sensitiveWordService,
                              @Value("${forum.message.max-text-length:2000}") int maxTextLength,
                              @Value("${forum.message.send-limit-per-minute:30}") int sendLimitPerMinute,
                              @Value("${forum.message.recall-window-seconds:120}") long recallWindowSeconds) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.attachmentMapper = attachmentMapper;
        this.blockMapper = blockMapper;
        this.restrictionMapper = restrictionMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.pushService = pushService;
        this.sensitiveWordService = sensitiveWordService;
        this.maxTextLength = maxTextLength;
        this.sendLimitPerMinute = sendLimitPerMinute;
        this.recallWindowSeconds = recallWindowSeconds;
    }

    @Override
    public List<ConversationVO> listConversations(Long currentUserId) {
        // AGENTS.md 6.5 keeps a recent conversation cache in Redis; the
        // conversation list is read on every message page load but changes far
        // less often than it is read.
        String key = RedisKeys.imConversation(currentUserId);
        List<ConversationVO> cached = readCachedConversations(key);
        if (cached != null) {
            return cached;
        }

        List<ImConversation> conversations = conversationMapper.selectListByUser(currentUserId);
        if (conversations.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, User> peers = loadPeers(conversations, currentUserId);
        List<ConversationVO> result = new ArrayList<>(conversations.size());
        for (ImConversation conversation : conversations) {
            result.add(toConversationVO(conversation, peers.get(conversation.getTargetUserId())));
        }
        writeCachedConversations(key, result);
        return result;
    }

    @Override
    @Transactional
    public ConversationVO openConversation(Long currentUserId, Long targetUserId) {
        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            throw BusinessException.badRequest("cannot start a conversation with yourself");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw BusinessException.notFound("user not found");
        }
        if (blockMapper.existsBlock(targetUserId, currentUserId)) {
            throw BusinessException.forbidden("this user does not accept private messages");
        }

        ImConversation conversation = conversationMapper.selectByParticipants(currentUserId, targetUserId);
        if (conversation == null) {
            conversation = newConversation(currentUserId, targetUserId);
        } else if (Integer.valueOf(1).equals(conversation.getIsDeleted())) {
            // Reopening restores the caller's own row; the peer's is untouched.
            conversation.setIsDeleted(0);
            conversationMapper.updateById(conversation);
        }
        invalidateConversations(currentUserId);
        return toConversationVO(conversation, target);
    }

    @Override
    public ConversationVO getConversation(Long currentUserId, Long conversationId) {
        ImConversation conversation = requireOwnedConversation(currentUserId, conversationId);
        return toConversationVO(conversation, userMapper.selectById(conversation.getTargetUserId()));
    }

    @Override
    @Transactional
    public void deleteConversation(Long currentUserId, Long conversationId) {
        requireOwnedConversation(currentUserId, conversationId);
        conversationMapper.softDeleteOwned(conversationId, currentUserId);
        invalidateConversations(currentUserId);
    }

    @Override
    public List<ImMessageVO> loadHistory(Long currentUserId, Long conversationId, Long beforeId, Integer size) {
        ImConversation conversation = requireOwnedConversation(currentUserId, conversationId);
        int pageSize = size == null || size < 1 ? 30 : Math.min(size, 100);
        // Each side owns a conversation row, and a message is filed under its
        // sender's row, so both rows have to be read or the reader would only
        // see their own half of the thread.
        ImConversation peerRow =
                conversationMapper.selectByParticipants(conversation.getTargetUserId(), currentUserId);
        Long peerConversationId = peerRow == null ? conversationId : peerRow.getId();
        List<ImMessage> rows =
                messageMapper.selectHistoryBefore(conversationId, peerConversationId, beforeId, pageSize);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        // The query returns newest first for the cursor; hand back ascending.
        Collections.reverse(rows);
        Map<Long, List<ImMessageAttachmentVO>> attachments = loadAttachments(rows);
        List<ImMessageVO> result = new ArrayList<>(rows.size());
        for (ImMessage row : rows) {
            result.add(toMessageVO(row, attachments.get(row.getId())));
        }
        return result;
    }

    @Override
    @Transactional
    public ImMessageVO sendMessage(Long currentUserId, SendMessageRequest request) {
        // The checks below follow the order mandated by AGENTS.md 6.3.

        // 1. The caller is authenticated.
        if (currentUserId == null) {
            throw BusinessException.unauthorized("not logged in");
        }

        // 2. The receiver must exist and be a different account.
        Long receiverId = request.getReceiverId();
        if (receiverId == null || receiverId.equals(currentUserId)) {
            throw BusinessException.badRequest("cannot send a private message to yourself");
        }
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw BusinessException.notFound("receiver not found");
        }

        // 3. The sender must not be banned, and staff may restrict messaging.
        User sender = userMapper.selectById(currentUserId);
        if (sender == null) {
            throw BusinessException.unauthorized("not logged in");
        }
        if (!UserStatus.of(sender.getStatus()).canSendPrivateMessage()) {
            throw BusinessException.forbidden("your account cannot send private messages");
        }
        assertNotRestricted(currentUserId);
        assertNotRestricted(receiverId);

        // 4. A block stops traffic in either direction, so both orientations
        //    are checked: the receiver blocking the sender, and vice versa.
        if (blockMapper.existsBlock(receiverId, currentUserId)
                || blockMapper.existsBlock(currentUserId, receiverId)) {
            throw BusinessException.forbidden("this user does not accept private messages");
        }

        // 5. Payload and type validation.
        MessageType messageType = MessageType.of(request.getMessageType());
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw BusinessException.badRequest("message content must not be blank");
        }
        if (content.length() > maxTextLength) {
            throw BusinessException.badRequest("message must not exceed " + maxTextLength + " characters");
        }
        if (MessageType.SYSTEM.equals(messageType)) {
            // Only the server may mint system messages.
            throw BusinessException.forbidden("system messages cannot be sent by clients");
        }
        content = sensitiveWordService.mask(content);
        assertSendRateLimit(currentUserId);

        // A retried request must not create a second row.
        ImMessage duplicate = messageMapper.selectRecentDuplicate(currentUserId, receiverId, content,
                messageType.name(), LocalDateTime.now().minus(DUPLICATE_WINDOW));
        if (duplicate != null) {
            return toMessageVO(duplicate, null);
        }

        // 6. Persist to MariaDB, the single source of truth.
        ImConversation receiverConversation =
                conversationMapper.selectByParticipants(receiverId, currentUserId);
        if (receiverConversation == null) {
            receiverConversation = newConversation(receiverId, currentUserId);
        }
        ImConversation senderConversation =
                conversationMapper.selectByParticipants(currentUserId, receiverId);
        if (senderConversation == null) {
            senderConversation = newConversation(currentUserId, receiverId);
        }

        LocalDateTime now = LocalDateTime.now();
        ImMessage message = new ImMessage();
        // Filed under the sender's own conversation row; readers query both
        // rows of the pair, so either side sees the whole thread.
        message.setConversationId(senderConversation.getId());
        message.setSenderId(currentUserId);
        message.setReceiverId(receiverId);
        message.setMessageType(messageType.name());
        message.setContent(content);
        message.setExtra(request.getExtra());
        message.setStatus(MessageStatus.SENT.name());
        message.setSendTime(now);
        message.setIsDeleted(0);
        messageMapper.insert(message);

        // 7. Advance both conversation rows.
        String preview = buildPreview(messageType, content);
        conversationMapper.touchLastMessage(receiverConversation.getId(), receiverId,
                message.getId(), preview, now);
        conversationMapper.touchLastMessage(senderConversation.getId(), currentUserId,
                message.getId(), preview, now);
        // Both participants' previews just changed.
        invalidateConversations(receiverId);
        invalidateConversations(currentUserId);

        // 8. Bump the receiver's unread counter.
        conversationMapper.incrementUnread(receiverConversation.getId(), receiverId);
        int unread = conversationMapper.sumUnread(receiverId);
        cacheUnread(receiverId, unread);

        // 9. Push when the receiver is connected; otherwise the unread counter
        //    and the history endpoint carry the delivery.
        if (isOnline(receiverId)) {
            pushService.pushNewMessage(receiverId, toMessageVO(message, null));
            pushService.pushUnreadCount(receiverId, unread);
            messageMapper.update(null, new UpdateWrapper<ImMessage>()
                    .eq("id", message.getId())
                    .set("status", MessageStatus.DELIVERED.name()));
            message.setStatus(MessageStatus.DELIVERED.name());
        }

        // 10. Return the persisted id and status.
        log.info("private message {} sent from user {} to user {}", message.getId(), currentUserId, receiverId);
        return toMessageVO(message, null);
    }

    @Override
    @Transactional
    public void markMessageRead(Long currentUserId, Long messageId) {
        ImMessage message = requireParticipantMessage(currentUserId, messageId);
        if (!message.getReceiverId().equals(currentUserId)) {
            throw BusinessException.forbidden("only the receiver can mark a message as read");
        }
        if (MessageStatus.READ.name().equals(message.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        messageMapper.markSingleRead(messageId, currentUserId, now);
        // The unread badge lives on the reader's own conversation row.
        ImConversation mine = conversationMapper.selectByParticipants(currentUserId, message.getSenderId());
        if (mine != null) {
            conversationMapper.clearUnread(mine.getId(), currentUserId);
        }
        cacheUnread(currentUserId, conversationMapper.sumUnread(currentUserId));
        pushService.pushMessageRead(message.getSenderId(), messageId, message.getConversationId());
    }

    @Override
    @Transactional
    public void markConversationRead(Long currentUserId, Long conversationId) {
        ImConversation conversation = requireOwnedConversation(currentUserId, conversationId);
        LocalDateTime now = LocalDateTime.now();
        ImConversation peer = conversationMapper.selectByParticipants(conversation.getTargetUserId(), currentUserId);
        messageMapper.markConversationRead(conversationId,
                peer == null ? conversationId : peer.getId(), currentUserId, now);
        conversationMapper.clearUnread(conversationId, currentUserId);
        int unread = conversationMapper.sumUnread(currentUserId);
        cacheUnread(currentUserId, unread);
        pushService.pushMessageRead(conversation.getTargetUserId(), conversationId, conversationId);
        pushService.pushUnreadCount(currentUserId, unread);
    }

    @Override
    @Transactional
    public void recallMessage(Long currentUserId, Long messageId) {
        ImMessage message = messageMapper.selectById(messageId);
        if (message == null || Integer.valueOf(1).equals(message.getIsDeleted())) {
            throw BusinessException.notFound("message not found");
        }
        // Only the sender may recall, so recall can never destroy someone
        // else's content.
        if (!message.getSenderId().equals(currentUserId)) {
            throw BusinessException.forbidden("only the sender can recall a message");
        }
        if (MessageStatus.RECALLED.name().equals(message.getStatus())) {
            return;
        }
        LocalDateTime recallTime = LocalDateTime.now();
        if (recallTime.isAfter(message.getSendTime().plusSeconds(recallWindowSeconds))) {
            throw BusinessException.conflict("the recall window has expired");
        }
        messageMapper.markRecalled(messageId, recallTime);
        pushService.pushMessageRecalled(message.getSenderId(), messageId, message.getConversationId());
        pushService.pushMessageRecalled(message.getReceiverId(), messageId, message.getConversationId());
        log.info("private message {} recalled by its sender", messageId);
    }

    @Override
    @Transactional
    public void deleteMessage(Long currentUserId, Long messageId) {
        ImMessage message = requireParticipantMessage(currentUserId, messageId);
        message.setIsDeleted(1);
        messageMapper.updateById(message);
    }

    @Override
    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId, String reason) {
        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            throw BusinessException.badRequest("cannot block yourself");
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw BusinessException.notFound("user not found");
        }
        if (blockMapper.existsBlock(currentUserId, targetUserId)) {
            return;
        }
        blockMapper.insert(ImBlock.of(currentUserId, targetUserId, reason));
        log.info("user {} blocked user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        if (blockMapper.existsBlock(currentUserId, targetUserId)) {
            blockMapper.delete(new QueryWrapper<ImBlock>()
                    .eq("user_id", currentUserId)
                    .eq("blocked_user_id", targetUserId));
        }
    }

    @Override
    public List<Long> listBlockedUsers(Long currentUserId) {
        return blockMapper.selectBlockedUserIds(currentUserId);
    }

    @Override
    public int unreadCount(Long currentUserId) {
        String key = RedisKeys.imUnread(currentUserId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException ex) {
                redisTemplate.delete(key);
            }
        }
        int unread = conversationMapper.sumUnread(currentUserId);
        cacheUnread(currentUserId, unread);
        return unread;
    }

    @Override
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.imOnline(userId)));
    }

    @Override
    @Transactional
    public boolean setPrivateMessageBan(Long operatorId, Long targetUserId, boolean banned,
                                         Integer days, String reason) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw BusinessException.notFound("user not found");
        }
        if (target.getId().equals(operatorId)) {
            throw BusinessException.badRequest("an administrator cannot ban their own messaging");
        }
        ImUserRestriction existing = restrictionMapper.selectByUserId(targetUserId);
        if (!banned) {
            if (existing != null) {
                restrictionMapper.deleteById(existing.getId());
            }
            log.info("private messaging ban lifted from user {} by {}", targetUserId, operatorId);
            return false;
        }

        ImUserRestriction restriction = existing == null ? new ImUserRestriction() : existing;
        restriction.setUserId(targetUserId);
        restriction.setForbidPrivateMessage(1);
        restriction.setReason(reason);
        restriction.setOperatorId(operatorId);
        // A null expiry means the ban does not lift on its own.
        restriction.setExpiresAt(days == null ? null : LocalDateTime.now().plusDays(days));
        restriction.setUpdateTime(LocalDateTime.now());
        if (existing == null) {
            restriction.setCreateTime(LocalDateTime.now());
            restrictionMapper.insert(restriction);
        } else {
            restrictionMapper.updateById(restriction);
        }
        log.info("private messaging ban applied to user {} by {}", targetUserId, operatorId);
        return true;
    }

    // --- helpers -------------------------------------------------------

    /**
     * Loads a conversation only if the caller owns the row.
     *
     * <p>A missing row and someone else's row both raise {@code 40400}: telling
     * them apart would confirm that an id belongs to a different user.
     */
    private ImConversation requireOwnedConversation(Long currentUserId, Long conversationId) {
        ImConversation conversation = conversationMapper.selectOwned(conversationId, currentUserId);
        if (conversation == null) {
            throw BusinessException.notFound("conversation not found");
        }
        return conversation;
    }

    private ImMessage requireParticipantMessage(Long currentUserId, Long messageId) {
        ImMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw BusinessException.notFound("message not found");
        }
        boolean participant = message.getSenderId().equals(currentUserId)
                || message.getReceiverId().equals(currentUserId);
        if (!participant) {
            throw BusinessException.notFound("message not found");
        }
        return message;
    }

    private void assertNotRestricted(Long userId) {
        ImUserRestriction restriction = restrictionMapper.selectByUserId(userId);
        if (restriction != null && restriction.isActiveAt(LocalDateTime.now())) {
            throw BusinessException.forbidden("private messaging is disabled for this account");
        }
    }

    private void assertSendRateLimit(Long userId) {
        String key = RedisKeys.imSendLimit(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > sendLimitPerMinute) {
            throw BusinessException.tooManyRequests("you are sending messages too quickly");
        }
    }

    private void cacheUnread(Long userId, int unread) {
        redisTemplate.opsForValue().set(RedisKeys.imUnread(userId), String.valueOf(unread), Duration.ofHours(6));
    }

    /**
     * Drops the cached conversation list.
     *
     * <p>Called whenever a conversation row changes, so the list is never more
     * than one interaction behind. A cache failure must not break the action
     * that triggered it.
     */
    private void invalidateConversations(Long userId) {
        try {
            redisTemplate.delete(RedisKeys.imConversation(userId));
        } catch (RuntimeException ex) {
            log.debug("failed to invalidate conversation cache for user {}: {}", userId, ex.getMessage());
        }
    }

    private List<ConversationVO> readCachedConversations(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ConversationVO.class));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            // An unreadable cache is simply a miss.
            log.debug("conversation cache read failed: {}", ex.getMessage());
            return null;
        }
    }

    private void writeCachedConversations(String key, List<ConversationVO> conversations) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(conversations),
                    CONVERSATION_CACHE_TTL);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.debug("conversation cache write failed: {}", ex.getMessage());
        }
    }

    private ImConversation newConversation(Long userId, Long targetUserId) {
        ImConversation conversation = new ImConversation();
        conversation.setUserId(userId);
        conversation.setTargetUserId(targetUserId);
        conversation.setUnreadCount(0);
        conversation.setIsPinned(0);
        conversation.setIsDeleted(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private Map<Long, User> loadPeers(List<ImConversation> conversations, Long currentUserId) {
        List<Long> peerIds = new ArrayList<>();
        for (ImConversation conversation : conversations) {
            if (!conversation.getTargetUserId().equals(currentUserId)) {
                peerIds.add(conversation.getTargetUserId());
            }
        }
        Map<Long, User> peers = new HashMap<>();
        if (peerIds.isEmpty()) {
            return peers;
        }
        for (User user : userMapper.selectBatchIds(peerIds)) {
            peers.put(user.getId(), user);
        }
        return peers;
    }

    private Map<Long, List<ImMessageAttachmentVO>> loadAttachments(List<ImMessage> rows) {
        List<Long> ids = new ArrayList<>(rows.size());
        for (ImMessage row : rows) {
            ids.add(row.getId());
        }
        List<ImMessageAttachment> attachments = attachmentMapper.selectByMessageIds(ids);
        Map<Long, List<ImMessageAttachmentVO>> byMessage = new HashMap<>();
        for (ImMessageAttachment attachment : attachments) {
            byMessage.computeIfAbsent(attachment.getMessageId(), key -> new ArrayList<>())
                    .add(ImMessageAttachmentVO.builder()
                            .objectKey(attachment.getObjectKey())
                            .url(attachment.getUrl())
                            .fileName(attachment.getFileName())
                            .contentType(attachment.getContentType())
                            .size(attachment.getSize())
                            .build());
        }
        return byMessage;
    }

    private ConversationVO toConversationVO(ImConversation conversation, User peer) {
        return ConversationVO.builder()
                .id(conversation.getId())
                .userId(conversation.getUserId())
                .targetUserId(conversation.getTargetUserId())
                .targetNickname(peer == null ? null : peer.getNickname())
                .targetAvatarUrl(peer == null ? null : peer.getAvatarUrl())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageTime(conversation.getLastMessageTime())
                .unreadCount(conversation.getUnreadCount())
                .isPinned(conversation.getIsPinned())
                .targetOnline(peer != null && isOnline(peer.getId()))
                .build();
    }

    private ImMessageVO toMessageVO(ImMessage message, List<ImMessageAttachmentVO> attachments) {
        return ImMessageVO.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .extra(message.getExtra())
                .status(message.getStatus())
                .sendTime(message.getSendTime())
                .readTime(message.getReadTime())
                .recallTime(message.getRecallTime())
                .attachments(attachments)
                .build();
    }

    private String buildPreview(MessageType type, String content) {
        String preview = MessageType.IMAGE.equals(type) ? "[image]"
                : MessageType.FILE.equals(type) ? "[file]" : content;
        preview = preview.replaceAll("\\s+", " ").trim();
        return preview.length() <= PREVIEW_LENGTH ? preview : preview.substring(0, PREVIEW_LENGTH);
    }
}
