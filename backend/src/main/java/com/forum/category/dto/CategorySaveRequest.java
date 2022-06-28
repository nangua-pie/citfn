package com.forum.category.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Board create and update payload.
 *
 * <p>Kept separate from {@link CategoryVO} so the service never depends on a
 * response shape, and so validation constraints live with the input.
 */
@Data
public class CategorySaveRequest {

    @NotBlank(message = "is required")
    @Size(max = 64, message = "must not exceed 64 characters")
    private String name;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String description;

    @Size(max = 64, message = "must not exceed 64 characters")
    private String icon;

    private Integer sortOrder;

    private Integer status;
}
