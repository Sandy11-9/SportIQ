package com.sportiq.backend.web.dto;

public record AuthResponse(String token, UserResponse user) {
}