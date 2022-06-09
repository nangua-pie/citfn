package com.forum.comment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.forum.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * A comment or a reply.
 *
 * <p>{@code parentId} points at the comment being replied to (null for a root
 * comment) and {@code rootId} points at the thread root, which lets a whole
 * sub-thread load with one indexed query.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_comment")
public class Comment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("post_id")
    private Long postId;

    @TableField("user_id")
    private Long userId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("root_id")
    private Long rootId;

    /** Author of the comment this one replies to, for the "reply to @x" label. */
    @TableField("reply_user_id")
    private Long replyUserId;

    @TableField("content")
    private String content;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("reply_count")
    private Integer replyCount;

    /** @see CommentStatus */
    @TableField("status")
    private Integer status;

    @TableField("audit_time")
    private LocalDateTime auditTime;
}
