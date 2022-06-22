package com.forum.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result of toggling a like or a bookmark.
 */
@Data
@AllArgsConstructor
public class LikeResult {

    /** True when the interaction is now active. */
    private Boolean active;

    /** Counter after the change. */
    private Integer count;
}
