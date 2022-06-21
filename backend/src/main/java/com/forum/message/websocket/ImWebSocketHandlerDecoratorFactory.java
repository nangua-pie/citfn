package com.forum.message.websocket;

import com.forum.common.util.RedisKeys;
import com.forum.message.service.MessagePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is connected and mirrors that state into Redis.
 *
 * <p>Keys match AGENTS.md section 6.5: {@code im:online:{userId}} is the
 * presence marker and {@code im:socket:{userId}} records the live session ids.
 * A user is online while at least one of their sockets is open, so closing one
 * browser tab does not report the user as gone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImWebSocketHandlerDecoratorFactory {

    /** Presence entries expire on their own so a crashed node self-heals. */
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(45);

    private final StringRedisTemplate redisTemplate;
    private final MessagePushService messagePushService;

    private final Set<String> sessionIndex = ConcurrentHashMap.newKeySet();

    public ChannelInterceptor create() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                if (command == null) {
                    return message;
                }
                if (!(accessor.getUser() instanceof StompPrincipal) || accessor.getSessionId() == null) {
                    return message;
                }
                StompPrincipal principal = (StompPrincipal) accessor.getUser();
                if (StompCommand.CONNECT.equals(command)) {
                    markOnline(principal.getUserId(), accessor.getSessionId());
                } else if (StompCommand.DISCONNECT.equals(command)) {
                    markOffline(principal.getUserId(), accessor.getSessionId());
                }
                return message;
            }
        };
    }

    private void markOnline(Long userId, String sessionId) {
        sessionIndex.add(key(userId, sessionId));
        redisTemplate.opsForSet().add(RedisKeys.imSocket(userId), sessionId);
        redisTemplate.opsForValue().set(RedisKeys.imOnline(userId), "1", PRESENCE_TTL);
        messagePushService.pushOnlineState(userId, true);
        log.info("user {} connected to private messaging", userId);
    }

    private void markOffline(Long userId, String sessionId) {
        sessionIndex.remove(key(userId, sessionId));
        redisTemplate.opsForSet().remove(RedisKeys.imSocket(userId), sessionId);
        // Another tab may still be open, in which case the user stays online.
        Long remaining = redisTemplate.opsForSet().size(RedisKeys.imSocket(userId));
        if (remaining == null || remaining == 0L) {
            redisTemplate.delete(RedisKeys.imOnline(userId));
            redisTemplate.delete(RedisKeys.imSocket(userId));
            messagePushService.pushOnlineState(userId, false);
            log.info("user {} disconnected from private messaging", userId);
        }
    }

    /** @return true when the user currently has at least one open socket. */
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.imOnline(userId)));
    }

    private String key(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }
}
