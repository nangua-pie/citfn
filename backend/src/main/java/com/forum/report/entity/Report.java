package com.forum.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A report raised against a post or a comment.
 *
 * <p>Unique on (target_type, target_id, reporter_id) so one user cannot flood
 * the moderation queue for the same item.
 */
@Data
@TableName("forum_report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** {@code POST} or {@code COMMENT}. */
    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reason_code")
    private String reasonCode;

    @TableField("reason_detail")
    private String reasonDetail;

    /** @see ReportStatus */
    @TableField("status")
    private Integer status;

    @TableField("handle_user_id")
    private Long handleUserId;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("handle_note")
    private String handleNote;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
