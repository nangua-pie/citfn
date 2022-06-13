package com.forum.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.forum.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * One participant's view of a private conversation.
 *
 * <p>A chat between two users therefore owns two rows, one per side, which is
 * what makes {@code (user_id, target_user_id)} unique and lets each side have
 * its own unread counter and pinned flag.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_conversation")
public class ImConversation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Owner of this row, i.e. the user looking at the conversation. */
    @TableField("user_id")
    private Long userId;

    /** The other participant. */
    @TableField("target_user_id")
    private Long targetUserId;

    /** Cursor anchor: the newest message id seen by the owner. */
    @TableField("last_message_id")
    private Long lastMessageId;

    /** Truncated, plaintext preview for the conversation list. */
    @TableField("last_message_preview")
    private String lastMessagePreview;

    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    @TableField("unread_count")
    private Integer unreadCount;

    @TableField("is_pinned")
    private Integer isPinned;
}
