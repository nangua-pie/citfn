package com.forum.auth.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Request to send a verification code to an email address.
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "is required")
    @Email(message = "must be a valid email address")
    private String email;

    @NotBlank(message = "is required")
    @Pattern(regexp = "REGISTER|LOGIN|RESET_PASSWORD", message = "must be REGISTER, LOGIN or RESET_PASSWORD")
    private String scene;
}
