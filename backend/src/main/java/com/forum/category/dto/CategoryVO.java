package com.forum.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Category projection for the API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer postCount;
    private Integer status;
    private LocalDateTime createTime;
}
