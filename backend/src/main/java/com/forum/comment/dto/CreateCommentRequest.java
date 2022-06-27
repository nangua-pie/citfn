package com.forum.comment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for posting a comment or a reply.
 */
@Data
public class CreateCommentRequest {

    @NotNull(message = "is required")
    private Long postId;

    /**
     * Comment being replied to; {@code null} creates a root comment.
     *
     * <p>The service resolves {@code rootId} and {@code replyUserId} from it, so
     * a client cannot attach a reply to the wrong thread.
     */
    private Long parentId;

    @NotBlank(message = "is required")
    @Size(max = 2000, message = "must not exceed 2000 characters")
    private String content;
}
