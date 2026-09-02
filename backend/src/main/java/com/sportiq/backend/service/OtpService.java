package com.sportiq.backend.service;

import com.sportiq.backend.config.OtpProperties;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Generates and hashes verification codes. The raw code is emailed to the
 * user; only its SHA-256 digest is persisted ({@code lld.md} §2.1), so a
 * leaked database cannot be replayed to forge verification.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpProperties properties;

    public OtpService(OtpProperties properties) {
        this.properties = properties;
    }

    public String generate() {
        int length = properties.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public String hash(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(otp.getBytes());
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public long ttlSeconds() {
        return properties.ttlSeconds();
    }

    public long resendCooldownSeconds() {
        return properties.resendCooldownSeconds();
    }

    public Instant expiresAt() {
        return Instant.now().plusSeconds(properties.ttlSeconds());
    }

    public Instant cooldownUntil() {
        return Instant.now().plusSeconds(properties.resendCooldownSeconds());
    }
}
