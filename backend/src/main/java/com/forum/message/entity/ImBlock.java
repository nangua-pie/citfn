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
 * A one-directional block: {@code userId} refuses private messages from
 * {@code blockedUserId}. Sending is checked in both directions, so the service
 * can treat a block as "no messages either way".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("im_block")
public class ImBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("blocked_user_id")
    private Long blockedUserId;

    @TableField("reason")
    private String reason;

    @TableField("create_time")
    private LocalDateTime createTime;

    public static ImBlock of(Long userId, Long blockedUserId, String reason) {
        ImBlock block = new ImBlock();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setReason(reason);
        block.setCreateTime(LocalDateTime.now());
        return block;
    }
}
