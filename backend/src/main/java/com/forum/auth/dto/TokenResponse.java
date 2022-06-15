package com.forum.auth.dto;

import com.forum.user.dto.UserProfileVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Successful authentication result.
 */
@Data
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;

    private String refreshToken;

    /** Access token lifetime in seconds, so the client can schedule a refresh. */
    private long expiresIn;

    private UserProfileVO user;
}
