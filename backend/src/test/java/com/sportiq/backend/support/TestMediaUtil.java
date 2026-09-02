package com.sportiq.backend.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates real MP4 test videos with ffmpeg (a scrolling color pattern of a
 * given duration) so video validation and frame extraction can be exercised
 * against real media without shipping fixture binaries.
 */
public final class TestMediaUtil {

    private TestMediaUtil() {
    }

    /**
     * Creates a playable MP4 of the requested duration (seconds, >= 0.1).
     *
     * @return path to the generated file in the JVM tmp dir
     */
    public static Path createTestVideo(double seconds) throws IOException, InterruptedException {
        Path file = Files.createTempFile("sportiq-test-", ".mp4");
        Process p = new ProcessBuilder(
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "lavfi",
                "-i", String.format("testsrc=duration=%.1f:size=320x240:rate=30", seconds),
                "-pix_fmt", "yuv420p",
                file.toString())
                .redirectErrorStream(true)
                .start();
        int exit = p.waitFor();
        if (exit != 0) {
            Files.deleteIfExists(file);
            throw new IllegalStateException("ffmpeg failed to generate test video with exit code " + exit);
        }
        return file;
    }

    /**
     * @return the bytes of a freshly generated test video of the given duration.
     */
    public static byte[] videoBytes(double seconds) {
        try {
            Path file = createTestVideo(seconds);
            byte[] bytes = Files.readAllBytes(file);
            Files.deleteIfExists(file);
            return bytes;
        } catch (IOException | InterruptedException ex) {
            throw new IllegalStateException("ffmpeg test video generation failed", ex);
        }
    }
}