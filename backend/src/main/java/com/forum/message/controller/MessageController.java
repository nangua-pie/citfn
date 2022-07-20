package com.forum.message.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.exception.BusinessException;
import com.forum.common.security.CurrentUser;
import com.forum.message.dto.ConversationVO;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.dto.SendMessageRequest;
import com.forum.message.service.MessageService;
import com.forum.rbac.entity.PermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Private message REST API, matching AGENTS.md section 6.3.
 *
 * <p>The controller only adapts HTTP to service calls. Authorisation comes
 * from {@link CurrentUser} and is enforced again inside the service.
 */
@Validated
@RestController
@RequestMapping("/api/im")
@RequiredArgsConstructor
public class MessageController {

    /** Hard ceiling on one history page, matching the service side clamp. */
    private static final int MAX_PAGE_SIZE = 100;

    private final MessageService messageService;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> listConversations() {
        return ApiResponse.success(messageService.listConversations(CurrentUser.requireUserId()));
    }

    @PostMapping("/conversations/{targetUserId}")
    public ApiResponse<ConversationVO> openConversation(@PathVariable Long targetUserId) {
        return ApiResponse.success(
                messageService.openConversation(CurrentUser.requireUserId(), targetUserId));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationVO> getConversation(@PathVariable Long conversationId) {
        return ApiResponse.success(
                messageService.getConversation(CurrentUser.requireUserId(), conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable Long conversationId) {
        messageService.deleteConversation(CurrentUser.requireUserId(), conversationId);
        return ApiResponse.success();
    }

    /**
     * Cursor based history.
     *
     * <p>{@code beforeId} is exclusive; pass the smallest id already received to
     * walk backwards. Deep offset paging is intentionally not offered.
     *
     * <p>The size bound is checked here rather than relying on the annotation:
     * Spring 5.3 does not apply method validation to controller parameters, so
     * {@code @Max} alone would silently do nothing.
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ImMessageVO>> loadHistory(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) Integer size) {
        if (size == null || size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException.badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return ApiResponse.success(messageService.loadHistory(
                CurrentUser.requireUserId(), conversationId, beforeId, size));
    }

    @PostMapping("/messages")
    public ApiResponse<ImMessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messageService.sendMessage(CurrentUser.requireUserId(), request));
    }

    @PostMapping("/messages/{messageId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long messageId) {
        messageService.markMessageRead(CurrentUser.requireUserId(), messageId);
        return ApiResponse.success();
    }

    @PostMapping("/messages/{messageId}/recall")
    public ApiResponse<Void> recall(@PathVariable Long messageId) {
        messageService.recallMessage(CurrentUser.requireUserId(), messageId);
        return ApiResponse.success();
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(CurrentUser.requireUserId(), messageId);
        return ApiResponse.success();
    }

    @PostMapping("/users/{userId}/block")
    public ApiResponse<Void> block(@PathVariable Long userId,
                                   @RequestParam(required = false) String reason) {
        messageService.blockUser(CurrentUser.requireUserId(), userId, reason);
        return ApiResponse.success();
    }

    @DeleteMapping("/users/{userId}/block")
    public ApiResponse<Void> unblock(@PathVariable Long userId) {
        messageService.unblockUser(CurrentUser.requireUserId(), userId);
        return ApiResponse.success();
    }

    @GetMapping("/users/blocked")
    public ApiResponse<List<Long>> listBlocked() {
        return ApiResponse.success(messageService.listBlockedUsers(CurrentUser.requireUserId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountVO> unreadCount() {
        int unread = messageService.unreadCount(CurrentUser.requireUserId());
        return ApiResponse.success(new UnreadCountVO(unread));
    }

    @GetMapping("/online/{userId}")
    public ApiResponse<OnlineStatusVO> onlineStatus(@PathVariable Long userId) {
        return ApiResponse.success(new OnlineStatusVO(userId, messageService.isOnline(userId)));
    }

    /**
     * Administrator imposed private messaging ban.
     *
     * <p>{@code days} omitted means a permanent ban.
     */
    @PatchMapping("/admin/users/{userId}/restriction")
    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_PRIVATE_MESSAGE_BAN + "')")
    public ApiResponse<RestrictionStatusVO> setRestriction(
            @PathVariable Long userId,
            @RequestParam boolean banned,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String reason) {
        boolean active = messageService.setPrivateMessageBan(
                CurrentUser.requireUserId(), userId, banned, days, reason);
        return ApiResponse.success(new RestrictionStatusVO(userId, active));
    }

    /** Small wrappers so the response shape stays stable. */
    public static class UnreadCountVO {
        private final int unreadCount;

        public UnreadCountVO(int unreadCount) {
            this.unreadCount = unreadCount;
        }

        public int getUnreadCount() {
            return unreadCount;
        }
    }

    public static class OnlineStatusVO {
        private final Long userId;
        private final boolean online;

        public OnlineStatusVO(Long userId, boolean online) {
            this.userId = userId;
            this.online = online;
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isOnline() {
            return online;
        }
    }

    /** Whether a messaging ban is now in force for the user. */
    public static class RestrictionStatusVO {
        private final Long userId;
        private final boolean restricted;

        public RestrictionStatusVO(Long userId, boolean restricted) {
            this.userId = userId;
            this.restricted = restricted;
        }

        public Long getUserId() {
            return userId;
        }

        public boolean isRestricted() {
            return restricted;
        }
    }
}
