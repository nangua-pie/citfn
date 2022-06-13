package com.forum.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A durable private message row.
 *
 * <p>Ids increase monotonically, which is what allows cursor pagination
 * ({@code beforeId}) instead of deep offset paging. Redis is only a cache; this
 * table is the source of truth.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("im_message")
public class ImMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Conversation row of the receiver, used for that side's unread counter. */
    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    /** @see MessageType */
    @TableField("message_type")
    private String messageType;

    @TableField("content")
    private String content;

    /** JSON blob with type specific extras; never logged. */
    @TableField("extra")
    private String extra;

    /** @see MessageStatus */
    @TableField("status")
    private String status;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("recall_time")
    private LocalDateTime recallTime;

    @TableField("is_deleted")
    private Integer isDeleted;
}
