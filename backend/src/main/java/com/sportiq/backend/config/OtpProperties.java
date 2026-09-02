package com.sportiq.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sportiq.otp")
public record OtpProperties(int length, long ttlSeconds, int resendCooldownSeconds) {

    public OtpProperties {
        if (length <= 0) {
            length = 6;
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = 600;
        }
        if (resendCooldownSeconds <= 0) {
            resendCooldownSeconds = 30;
        }
    }
}
