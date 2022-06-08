package com.forum.post.entity;

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
 * A bookmark on a post, unique per (post, user).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_post_collect")
public class PostCollect implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("user_id")
    private Long userId;

    @TableField("create_time")
    private LocalDateTime createTime;

    public static PostCollect of(Long postId, Long userId) {
        PostCollect collect = new PostCollect();
        collect.setPostId(postId);
        collect.setUserId(userId);
        collect.setCreateTime(LocalDateTime.now());
        return collect;
    }
}
