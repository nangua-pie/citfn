package com.forum.post.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The markdown body of a post, keyed by {@code post_id}.
 *
 * <p>Raw markdown is stored; the frontend renders it and sanitises the HTML
 * with DOMPurify. The backend never sends unsanitised HTML.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("forum_post_content")
public class PostContent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long postId;

    @TableField("content_md")
    private String contentMd;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
