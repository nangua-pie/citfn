package com.forum.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification projection returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private Integer isRead;
    private LocalDateTime createTime;
}
