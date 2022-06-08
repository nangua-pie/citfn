package com.forum.post.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.forum.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Post metadata used by list and search queries.
 *
 * <p>The markdown body lives in {@link PostContent} so listing never pulls a
 * large MEDIUMTEXT column.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post")
public class Post extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("category_id")
    private Long categoryId;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    /** Plain text excerpt derived from the markdown body. */
    @TableField("summary")
    private String summary;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("collect_count")
    private Integer collectCount;

    @TableField("report_count")
    private Integer reportCount;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_essential")
    private Integer isEssential;

    /** @see PostStatus */
    @TableField("status")
    private Integer status;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_reason")
    private String auditReason;
}
