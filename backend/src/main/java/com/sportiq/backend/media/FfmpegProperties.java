package com.sportiq.backend.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sportiq.ffmpeg")
public record FfmpegProperties(String binary, String ffprobeBinary) {
}