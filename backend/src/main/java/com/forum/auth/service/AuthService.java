package com.forum.auth.service;

import com.forum.auth.dto.ChangePasswordRequest;
import com.forum.auth.dto.LoginRequest;
import com.forum.auth.dto.RefreshTokenRequest;
import com.forum.auth.dto.RegisterRequest;
import com.forum.auth.dto.SendCodeRequest;
import com.forum.auth.dto.TokenResponse;

/**
 * Registration, login, token refresh and logout.
 */
public interface AuthService {

    /** Creates an account after verifying the emailed code and uniqueness. */
    TokenResponse register(RegisterRequest request);

    /**
     * Verifies credentials and issues a token pair.
     *
     * <p>Rate limited per identifier; repeated failures lock the account
     * temporarily so the endpoint cannot be used to brute force passwords.
     */
    TokenResponse login(LoginRequest request, String clientIp);

    /** Issues a new access token from a valid, unrevoked refresh token. */
    TokenResponse refresh(RefreshTokenRequest request);

    /** Revokes the presented access token until its natural expiry. */
    void logout(String accessToken);

    /** Revokes every live token of the user. */
    void logoutAll(Long userId);

    /** Changes the password of the signed in user and revokes existing tokens. */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * Issues a verification code for the given scene.
     *
     * <p>The generated code is logged rather than emailed in this build; the
     * sending channel is intentionally left to deployment configuration.
     */
    void sendCode(SendCodeRequest request);
}
