package com.sportiq.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "otp is required")
        @Pattern(regexp = "^\\d{6}$", message = "otp must be a 6-digit code")
        String otp) {
}
