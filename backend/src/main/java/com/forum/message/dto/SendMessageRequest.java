package com.forum.message.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Private message payload, matching the shape documented in AGENTS.md 6.4.
 */
@Data
public class SendMessageRequest {

    /**
     * Optional hint. The server resolves the conversation from the sender and
     * receiver pair, so a client cannot address a conversation it does not own.
     */
    private Long conversationId;

    @NotNull(message = "is required")
    private Long receiverId;

    @NotBlank(message = "is required")
    @Pattern(regexp = "TEXT|IMAGE|FILE|SYSTEM", message = "must be TEXT, IMAGE, FILE or SYSTEM")
    private String messageType;

    /**
     * Text body, or a short description for file messages. Length is enforced
     * again in the service against {@code forum.message.max-text-length}.
     */
    @NotBlank(message = "is required")
    @Size(max = 2000, message = "must not exceed 2000 characters")
    private String content;

    /** JSON blob with type specific extras, for example image metadata. */
    @Size(max = 1000, message = "must not exceed 1000 characters")
    private String extra;
}
