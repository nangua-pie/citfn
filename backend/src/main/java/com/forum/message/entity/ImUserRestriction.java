package com.forum.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Administrator imposed private message ban.
 *
 * <p>A missing row means the user may send messages; {@code expiresAt} in the
 * past means the ban has lapsed.
 */
@Data
@TableName("im_user_restriction")
public class ImUserRestriction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("forbid_private_message")
    private Integer forbidPrivateMessage;

    @TableField("reason")
    private String reason;

    @TableField("operator_id")
    private Long operatorId;

    /** {@code null} means the ban never expires. */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** @return true when the ban is currently in force. */
    public boolean isActiveAt(LocalDateTime now) {
        if (forbidPrivateMessage == null || forbidPrivateMessage != 1) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
