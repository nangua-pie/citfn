package com.forum.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.api.PageResult;
import com.forum.common.util.Pagination;
import com.forum.notification.dto.NotificationVO;
import com.forum.notification.entity.Notification;
import com.forum.notification.entity.NotificationType;
import com.forum.notification.mapper.NotificationMapper;
import com.forum.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notification persistence.
 *
 * <p>{@link #notify} runs asynchronously because a failed or slow notification
 * must never roll back the comment or like that triggered it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Async
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String content,
                       String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        try {
            notificationMapper.insert(
                    Notification.of(userId, type, title, content, targetType, targetId));
        } catch (RuntimeException ex) {
            // Losing a notification is acceptable; losing the user's action is not.
            log.warn("failed to store {} notification for user {}: {}", type, userId, ex.getMessage());
        }
    }

    @Override
    public PageResult<NotificationVO> list(Long userId, Boolean unreadOnly, Long page, Long size) {
        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(Pagination.normalizePage(page), Pagination.normalizeSize(size)),
                new QueryWrapper<Notification>()
                        .eq("user_id", userId)
                        .eq(Boolean.TRUE.equals(unreadOnly), "is_read", 0)
                        .orderByDesc("create_time"));
        List<NotificationVO> records = result.getRecords().stream()
                .map(NotificationServiceImpl::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        // Scoped by user_id so one user cannot mark another's notification read.
        notificationMapper.markRead(userId, notificationId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId, LocalDateTime.now());
    }

    @Override
    public int unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    private static NotificationVO toVO(Notification notification) {
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .isRead(notification.getIsRead())
                .createTime(notification.getCreateTime())
                .build();
    }
}
