package com.forum.post.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Partial update of a post. Omitted fields keep their current value.
 */
@Data
public class UpdatePostRequest {

    @Size(min = 4, max = 128, message = "must be 4-128 characters")
    private String title;

    @NotBlank(message = "is required")
    @Size(max = 20000, message = "must not exceed 20000 characters")
    private String content;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String coverUrl;

    @Size(max = 5, message = "must not contain more than 5 tags")
    private List<@Size(max = 48, message = "must not exceed 48 characters") String> tags;
}
