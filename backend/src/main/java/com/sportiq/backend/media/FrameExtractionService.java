package com.sportiq.backend.media;

import com.sportiq.backend.exception.FrameExtractionException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Extracts evenly spaced JPEG frames from a validated video at a constant
 * sampling rate ({@code fps}), producing a dynamic frame count that grows
 * with clip duration ({@code hld.md} §3.1). The caller is responsible for
 * deleting the frame dir.
 */
@Service
public class FrameExtractionService {

    private final MediaProperties properties;
    private final FfmpegProperties ffmpeg;
    private final ProcessRunner processRunner;

    public FrameExtractionService(MediaProperties properties, FfmpegProperties ffmpeg, ProcessRunner processRunner) {
        this.properties = properties;
        this.ffmpeg = ffmpeg;
        this.processRunner = processRunner;
    }

    public List<Path> extractFrames(Path videoPath, double durationSeconds) {
        Path frameDir;
        try {
            frameDir = Files.createDirectories(Path.of(properties.tempDir())
                    .resolve(videoPath.getFileName().toString().replace(".mp4", "_frames")));
        } catch (IOException ex) {
            throw new FrameExtractionException("Could not create frame directory: " + ex.getMessage(), ex);
        }

        int target = Math.max(1, (int) Math.round(properties.framesPerSecond() * durationSeconds));
        String filter = String.format(Locale.ROOT, "fps=%.3f", properties.framesPerSecond());
        String outputPattern = frameDir.resolve("frame_%03d.jpg").toString();
        CommandResult result = processRunner.run(List.of(
                ffmpeg.binary(),
                "-y",
                "-v", "error",
                "-i", videoPath.toString(),
                "-vf", filter,
                "-frames:v", String.valueOf(target),
                outputPattern));

        if (result.exitCode() != 0) {
            MediaFileUtils.deleteTree(frameDir);
            throw new FrameExtractionException("Frame extraction failed: " + result.output().trim());
        }

        List<Path> frames;
        try (Stream<Path> stream = Files.list(frameDir)) {
            frames = stream.sorted(Comparator.comparing(Path::getFileName)).toList();
        } catch (IOException ex) {
            throw new FrameExtractionException("Could not list extracted frames: " + ex.getMessage(), ex);
        }

        if (frames.isEmpty()) {
            MediaFileUtils.deleteTree(frameDir);
            throw new FrameExtractionException("No frames were extracted from the video");
        }
        return frames;
    }
}