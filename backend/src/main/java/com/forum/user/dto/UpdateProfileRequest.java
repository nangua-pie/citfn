package com.forum.user.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Profile fields the signed in user may change.
 *
 * <p>Every field is optional so the endpoint behaves as a PATCH; omitted
 * fields keep their stored value.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 64, message = "must be 1-64 characters")
    private String nickname;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String avatarUrl;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String bio;
}
