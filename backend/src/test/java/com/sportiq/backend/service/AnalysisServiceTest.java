package com.sportiq.backend.service;

import com.sportiq.backend.ai.AnalysisResult;
import com.sportiq.backend.ai.SpringAiOllamaService;
import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.entity.AnalysisSession;
import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.exception.LlmAnalysisException;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.media.FrameExtractionService;
import com.sportiq.backend.media.ValidatedVideo;
import com.sportiq.backend.media.VideoValidationService;
import com.sportiq.backend.repository.AnalysisSessionRepository;
import com.sportiq.backend.repository.AppUserRepository;
import com.sportiq.backend.repository.SkillRepository;
import com.sportiq.backend.repository.SportRepository;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private Path root;
    private VideoValidationService videoValidationService;
    private FrameExtractionService frameExtractionService;
    private SpringAiOllamaService ollamaService;
    private AnalysisSessionRepository sessionRepository;
    private AnalysisService service;

    private final Sport sport = new Sport(2, "Football");
    private final Skill skill = new Skill(4, sport, "Passing", "football_passing");

    @BeforeEach
    void setUp() throws IOException {
        root = Files.createTempDirectory("sportiq-analysis-test");
        videoValidationService = mock(VideoValidationService.class);
        frameExtractionService = mock(FrameExtractionService.class);
        ollamaService = mock(SpringAiOllamaService.class);
        sessionRepository = mock(AnalysisSessionRepository.class);

        AppUserRepository userRepository = mock(AppUserRepository.class);
        SportRepository sportRepository = mock(SportRepository.class);
        SkillRepository skillRepository = mock(SkillRepository.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(new AppUser("a@b.dev", "h", "Coach")));
        when(sportRepository.findById(2)).thenReturn(Optional.of(sport));
        when(skillRepository.findByIdAndSportId(4, 2)).thenReturn(Optional.of(skill));
        when(sessionRepository.save(any(AnalysisSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new AnalysisService(videoValidationService, frameExtractionService, ollamaService,
                sportRepository, skillRepository, userRepository, sessionRepository);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (root != null) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private Path tempFramesDir() throws IOException {
        Path dir = Files.createDirectories(root.resolve("frames"));
        Files.write(dir.resolve("frame_001.jpg"), new byte[]{1});
        Files.write(dir.resolve("frame_002.jpg"), new byte[]{2});
        return dir;
    }

    private void stubSuccessfulPipeline(Path videoPath, Path framesDir) throws IOException {
        when(videoValidationService.validate(any()))
                .thenReturn(new ValidatedVideo(videoPath, 6.0));
        when(frameExtractionService.extractFrames(any(), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(List.of(framesDir.resolve("frame_001.jpg"), framesDir.resolve("frame_002.jpg")));
        when(ollamaService.describeFrames(any(Skill.class), any())).thenReturn(List.of("a", "b"));
        when(ollamaService.aggregate(any(Skill.class), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyDouble())).thenReturn(new AnalysisResult(
                List.of(new AnalysisResult.ScoreEntry("Accuracy", 80)),
                List.of("grip"), List.of("lazy"), List.of("follow through")));
    }

    @Test
    void runsPipelineAndPersistsResult() throws IOException {
        Path video = Files.createTempFile(root.toAbsolutePath(), "clip", ".mp4");
        Path framesDir = tempFramesDir();
        stubSuccessfulPipeline(video, framesDir);

        AnalysisSessionResponse response =
                service.analyze(1L, 2, 4, new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[]{1}));

        assertEquals(Integer.valueOf(80), response.scores().get(0).score());
        assertEquals("Passing", response.skillName());
        assertEquals(List.of("grip"), response.goodPoints());
        verify(sessionRepository).save(any(AnalysisSession.class));
    }

    @Test
    void cleansUpVideoAndFramesOnSuccess() throws IOException {
        Path video = Files.createTempFile(root.toAbsolutePath(), "clip", ".mp4");
        Path framesDir = tempFramesDir();
        stubSuccessfulPipeline(video, framesDir);

        service.analyze(1L, 2, 4, new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[]{1}));

        assertFalse(Files.exists(video), "uploaded video should be deleted");
        assertFalse(Files.exists(framesDir), "frame dir should be deleted");
    }

    @Test
    void cleansUpVideoAndFramesOnFailure() throws IOException {
        Path video = Files.createTempFile(root.toAbsolutePath(), "clip", ".mp4");
        Path framesDir = tempFramesDir();
        stubSuccessfulPipeline(video, framesDir);
        when(ollamaService.aggregate(any(Skill.class), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyDouble()))
                .thenThrow(new LlmAnalysisException("aggregation failed"));

        assertThrows(LlmAnalysisException.class,
                () -> service.analyze(1L, 2, 4, new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[]{1})));

        assertFalse(Files.exists(video));
        assertFalse(Files.exists(framesDir));
    }

    @Test
    void rejectsUnknownSportBeforeMediaWork() {
        org.springframework.web.multipart.MultipartFile file =
                new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[]{1});

        assertThrows(ResourceNotFoundException.class, () -> service.analyze(1L, 99, 4, file));
        verify(videoValidationService, org.mockito.Mockito.never()).validate(any());
    }

    @Test
    void rejectsSkillNotInSport() {
        org.springframework.web.multipart.MultipartFile file =
                new MockMultipartFile("video", "clip.mp4", "video/mp4", new byte[]{1});

        assertThrows(ResourceNotFoundException.class, () -> service.analyze(1L, 2, 99, file));
    }
}