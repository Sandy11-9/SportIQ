package com.sportiq.backend.media;

import com.sportiq.backend.exception.VideoValidationException;
import com.sportiq.backend.support.TestMediaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VideoValidationServiceTest {

    private MediaProperties properties;
    private Path root;
    private VideoValidationService service;

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createTempDirectory("sportiq-validation-test");
        properties = new MediaProperties(root.toString(), 1, 5, 20, 10, 50L * 1024 * 1024, null);
        service = new VideoValidationService(properties, new FfmpegProperties("ffmpeg", "ffprobe"),
                new ProcessRunner());
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private MockMultipartFile validVideo() {
        return new MockMultipartFile("video", "clip.mp4", "video/mp4", TestMediaUtil.videoBytes(6));
    }

    @Test
    void acceptsValidSixSecondVideo() throws IOException {
        ValidatedVideo validated = service.validate(validVideo());

        assertTrue(validated.durationSeconds() >= 5.5 && validated.durationSeconds() <= 6.5);
        assertTrue(Files.isRegularFile(validated.path()));
        Files.deleteIfExists(validated.path());
    }

    @Test
    void acceptsTwentySecondVideoAtUpperBound() throws IOException {
        MockMultipartFile maxLength = new MockMultipartFile("video", "max.mp4", "video/mp4",
                TestMediaUtil.videoBytes(20));

        ValidatedVideo validated = service.validate(maxLength);

        assertTrue(validated.durationSeconds() >= 19.5 && validated.durationSeconds() <= 20.5);
        Files.deleteIfExists(validated.path());
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("video", "empty.mp4", "video/mp4", new byte[0]);

        assertThrows(VideoValidationException.class, () -> service.validate(empty));
    }

    @Test
    void rejectsOversizedFile() {
        org.springframework.web.multipart.MultipartFile oversized = mock(org.springframework.web.multipart.MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(50L * 1024 * 1024 + 1);
        when(oversized.getContentType()).thenReturn("video/mp4");

        assertThrows(VideoValidationException.class, () -> service.validate(oversized));
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile bad = new MockMultipartFile("video", "song.mp3", "audio/mpeg",
                TestMediaUtil.videoBytes(6));

        assertThrows(VideoValidationException.class, () -> service.validate(bad));
    }

    @Test
    void rejectsTooShortVideo() {
        MockMultipartFile shortClip = new MockMultipartFile("video", "short.mp4", "video/mp4",
                TestMediaUtil.videoBytes(2));

        assertThrows(VideoValidationException.class, () -> service.validate(shortClip));
    }

    @Test
    void rejectsTooLongVideo() {
        MockMultipartFile longClip = new MockMultipartFile("video", "long.mp4", "video/mp4",
                TestMediaUtil.videoBytes(21));

        assertThrows(VideoValidationException.class, () -> service.validate(longClip));
    }

    @Test
    void rejectedUploadLeavesNothingBehind() throws IOException {
        assertThrows(VideoValidationException.class,
                () -> service.validate(new MockMultipartFile("video", "long.mp4", "video/mp4",
                        TestMediaUtil.videoBytes(21))));

        try (java.util.stream.Stream<Path> files = Files.list(Path.of(properties.tempDir()))) {
            assertEquals(List.of(), files.toList(), "temp dir should be empty after rejected upload");
        }
    }
}