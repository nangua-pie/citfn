package com.forum.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Exchange a refresh token for a new access token.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "is required")
    private String refreshToken;
}
