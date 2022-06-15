package com.forum.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Username and password login payload.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "is required")
    @Size(min = 3, max = 64, message = "must be 3-64 characters")
    private String username;

    @NotBlank(message = "is required")
    @Size(min = 8, max = 64, message = "must be 8-64 characters")
    private String password;

    /** Optional second factor for the code based login scene. */
    @Size(max = 16, message = "must not exceed 16 characters")
    private String code;
}
