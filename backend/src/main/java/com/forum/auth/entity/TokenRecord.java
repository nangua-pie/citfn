package com.forum.auth.entity;

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
 * Issued token metadata. Only the SHA-256 fingerprint is stored so a database
 * leak cannot be replayed against the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_token")
public class TokenRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("token_type")
    private String tokenType;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("revoked")
    private Integer revoked;

    @TableField("revoke_time")
    private LocalDateTime revokeTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
