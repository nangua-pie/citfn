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
 * One time verification code for registration, login and password reset.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_verify_code")
public class VerifyCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("target")
    private String target;

    @TableField("code")
    private String code;

    @TableField("scene")
    private String scene;

    @TableField("used")
    private Integer used;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("create_time")
    private LocalDateTime createTime;
}
