package com.sportiq.backend.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sportiq.media")
public record MediaProperties(
        String tempDir,
        long tempMaxAgeHours,
        double minDurationSeconds,
        double maxDurationSeconds,
        double framesPerSecond,
        long maxFileSizeBytes,
        List<String> allowedContentTypes) {

    public MediaProperties {
        if (tempDir == null || tempDir.isBlank()) {
            tempDir = "uploads";
        }
        if (tempMaxAgeHours <= 0) {
            tempMaxAgeHours = 1;
        }
        if (minDurationSeconds <= 0) {
            minDurationSeconds = 5;
        }
        if (maxDurationSeconds <= 0) {
            maxDurationSeconds = 20;
        }
        if (framesPerSecond <= 0) {
            framesPerSecond = 1.5;
        }
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes = 50L * 1024 * 1024;
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.of("video/mp4", "video/quicktime", "video/x-m4v",
                    "video/webm", "video/x-matroska");
        }
    }
}