package com.forum.message.service.impl;

import com.forum.common.exception.BusinessException;
import com.forum.common.util.RedisKeys;
import com.forum.message.dto.SendMessageRequest;
import com.forum.message.entity.ImConversation;
import com.forum.message.entity.ImMessage;
import com.forum.message.entity.ImUserRestriction;
import com.forum.message.entity.MessageStatus;
import com.forum.message.mapper.ImBlockMapper;
import com.forum.message.mapper.ImConversationMapper;
import com.forum.message.mapper.ImMessageAttachmentMapper;
import com.forum.message.mapper.ImMessageMapper;
import com.forum.message.mapper.ImUserRestrictionMapper;
import com.forum.message.service.MessagePushService;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the private message service.
 *
 * <p>These cover the ordering rules of AGENTS.md 6.3 and the authorisation
 * requirements of section 11: nobody may read another user's conversation,
 * recall someone else's message, or push a message past a block.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {

    private static final Long SENDER_ID = 12L;
    private static final Long RECEIVER_ID = 25L;
    private static final Long CONVERSATION_ID = 100L;

    @Mock
    private ImConversationMapper conversationMapper;
    @Mock
    private ImMessageMapper messageMapper;
    @Mock
    private ImMessageAttachmentMapper attachmentMapper;
    @Mock
    private ImBlockMapper blockMapper;
    @Mock
    private ImUserRestrictionMapper restrictionMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MessagePushService pushService;
    @Mock
    private SensitiveWordService sensitiveWordService;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageServiceImpl(conversationMapper, messageMapper, attachmentMapper,
                blockMapper, restrictionMapper, userMapper, redisTemplate, pushService,
                sensitiveWordService, 2000, 30, 120);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(sensitiveWordService.mask(anyString())).thenAnswer(call -> call.getArgument(0));
    }

    // --- send ----------------------------------------------------------

    @Test
    @DisplayName("send persists the message and updates both conversation rows")
    void sendPersistsAndAdvancesConversations() {
        stubSendHappyPath();

        service.sendMessage(SENDER_ID, request("TEXT", "hello"));

        ArgumentCaptor<ImMessage> captor = ArgumentCaptor.forClass(ImMessage.class);
        verify(messageMapper).insert(captor.capture());
        ImMessage saved = captor.getValue();
        assertThat(saved.getSenderId()).isEqualTo(SENDER_ID);
        assertThat(saved.getReceiverId()).isEqualTo(RECEIVER_ID);
        assertThat(saved.getStatus()).isEqualTo(MessageStatus.SENT.name());
        assertThat(saved.getContent()).isEqualTo("hello");

        // Both sides of the conversation move forward, so the call happens once
        // per participant rather than once per message.
        verify(conversationMapper, times(2)).touchLastMessage(anyLong(), anyLong(), eq(saved.getId()),
                anyString(), any(LocalDateTime.class));
        verify(conversationMapper).incrementUnread(anyLong(), eq(RECEIVER_ID));
    }

    @Test
    @DisplayName("send rejects a message when the receiver blocked the sender")
    void sendRejectedWhenReceiverBlockedSender() {
        stubSendHappyPath();
        when(blockMapper.existsBlock(RECEIVER_ID, SENDER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("TEXT", "hello")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);

        verify(messageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("send rejects a banned sender before anything is written")
    void sendRejectedWhenSenderBanned() {
        User sender = activeUser(SENDER_ID);
        sender.setStatus(UserStatus.BANNED.getCode());
        when(userMapper.selectById(SENDER_ID)).thenReturn(sender);
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("TEXT", "hello")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);

        verify(messageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("send rejects a muted sender because muted accounts may not message")
    void sendRejectedWhenSenderMuted() {
        User sender = activeUser(SENDER_ID);
        sender.setStatus(UserStatus.MUTED.getCode());
        when(userMapper.selectById(SENDER_ID)).thenReturn(sender);
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("TEXT", "hi")))
                .isInstanceOf(BusinessException.class);

        verify(messageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("send rejects an administrator imposed messaging ban")
    void sendRejectedWhenRestricted() {
        when(userMapper.selectById(SENDER_ID)).thenReturn(activeUser(SENDER_ID));
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));
        ImUserRestriction restriction = new ImUserRestriction();
        restriction.setUserId(SENDER_ID);
        restriction.setForbidPrivateMessage(1);
        restriction.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(restrictionMapper.selectByUserId(SENDER_ID)).thenReturn(restriction);

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("TEXT", "hi")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);
    }

    @Test
    @DisplayName("send rejects a missing receiver")
    void sendRejectedWhenReceiverMissing() {
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("TEXT", "hi")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40400);
    }

    @Test
    @DisplayName("send rejects content longer than the configured limit")
    void sendRejectedWhenContentTooLong() {
        when(userMapper.selectById(SENDER_ID)).thenReturn(activeUser(SENDER_ID));
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));

        SendMessageRequest request = request("TEXT", "x".repeat(2001));
        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40000);
    }

    @Test
    @DisplayName("send refuses a client forged SYSTEM message")
    void sendRejectsSystemMessage() {
        when(userMapper.selectById(SENDER_ID)).thenReturn(activeUser(SENDER_ID));
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, request("SYSTEM", "notice")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);
    }

    @Test
    @DisplayName("an identical message inside the retry window reuses the stored row")
    void sendDeduplicatesRapidRetries() {
        stubSendHappyPath();
        ImMessage duplicate = message(1L, "hello");
        when(messageMapper.selectRecentDuplicate(eq(SENDER_ID), eq(RECEIVER_ID), eq("hello"),
                eq("TEXT"), any(LocalDateTime.class))).thenReturn(duplicate);

        var result = service.sendMessage(SENDER_ID, request("TEXT", "hello"));

        assertThat(result.getMessageId()).isEqualTo(1L);
        verify(messageMapper, never()).insert(any());
    }

    // --- history and authorisation -------------------------------------

    @Test
    @DisplayName("history is refused for a user who does not own the conversation")
    void historyRefusedForNonParticipant() {
        when(conversationMapper.selectOwned(CONVERSATION_ID, SENDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.loadHistory(SENDER_ID, CONVERSATION_ID, null, 30))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                // 40400 rather than 40300 so an id cannot be probed for existence.
                .isEqualTo(40400);

        verify(messageMapper, never()).selectHistoryBefore(anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    @DisplayName("history clamps the page size and returns messages oldest first")
    void historyClampsSizeAndOrdersAscending() {
        when(conversationMapper.selectOwned(CONVERSATION_ID, SENDER_ID))
                .thenReturn(conversation());
        ImMessage newer = message(20L, "second");
        ImMessage older = message(10L, "first");
        when(messageMapper.selectHistoryBefore(CONVERSATION_ID, CONVERSATION_ID, 30L, 100))
                .thenReturn(java.util.Arrays.asList(newer, older));
        when(attachmentMapper.selectByMessageIds(any())).thenReturn(Collections.emptyList());

        var result = service.loadHistory(SENDER_ID, CONVERSATION_ID, 30L, 5000);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessageId()).isEqualTo(10L);
        assertThat(result.get(1).getMessageId()).isEqualTo(20L);
        // 5000 is clamped to the 100 row ceiling.
        verify(messageMapper).selectHistoryBefore(CONVERSATION_ID, CONVERSATION_ID, 30L, 100);
    }

    @Test
    @DisplayName("reading a conversation clears the unread counter for its owner only")
    void markConversationReadClearsOwnUnreadOnly() {
        ImConversation conversation = conversation();
        when(conversationMapper.selectOwned(CONVERSATION_ID, RECEIVER_ID)).thenReturn(conversation);
        when(conversationMapper.sumUnread(RECEIVER_ID)).thenReturn(0);

        service.markConversationRead(RECEIVER_ID, CONVERSATION_ID);

        verify(conversationMapper).clearUnread(CONVERSATION_ID, RECEIVER_ID);
        verify(conversationMapper, never()).clearUnread(eq(CONVERSATION_ID), eq(SENDER_ID));
    }

    // --- recall --------------------------------------------------------

    @Test
    @DisplayName("a recipient cannot recall the sender's message")
    void recallRefusedForNonSender() {
        ImMessage message = message(5L, "mine");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        when(messageMapper.selectById(5L)).thenReturn(message);

        assertThatThrownBy(() -> service.recallMessage(RECEIVER_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);

        verify(messageMapper, never()).markRecalled(anyLong(), any());
    }

    @Test
    @DisplayName("the sender can recall inside the window and both sides are notified")
    void recallSucceedsInsideWindow() {
        ImMessage message = message(5L, "mine");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        message.setSendTime(LocalDateTime.now().minusSeconds(10));
        when(messageMapper.selectById(5L)).thenReturn(message);

        service.recallMessage(SENDER_ID, 5L);

        verify(messageMapper).markRecalled(eq(5L), any(LocalDateTime.class));
        verify(pushService).pushMessageRecalled(SENDER_ID, 5L, message.getConversationId());
        verify(pushService).pushMessageRecalled(RECEIVER_ID, 5L, message.getConversationId());
    }

    @Test
    @DisplayName("recall is refused once the window has expired")
    void recallRefusedAfterWindow() {
        ImMessage message = message(5L, "old");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        message.setSendTime(LocalDateTime.now().minusSeconds(600));
        when(messageMapper.selectById(5L)).thenReturn(message);

        assertThatThrownBy(() -> service.recallMessage(SENDER_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40900);

        verify(messageMapper, never()).markRecalled(anyLong(), any());
    }

    @Test
    @DisplayName("recall retracts the conversation preview on both sides")
    void recallBlanksConversationPreviewForBothParticipants() {
        ImMessage message = message(5L, "mine");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        message.setSendTime(LocalDateTime.now().minusSeconds(10));
        when(messageMapper.selectById(5L)).thenReturn(message);

        service.recallMessage(SENDER_ID, 5L);

        // Each participant owns a conversation row, so the preview has to be
        // rewritten in both orientations or one side keeps seeing the
        // retracted text in the message list.
        verify(conversationMapper).replaceLastMessagePreview(
                SENDER_ID, RECEIVER_ID, 5L, "[message recalled]");
        verify(conversationMapper).replaceLastMessagePreview(
                RECEIVER_ID, SENDER_ID, 5L, "[message recalled]");
    }

    @Test
    @DisplayName("recall drops the cached conversation lists so the preview cannot come back")
    void recallInvalidatesCachedConversationLists() {
        ImMessage message = message(5L, "mine");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        message.setSendTime(LocalDateTime.now().minusSeconds(10));
        when(messageMapper.selectById(5L)).thenReturn(message);

        service.recallMessage(SENDER_ID, 5L);

        // The conversation list is cached, so without these deletes the
        // retracted preview would still be served from Redis until the TTL.
        verify(redisTemplate).delete(RedisKeys.imConversation(SENDER_ID));
        verify(redisTemplate).delete(RedisKeys.imConversation(RECEIVER_ID));
    }

    @Test
    @DisplayName("a stranger cannot delete another user's message")
    void deleteRefusedForStranger() {
        ImMessage message = message(9L, "secret");
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        when(messageMapper.selectById(9L)).thenReturn(message);

        assertThatThrownBy(() -> service.deleteMessage(999L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40400);
    }

    // --- administrator private messaging ban --------------------------------

    @Test
    @DisplayName("an administrator can ban a user from private messaging")
    void adminCanApplyBan() {
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));
        when(restrictionMapper.selectByUserId(RECEIVER_ID)).thenReturn(null);
        when(restrictionMapper.insert(any(ImUserRestriction.class))).thenAnswer(call -> {
            ((ImUserRestriction) call.getArgument(0)).setId(1L);
            return 1;
        });

        boolean active = service.setPrivateMessageBan(SENDER_ID, RECEIVER_ID, true, 7, "spam");

        assertThat(active).isTrue();
        ArgumentCaptor<ImUserRestriction> captor = ArgumentCaptor.forClass(ImUserRestriction.class);
        verify(restrictionMapper).insert(captor.capture());
        assertThat(captor.getValue().getForbidPrivateMessage()).isEqualTo(1);
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("omitting the duration makes the ban permanent")
    void adminBanWithoutDurationIsPermanent() {
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));
        when(restrictionMapper.selectByUserId(RECEIVER_ID)).thenReturn(null);

        service.setPrivateMessageBan(SENDER_ID, RECEIVER_ID, true, null, "permanent");

        ArgumentCaptor<ImUserRestriction> captor = ArgumentCaptor.forClass(ImUserRestriction.class);
        verify(restrictionMapper).insert(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("lifting the ban deletes the existing restriction row")
    void adminCanLiftBan() {
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));
        ImUserRestriction existing = new ImUserRestriction();
        existing.setId(9L);
        existing.setUserId(RECEIVER_ID);
        existing.setForbidPrivateMessage(1);
        when(restrictionMapper.selectByUserId(RECEIVER_ID)).thenReturn(existing);

        assertThat(service.setPrivateMessageBan(SENDER_ID, RECEIVER_ID, false, null, null)).isFalse();

        verify(restrictionMapper).deleteById(9L);
    }

    @Test
    @DisplayName("an administrator cannot ban their own messaging")
    void adminCannotBanSelf() {
        when(userMapper.selectById(SENDER_ID)).thenReturn(activeUser(SENDER_ID));

        assertThatThrownBy(() -> service.setPrivateMessageBan(SENDER_ID, SENDER_ID, true, 1, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40000);

        verify(restrictionMapper, never()).insert(any());
    }

    // --- helpers -------------------------------------------------------

    private void stubSendHappyPath() {
        when(userMapper.selectById(SENDER_ID)).thenReturn(activeUser(SENDER_ID));
        when(userMapper.selectById(RECEIVER_ID)).thenReturn(activeUser(RECEIVER_ID));
        when(blockMapper.existsBlock(RECEIVER_ID, SENDER_ID)).thenReturn(false);
        when(restrictionMapper.selectByUserId(anyLong())).thenReturn(null);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(conversationMapper.selectByParticipants(eq(RECEIVER_ID), eq(SENDER_ID)))
                .thenReturn(receiverConversation());
        when(conversationMapper.selectByParticipants(eq(SENDER_ID), eq(RECEIVER_ID)))
                .thenReturn(senderConversation());
        when(conversationMapper.sumUnread(RECEIVER_ID)).thenReturn(1);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        // MyBatis-Plus assigns the id during insert; emulate that here.
        when(messageMapper.insert(any(ImMessage.class))).thenAnswer(call -> {
            ImMessage message = call.getArgument(0);
            message.setId(9521L);
            return 1;
        });
    }

    private SendMessageRequest request(String type, String content) {
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(RECEIVER_ID);
        request.setMessageType(type);
        request.setContent(content);
        return request;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setNickname("user" + id);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setIsDeleted(0);
        return user;
    }

    private ImConversation conversation() {
        ImConversation conversation = new ImConversation();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(SENDER_ID);
        conversation.setTargetUserId(RECEIVER_ID);
        conversation.setUnreadCount(2);
        conversation.setIsDeleted(0);
        return conversation;
    }

    private ImConversation senderConversation() {
        ImConversation conversation = conversation();
        conversation.setUserId(SENDER_ID);
        conversation.setTargetUserId(RECEIVER_ID);
        return conversation;
    }

    private ImConversation receiverConversation() {
        ImConversation conversation = conversation();
        conversation.setId(200L);
        conversation.setUserId(RECEIVER_ID);
        conversation.setTargetUserId(SENDER_ID);
        return conversation;
    }

    private ImMessage message(Long id, String content) {
        ImMessage message = new ImMessage();
        message.setId(id);
        message.setConversationId(CONVERSATION_ID);
        message.setSenderId(SENDER_ID);
        message.setReceiverId(RECEIVER_ID);
        message.setMessageType("TEXT");
        message.setContent(content);
        message.setStatus(MessageStatus.SENT.name());
        message.setSendTime(LocalDateTime.now());
        message.setIsDeleted(0);
        return message;
    }
}
