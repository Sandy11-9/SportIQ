package com.sportiq.backend.media;

import com.sportiq.backend.exception.FrameExtractionException;
import com.sportiq.backend.support.TestMediaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameExtractionServiceTest {

    private Path root;
    private FrameExtractionService service;

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createTempDirectory("sportiq-extraction-test");
        MediaProperties properties = new MediaProperties(root.toString(), 1, 5, 20, 1.5,
                50L * 1024 * 1024, null);
        service = new FrameExtractionService(properties, new FfmpegProperties("ffmpeg", "ffprobe"),
                new ProcessRunner());
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void extractsFramesProportionalToDuration() throws IOException {
        Path video = root.resolve(UUID.randomUUID() + ".mp4");
        Files.write(video, TestMediaUtil.videoBytes(6));

        try {
            List<Path> frames = service.extractFrames(video, 6.0);

            assertEquals(9, frames.size());
            for (Path frame : frames) {
                assertTrue(Files.isRegularFile(frame));
                assertTrue(frame.toString().endsWith(".jpg"));
            }
        } finally {
            Files.deleteIfExists(video);
        }
    }

    @Test
    void missingSourceThrowsFrameExtractionException() {
        Path missing = root.resolve("does-not-exist.mp4");

        assertThrows(FrameExtractionException.class, () -> service.extractFrames(missing, 6.0));
    }
}