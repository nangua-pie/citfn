package com.forum.message.websocket;

import com.forum.common.security.AuthenticatedUser;
import com.forum.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
 * Authenticates the STOMP handshake, stamps the principal onto every frame and
 * keeps the online markers in Redis in step with the session lifecycle.
 *
 * <p>SockJS cannot set an {@code Authorization} header on the handshake, so the
 * token arrives as the {@code accessToken} query parameter, exactly as the REST
 * filter also accepts.
 */
@Slf4j
@Component
public class ImChannelInterceptor implements ChannelInterceptor {

    private static final String TOKEN_PARAM = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    /**
     * Resolved lazily on purpose.
     *
     * <p>ImPresenceService needs MessagePushService, which needs
     * SimpMessagingTemplate, which is created by the message broker
     * configuration that collects this very interceptor. Constructor injecting
     * the presence service would close that loop and the context would fail to
     * start with a circular reference.
     */
    private final ObjectProvider<ImPresenceService> presenceService;

    public ImChannelInterceptor(JwtTokenProvider tokenProvider,
                                ObjectProvider<ImPresenceService> presenceService) {
        this.tokenProvider = tokenProvider;
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
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
            Message<?> authenticated =
                    MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            presenceService.getObject().markOnline(user.getUserId(), accessor.getSessionId());
            return authenticated;
        }

        // Every later frame must belong to an authenticated session.
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new org.springframework.messaging.MessagingException("session is not authenticated");
        }
        if (StompCommand.DISCONNECT.equals(command) && principal instanceof StompPrincipal) {
            presenceService.getObject()
                    .markOffline(((StompPrincipal) principal).getUserId(), accessor.getSessionId());
        }
        return message;
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
