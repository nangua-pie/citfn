package com.forum.auth.service.impl;

import com.forum.auth.service.LoginAttemptService;
import com.forum.common.exception.BusinessException;
import com.forum.common.util.RedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Failed-login throttling backed by Redis.
 *
 * <p>The counter lives in Redis rather than memory so a restart does not hand
 * an attacker a fresh budget, and so it would still hold if the deployment
 * grew past one instance.
 */
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptServiceImpl(StringRedisTemplate redisTemplate,
                                   @Value("${forum.auth.login-max-attempts:5}") int maxAttempts,
                                   @Value("${forum.auth.login-lock-seconds:900}") long lockSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    @Override
    public void assertNotLocked(String identifier) {
        String key = RedisKeys.loginAttempt(identifier);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return;
        }
        int attempts;
        try {
            attempts = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            // A malformed counter is not worth failing the login over.
            redisTemplate.delete(key);
            return;
        }
        if (attempts < maxAttempts) {
            return;
        }
        long remaining = remainingLockSeconds(identifier);
        throw BusinessException.tooManyRequests(
                "too many failed login attempts, try again in " + remaining + " seconds");
    }

    @Override
    public void recordFailure(String identifier) {
        String key = RedisKeys.loginAttempt(identifier);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, lockDuration);
        }
    }

    @Override
    public void recordSuccess(String identifier) {
        redisTemplate.delete(RedisKeys.loginAttempt(identifier));
    }

    @Override
    public long remainingLockSeconds(String identifier) {
        String key = RedisKeys.loginAttempt(identifier);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        int attempts;
        try {
            attempts = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
        if (attempts < maxAttempts) {
            return 0L;
        }
        Long ttl = redisTemplate.getExpire(key);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }
}
