package com.forum.message.websocket;

import com.forum.common.security.AuthenticatedUser;
import com.forum.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * Authenticates the STOMP handshake and stamps the principal onto every frame.
 *
 * <p>SockJS cannot set an {@code Authorization} header on the handshake, so the
 * token arrives as the {@code accessToken} query parameter, exactly as the REST
 * filter also accepts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImChannelInterceptor implements ChannelInterceptor {

    private static final String TOKEN_PARAM = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message, StompHeaderAccessor.SID, message.getHeaders());
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            String token = resolveToken(accessor);
            if (token == null) {
                throw new org.springframework.messaging.MessagingException("missing access token");
            }
            AuthenticatedUser user = tokenProvider.parseAccessToken(token);
            Principal principal = new StompPrincipal(user.getUserId(), user.getUsername());
            accessor.setUser(principal);
            return MessageBuilder.createMessage(payloadOf(message), accessor.getMessageHeaders());
        }

        // Every later frame must belong to an authenticated session.
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new org.springframework.messaging.MessagingException("session is not authenticated");
        }
        return message;
    }

    private Object payloadOf(Message<?> message) {
        return message.getPayload();
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> nativeHeaders = accessor.getNativeHeader("Authorization");
        if (nativeHeaders != null && !nativeHeaders.isEmpty()) {
            String value = nativeHeaders.get(0);
            if (value.startsWith(BEARER_PREFIX)) {
                return value.substring(BEARER_PREFIX.length()).trim();
            }
        }
        if (accessor.getSessionId() != null && accessor.getSessionAttributes() != null) {
            Object token = accessor.getSessionAttributes().get(TOKEN_PARAM);
            if (token instanceof String && !((String) token).isBlank()) {
                return ((String) token).trim();
            }
        }
        return null;
    }
}
