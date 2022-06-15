package com.forum.auth.service.impl;

import com.forum.auth.entity.VerifyCode;
import com.forum.auth.mapper.VerifyCodeMapper;
import com.forum.auth.service.VerifyCodeService;
import com.forum.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Verification codes backed by MariaDB.
 *
 * <p>Codes are stored in plaintext because they are single use, short lived and
 * already gated by a send cooldown; hashing would not improve the security of
 * the flow and would complicate lookup.
 */
@Slf4j
@Service
public class VerifyCodeServiceImpl implements VerifyCodeService {

    /** Minimum gap between two codes for the same target and scene. */
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);

    private final VerifyCodeMapper verifyCodeMapper;
    private final SecureRandom random = new SecureRandom();
    private final long registerTtlSeconds;
    private final long loginTtlSeconds;

    public VerifyCodeServiceImpl(VerifyCodeMapper verifyCodeMapper,
                                 @Value("${forum.auth.register-code-ttl:300}") long registerTtlSeconds,
                                 @Value("${forum.auth.login-code-ttl:300}") long loginTtlSeconds) {
        this.verifyCodeMapper = verifyCodeMapper;
        this.registerTtlSeconds = registerTtlSeconds;
        this.loginTtlSeconds = loginTtlSeconds;
    }

    @Override
    @Transactional
    public String issue(String target, String scene) {
        assertSendAllowed(target, scene);
        String code = String.format("%06d", random.nextInt(1_000_000));
        // Only one live code per target and scene.
        verifyCodeMapper.consumeAll(target, scene);
        VerifyCode record = new VerifyCode();
        record.setTarget(target);
        record.setScene(scene);
        record.setCode(code);
        record.setUsed(0);
        record.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds(scene)));
        record.setCreateTime(LocalDateTime.now());
        verifyCodeMapper.insert(record);
        log.info("verification code issued scene={} target={}", scene, maskTarget(target));
        return code;
    }

    @Override
    @Transactional
    public boolean consume(String target, String scene, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        VerifyCode active = verifyCodeMapper.selectActive(target, scene);
        if (active == null || !active.getCode().equals(code.trim())) {
            return false;
        }
        verifyCodeMapper.consumeAll(target, scene);
        return true;
    }

    @Override
    public void assertSendAllowed(String target, String scene) {
        int recent = verifyCodeMapper.countRecent(target, scene, LocalDateTime.now().minus(SEND_COOLDOWN));
        if (recent > 0) {
            throw BusinessException.tooManyRequests("a code was already sent recently, please wait");
        }
    }

    private long ttlSeconds(String scene) {
        return "LOGIN".equals(scene) ? loginTtlSeconds : registerTtlSeconds;
    }

    /** Keeps most of the address while leaving it non-actionable in logs. */
    private String maskTarget(String target) {
        if (target == null || target.length() <= 2) {
            return "***";
        }
        return target.charAt(0) + "***" + target.charAt(target.length() - 1);
    }
}
