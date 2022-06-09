package com.forum.comment.entity;

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
 * A like on a comment, unique per (comment, user).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_comment_like")
public class CommentLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("comment_id")
    private Long commentId;

    @TableField("user_id")
    private Long userId;

    @TableField("create_time")
    private LocalDateTime createTime;

    public static CommentLike of(Long commentId, Long userId) {
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        return like;
    }
}
