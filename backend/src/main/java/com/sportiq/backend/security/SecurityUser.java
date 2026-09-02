package com.sportiq.backend.security;

/** Authenticated principal installed in the SecurityContext by {@link JwtAuthenticationFilter}. */
public record SecurityUser(Long id, String email) {
}