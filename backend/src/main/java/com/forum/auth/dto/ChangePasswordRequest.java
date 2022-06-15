package com.forum.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Password change for a signed in user.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "is required")
    private String currentPassword;

    @NotBlank(message = "is required")
    @Size(min = 8, max = 64, message = "must be 8-64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
            message = "must contain at least one letter and one digit")
    private String newPassword;
}
