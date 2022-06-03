package com.forum.common.util;

/**
 * Central registry of Redis key patterns.
 *
 * <p>AGENTS.md section 6.5 pins the private message keys; everything else
 * follows the same {@code domain:sub:id} convention so key names are
 * greppable and never assembled ad hoc.
 */
public final class RedisKeys {

    // --- Private messaging (fixed by AGENTS.md 6.5) ---
    /** Presence marker for an online user. */
    public static final String IM_ONLINE = "im:online:";
    /** Cached unread counter. */
    public static final String IM_UNREAD = "im:unread:";
    /** Cached recent conversation list. */
    public static final String IM_CONVERSATION = "im:conversation:";
    /** Sliding window counter for send rate limiting. */
    public static final String IM_SEND_LIMIT = "im:send:limit:";
    /** WebSocket session tracking for a user. */
    public static final String IM_SOCKET = "im:socket:";

    // --- Authentication ---
    /** Revoked access tokens, keyed by token fingerprint. */
    public static final String TOKEN_BLACKLIST = "auth:token:blacklist:";
    /** Failed login counter for rate limiting and lockout. */
    public static final String LOGIN_ATTEMPT = "auth:login:attempt:";
    /** Registration / login verification code. */
    public static final String VERIFY_CODE = "auth:verify:";

    private RedisKeys() {
    }

    public static String imOnline(Long userId) {
        return IM_ONLINE + userId;
    }

    public static String imUnread(Long userId) {
        return IM_UNREAD + userId;
    }

    public static String imConversation(Long userId) {
        return IM_CONVERSATION + userId;
    }

    public static String imSendLimit(Long userId) {
        return IM_SEND_LIMIT + userId;
    }

    public static String imSocket(Long userId) {
        return IM_SOCKET + userId;
    }

    public static String tokenBlacklist(String fingerprint) {
        return TOKEN_BLACKLIST + fingerprint;
    }

    public static String loginAttempt(String identifier) {
        return LOGIN_ATTEMPT + identifier;
    }

    public static String verifyCode(String scene, String target) {
        return VERIFY_CODE + scene + ":" + target;
    }
}
