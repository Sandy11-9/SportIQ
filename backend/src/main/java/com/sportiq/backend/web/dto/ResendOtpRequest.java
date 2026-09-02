package com.sportiq.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email) {
}
