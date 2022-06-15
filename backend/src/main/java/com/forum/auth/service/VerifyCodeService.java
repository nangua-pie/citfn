package com.forum.auth.service;

/**
 * Email verification codes for registration, login and password reset.
 */
public interface VerifyCodeService {

    /** Generates, stores and returns a new code, replacing any earlier one. */
    String issue(String target, String scene);

    /**
     * Consumes a code.
     *
     * @return true when it matched and had not expired
     */
    boolean consume(String target, String scene, String code);

    /** Enforces the per-target send cooldown to stop code spamming. */
    void assertSendAllowed(String target, String scene);
}
