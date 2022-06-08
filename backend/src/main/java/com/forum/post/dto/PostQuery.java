package com.forum.post.dto;

import lombok.Data;

/**
 * Filter object for the post list endpoint.
 *
 * <p>{@code includeAllStatuses} is set by the service, never by the client:
 * regular listing may only ever see published posts.
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

    /** Service managed flag; when false the status filter is applied. */
    private boolean includeAllStatuses;

    /** Status considered publicly visible; normally {@code PUBLISHED}. */
    private Integer visibleStatus;
}
