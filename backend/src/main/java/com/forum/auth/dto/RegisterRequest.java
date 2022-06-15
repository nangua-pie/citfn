package com.forum.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Registration payload.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "is required")
    @Size(min = 3, max = 32, message = "must be 3-32 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$",
            message = "must start with a letter and contain only letters, digits or underscore")
    private String username;

    @NotBlank(message = "is required")
    @Size(min = 8, max = 64, message = "must be 8-64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
            message = "must contain at least one letter and one digit")
    private String password;

    @NotBlank(message = "is required")
    @Email(message = "must be a valid email address")
    @Size(max = 128, message = "must not exceed 128 characters")
    private String email;

    @NotBlank(message = "is required")
    @Pattern(regexp = "^\\d{6}$", message = "must be a 6 digit code")
    private String code;

    @Size(max = 64, message = "must not exceed 64 characters")
    private String nickname;
}
