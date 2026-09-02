package com.sportiq.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Read-only accessor for the {@link SecurityUser} installed by {@link JwtAuthenticationFilter}. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityUser user) {
            return user.id();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}