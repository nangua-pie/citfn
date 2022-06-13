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
 * A file or image carried by an {@code IMAGE} or {@code FILE} message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("im_message_attachment")
public class ImMessageAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("message_id")
    private Long messageId;

    /** Storage key; the bucket name is in {@code sys_file}. */
    @TableField("object_key")
    private String objectKey;

    @TableField("url")
    private String url;

    @TableField("file_name")
    private String fileName;

    @TableField("content_type")
    private String contentType;

    @TableField("size")
    private Long size;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private LocalDateTime createTime;
}
