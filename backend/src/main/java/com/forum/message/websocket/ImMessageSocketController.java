package com.forum.message.websocket;

import com.forum.common.exception.BusinessException;
import com.forum.message.dto.ImMessageVO;
import com.forum.message.dto.SendMessageRequest;
import com.forum.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import javax.validation.Valid;

/**
 * STOMP entry point for clients that prefer sending over the socket.
 *
 * <p>The destination is {@code /app/im.send} as fixed by AGENTS.md 6.4. The
 * sender id is taken from the authenticated session rather than the payload,
 * so a client cannot send as somebody else.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ImMessageSocketController {

    private final MessageService messageService;

    @MessageMapping("/im.send")
    public ImMessageVO send(@Payload @Valid SendMessageRequest request,
                            java.security.Principal principal) {
        return messageService.sendMessage(requireUserId(principal), request);
    }

    /** Convenience endpoint used by the client to acknowledge a receipt. */
    @MessageMapping("/im.read")
    public void read(@Payload ReadPayload payload, java.security.Principal principal) {
        if (payload == null || payload.getMessageId() == null) {
            throw BusinessException.badRequest("messageId is required");
        }
        messageService.markMessageRead(requireUserId(principal), payload.getMessageId());
    }

    /** The handshake interceptor guarantees the principal is a {@link StompPrincipal}. */
    private Long requireUserId(java.security.Principal principal) {
        if (!(principal instanceof StompPrincipal)) {
            throw new org.springframework.messaging.MessagingException("session is not authenticated");
        }
        return ((StompPrincipal) principal).getUserId();
    }

    /** Payload of the {@code /app/im.read} destination. */
    public static class ReadPayload {
        private Long messageId;

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }
    }
}
