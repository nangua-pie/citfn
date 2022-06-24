package com.forum.message.websocket;

import com.forum.common.util.RedisKeys;
import com.forum.message.service.MessagePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Tracks who is connected and mirrors that state into Redis.
 *
 * <p>Keys match AGENTS.md section 6.5: {@code im:online:{userId}} is the
 * presence marker and {@code im:socket:{userId}} holds the live session ids.
 * A user stays online while at least one socket is open, so closing one browser
 * tab does not report the user as gone.
 *
 * <p>Driven by {@link ImChannelInterceptor} when the STOMP CONNECT and
 * DISCONNECT commands pass through.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImPresenceService {

    /** Presence expires on its own, so a crashed node self-heals. */
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(45);

    private final StringRedisTemplate redisTemplate;
    private final MessagePushService messagePushService;

    public void markOnline(Long userId, String sessionId) {
        redisTemplate.opsForSet().add(RedisKeys.imSocket(userId), sessionId);
        redisTemplate.opsForValue().set(RedisKeys.imOnline(userId), "1", PRESENCE_TTL);
        messagePushService.pushOnlineState(userId, true);
        log.info("user {} connected to private messaging", userId);
    }

    public void markOffline(Long userId, String sessionId) {
        redisTemplate.opsForSet().remove(RedisKeys.imSocket(userId), sessionId);
        // Another tab may still be open, so the user stays online in that case.
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
}
