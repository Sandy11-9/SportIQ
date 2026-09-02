package com.sportiq.backend.media;

import com.sportiq.backend.exception.VideoValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Enforces the file contract ({@code hld.md} §3): type, size and a 5-8s
 * duration probed via ffprobe. Writes the upload into the temp dir; the
 * caller always deletes the {@link ValidatedVideo} path afterwards.
 */
@Service
public class VideoValidationService {

    private static final Logger log = LoggerFactory.getLogger(VideoValidationService.class);

    private final MediaProperties properties;
    private final FfmpegProperties ffmpeg;
    private final ProcessRunner processRunner;

    public VideoValidationService(MediaProperties properties, FfmpegProperties ffmpeg, ProcessRunner processRunner) {
        this.properties = properties;
        this.ffmpeg = ffmpeg;
        this.processRunner = processRunner;
    }

    public ValidatedVideo validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VideoValidationException("Video file is empty");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new VideoValidationException("Video exceeds the maximum allowed size");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !properties.allowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new VideoValidationException("Unsupported video format: " + contentType);
        }

        Path tempFile = null;
        try {
            Path tempDir = Files.createDirectories(Path.of(properties.tempDir()));
            tempFile = tempDir.resolve(UUID.randomUUID() + ".mp4");
            file.transferTo(tempFile);

            double duration = probeDuration(tempFile);
            if (duration < properties.minDurationSeconds() || duration > properties.maxDurationSeconds()) {
                throw new VideoValidationException(
                        String.format(Locale.ROOT, "Video duration must be between %.0f and %.0f seconds, got %.2fs",
                                properties.minDurationSeconds(), properties.maxDurationSeconds(), duration));
            }
            log.info("Validated upload {} ({}s)", tempFile, String.format(Locale.ROOT, "%.2f", duration));
            return new ValidatedVideo(tempFile, duration);
        } catch (VideoValidationException ex) {
            deleteQuietly(tempFile);
            throw ex;
        } catch (IOException ex) {
            deleteQuietly(tempFile);
            throw new VideoValidationException("Could not store uploaded video: " + ex.getMessage(), ex);
        }
    }

    private double probeDuration(Path file) {
        CommandResult result = processRunner.run(List.of(
                ffmpeg.ffprobeBinary(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString()));
        if (result.exitCode() != 0) {
            throw new VideoValidationException("Could not probe video: " + tail(result.output()));
        }
        try {
            return Double.parseDouble(result.output().trim());
        } catch (NumberFormatException ex) {
            throw new VideoValidationException("Could not read video duration: " + tail(result.output()));
        }
    }

    private static String tail(String output) {
        String trimmed = output.replace('\n', ' ').trim();
        return trimmed.length() > 200 ? trimmed.substring(trimmed.length() - 200) : trimmed;
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                log.warn("Could not clean temp upload {}", path);
            }
        }
    }
}