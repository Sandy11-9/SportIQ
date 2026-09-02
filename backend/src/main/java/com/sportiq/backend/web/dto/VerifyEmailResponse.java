package com.sportiq.backend.web.dto;

public record VerifyEmailResponse(Long id, String email, boolean verified) {
}
