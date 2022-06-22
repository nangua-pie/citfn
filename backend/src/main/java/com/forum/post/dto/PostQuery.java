package com.forum.post.dto;

import lombok.Data;

/**
 * Filter object for the post list endpoint.
 *
 * <p>Every field here is client supplied, so this type deliberately carries no
 * visibility flag: whether unpublished posts may be listed is decided by the
 * service from the caller's authorities and passed to the mapper separately.
 */
@Data
public class PostQuery {

    private Long categoryId;

    private Long userId;

    /** Matched against title and summary. */
    private String keyword;

    private String tagName;

    /** Restrict to posts an administrator marked as essential. */
    private Boolean onlyEssential;
}
