package com.forum.post.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Payload for publishing a post.
 */
@Data
public class CreatePostRequest {

    @NotNull(message = "is required")
    private Long categoryId;

    @NotBlank(message = "is required")
    @Size(min = 4, max = 128, message = "must be 4-128 characters")
    private String title;

    @NotBlank(message = "is required")
    @Size(max = 20000, message = "must not exceed 20000 characters")
    private String content;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String coverUrl;

    /** At most five tags; trimmed, de-duplicated and lower-cased on write. */
    @Size(max = 5, message = "must not contain more than 5 tags")
    private List<@Size(max = 48, message = "must not exceed 48 characters") String> tags;
}
