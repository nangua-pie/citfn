package com.forum.notification.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.api.PageResult;
import com.forum.common.security.CurrentUser;
import com.forum.notification.dto.NotificationVO;
import com.forum.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification API. Every call is scoped to the signed in user.
 */
@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> list(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        return ApiResponse.success(
                notificationService.list(CurrentUser.requireUserId(), unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount(CurrentUser.requireUserId()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(CurrentUser.requireUserId(), notificationId);
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(CurrentUser.requireUserId());
        return ApiResponse.success();
    }
}
