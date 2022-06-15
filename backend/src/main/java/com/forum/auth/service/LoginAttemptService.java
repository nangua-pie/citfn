package com.forum.auth.service;

/**
 * Failed-login throttling.
 */
public interface LoginAttemptService {

    /**
     * Blocks the attempt when the identifier has failed too often.
     *
     * @throws com.forum.common.exception.BusinessException with {@code 42900} when locked out
     */
    void assertNotLocked(String identifier);

    /** Records a failure; locks the identifier once the threshold is reached. */
    void recordFailure(String identifier);

    /** Clears the counter after a successful login. */
    void recordSuccess(String identifier);

    /** Remaining lock time in seconds, or {@code 0} when not locked. */
    long remainingLockSeconds(String identifier);
}
