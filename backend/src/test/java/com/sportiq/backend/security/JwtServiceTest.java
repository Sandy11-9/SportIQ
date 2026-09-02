package com.sportiq.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "sportiq-test-secret-key-0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);

    @Test
    void generatedTokenRoundTripsClaims() {
        String token = jwtService.generateToken(42L, "coach@sportiq.app");

        Claims claims = jwtService.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals("coach@sportiq.app", claims.get("email", String.class));
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(key)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.parse(expired));
    }

    @Test
    void forgedTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor("another-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.parse(forged));
    }
}