package com.forum.notification.service;

import com.forum.common.api.PageResult;
import com.forum.notification.dto.NotificationVO;
import com.forum.notification.entity.NotificationType;

/**
 * In-app notifications and their unread badge.
 */
public interface NotificationService {

    /**
     * Creates a notification, skipping self-notifications and duplicates that
     * would otherwise pile up.
     */
    void notify(Long userId, NotificationType type, String title, String content,
                String targetType, Long targetId);

    /** Notifications for the current user, newest first. */
    PageResult<NotificationVO> list(Long userId, Boolean unreadOnly, Long page, Long size);

    /** Marks one notification read; the caller's own row only. */
    void markRead(Long userId, Long notificationId);

    /** Marks every notification read. */
    void markAllRead(Long userId);

    /** Unread badge count. */
    int unreadCount(Long userId);
}
