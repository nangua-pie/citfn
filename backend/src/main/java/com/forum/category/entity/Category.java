package com.forum.category.entity;

import com.forum.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A forum board, for example "General" or "Development".
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_category")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    /** Material Design icon name, rendered by the frontend. */
    @TableField("icon")
    private String icon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("post_count")
    private Integer postCount;

    @TableField("status")
    private Integer status;
}
