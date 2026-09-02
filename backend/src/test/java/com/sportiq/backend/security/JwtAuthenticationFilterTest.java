package com.sportiq.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "sportiq-test-secret-key-0123456789abcdef";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenPopulatesSecurityContext() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(7L, "player@sportiq.app"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Authentication> captured = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) ->
                captured.set(SecurityContextHolder.getContext().getAuthentication()));

        Authentication authentication = captured.get();
        assertTrue(authentication != null && authentication.isAuthenticated());
        assertInstanceOf(SecurityUser.class, authentication.getPrincipal());
        assertEquals(7L, ((SecurityUser) authentication.getPrincipal()).id());
    }

    @Test
    void missingHeaderLeavesContextEmpty() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET, 3_600_000));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenIsIgnored() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET, 3_600_000));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer lets-fake-it");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}