package com.forum.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A conversation as seen by one participant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private Long id;
    private Long userId;
    private Long targetUserId;

    /** Denormalised nickname of the peer, so the list needs one query, not two. */
    private String targetNickname;
    private String targetAvatarUrl;

    private Long lastMessageId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isPinned;

    /** Live presence of the peer, read from Redis. */
    private Boolean targetOnline;
}
