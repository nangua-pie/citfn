package com.forum.common.security;

import com.forum.common.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Logout / revocation support.
 *
 * <p>A revoked access token is remembered until the moment it would have
 * expired anyway, so the blacklist can never grow without bound.
 */
@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider tokenProvider;

    public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtTokenProvider tokenProvider) {
        this.redisTemplate = redisTemplate;
        this.tokenProvider = tokenProvider;
    }

    /** Revokes an access token until its natural expiry. */
    public void revoke(String accessToken) {
        try {
            long remaining = tokenProvider.parse(accessToken).getExpiration().getTime() - System.currentTimeMillis();
            if (remaining <= 0) {
                return;
            }
            redisTemplate.opsForValue().set(
                    RedisKeys.tokenBlacklist(tokenProvider.fingerprint(accessToken)),
                    "1",
                    Duration.ofMillis(remaining));
        } catch (RuntimeException ex) {
            // An unparseable token is already useless; nothing to revoke.
        }
    }

    public boolean isRevoked(String accessToken) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKeys.tokenBlacklist(tokenProvider.fingerprint(accessToken))));
    }
}
