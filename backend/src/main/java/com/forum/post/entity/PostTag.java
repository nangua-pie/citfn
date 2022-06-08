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
 * A tag attached to a post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_post_tag")
public class PostTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("tag_name")
    private String tagName;

    @TableField("create_time")
    private LocalDateTime createTime;

    public static PostTag of(Long postId, String tagName) {
        PostTag tag = new PostTag();
        tag.setPostId(postId);
        tag.setTagName(tagName);
        tag.setCreateTime(LocalDateTime.now());
        return tag;
    }
}
