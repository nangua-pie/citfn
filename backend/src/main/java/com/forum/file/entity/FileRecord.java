package com.forum.file.entity;

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
 * Metadata for one uploaded object.
 *
 * <p>{@code storedName} is always a server generated random name; the original
 * file name is kept only for display and is never used as a path segment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file")
public class FileRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("original_name")
    private String originalName;

    @TableField("stored_name")
    private String storedName;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("object_key")
    private String objectKey;

    @TableField("url")
    private String url;

    @TableField("content_type")
    private String contentType;

    @TableField("size")
    private Long size;

    @TableField("uploader_id")
    private Long uploaderId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
