package com.forum.common.security;

import com.forum.common.api.ResultCode;
import com.forum.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates the HS256 access and refresh tokens.
 *
 * <p>The signing secret is never hardcoded; it must be supplied through
 * configuration. A missing or short secret fails fast at startup.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** HS256 requires at least 256 bits of key material. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenProvider(@Value("${forum.jwt.secret}") String secret,
                            @Value("${forum.jwt.issuer}") String issuer,
                            @Value("${forum.jwt.access-token-ttl}") long accessTokenTtlSeconds,
                            @Value("${forum.jwt.refresh-token-ttl}") long refreshTokenTtlSeconds) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "forum.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes; set FORUM_JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String createAccessToken(Long userId, String username, List<String> authorities) {
        return build(userId, username, authorities, TYPE_ACCESS, accessTokenTtlSeconds);
    }

    public String createRefreshToken(Long userId, String username) {
        return build(userId, username, List.of(), TYPE_REFRESH, refreshTokenTtlSeconds);
    }

    private String build(Long userId, String username, List<String> authorities, String type, long ttlSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000L);
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_AUTHORITIES, authorities)
                .claim(CLAIM_TYPE, type)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses and verifies a token.
     *
     * @throws BusinessException with {@link ResultCode#UNAUTHORIZED} when the
     *         token is missing, malformed, expired or has the wrong type.
     */
    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw BusinessException.unauthorized("token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("rejected jwt: {}", ex.getMessage());
            throw BusinessException.unauthorized("invalid token");
        }
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw BusinessException.unauthorized("token type mismatch");
        }
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        List<String> authorities = claims.get(CLAIM_AUTHORITIES, List.class);
        return new AuthenticatedUser(userId, username, authorities == null ? List.of() : authorities);
    }

    /** Returns the subject of a refresh token, or throws when it is not one. */
    public Long parseRefreshTokenSubject(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw BusinessException.unauthorized("token type mismatch");
        }
        return Long.valueOf(claims.getSubject());
    }

    /** Stable digest used to store tokens so the raw value never hits the database. */
    public String fingerprint(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }
}
