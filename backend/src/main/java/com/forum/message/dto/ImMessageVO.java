package com.forum.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message projection sent to clients and pushed over STOMP.
 *
 * <p>Field names match the payload documented in AGENTS.md section 6.4 so the
 * client can bind the pushed JSON directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImMessageVO {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String extra;
    private String status;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
    private LocalDateTime recallTime;

    /** Present for {@code IMAGE} and {@code FILE} messages. */
    private List<ImMessageAttachmentVO> attachments;
}
