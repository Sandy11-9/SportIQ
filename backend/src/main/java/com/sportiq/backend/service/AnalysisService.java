package com.sportiq.backend.service;

import com.sportiq.backend.ai.AnalysisResult;
import com.sportiq.backend.ai.SpringAiOllamaService;
import com.sportiq.backend.entity.AnalysisSession;
import com.sportiq.backend.entity.AppUser;
import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.entity.Sport;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.media.FrameExtractionService;
import com.sportiq.backend.media.MediaFileUtils;
import com.sportiq.backend.media.ValidatedVideo;
import com.sportiq.backend.media.VideoValidationService;
import com.sportiq.backend.repository.AnalysisSessionRepository;
import com.sportiq.backend.repository.AppUserRepository;
import com.sportiq.backend.repository.SkillRepository;
import com.sportiq.backend.repository.SportRepository;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates one analysis run: validate -> extract -> describe -> aggregate
 * -> persist. Uploaded video and extracted frames are always deleted, on both
 * success and failure paths ({@code hld.md} §3).
 */
@Service
public class AnalysisService {

    private final VideoValidationService videoValidationService;
    private final FrameExtractionService frameExtractionService;
    private final SpringAiOllamaService ollamaService;
    private final SportRepository sportRepository;
    private final SkillRepository skillRepository;
    private final AppUserRepository userRepository;
    private final AnalysisSessionRepository sessionRepository;

    public AnalysisService(VideoValidationService videoValidationService,
                           FrameExtractionService frameExtractionService,
                           SpringAiOllamaService ollamaService,
                           SportRepository sportRepository,
                           SkillRepository skillRepository,
                           AppUserRepository userRepository,
                           AnalysisSessionRepository sessionRepository) {
        this.videoValidationService = videoValidationService;
        this.frameExtractionService = frameExtractionService;
        this.ollamaService = ollamaService;
        this.sportRepository = sportRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public AnalysisSessionResponse analyze(Long userId, Integer sportId, Integer skillId, MultipartFile file) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Sport sport = sportRepository.findById(sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found"));
        Skill skill = skillRepository.findByIdAndSportId(skillId, sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));

        ValidatedVideo video = null;
        Path framesDir = null;
        try {
            video = videoValidationService.validate(file);
            List<Path> frames = frameExtractionService.extractFrames(video.path(), video.durationSeconds());
            framesDir = frames.get(0).getParent();
            List<String> observations = ollamaService.describeFrames(skill, frames);
            AnalysisResult result = ollamaService.aggregate(skill, observations, frames.size(), video.durationSeconds());

            AnalysisSession session = new AnalysisSession(
                    user, sport, skill, result.goodPoints(), result.badPoints(), result.improvements());
            for (AnalysisResult.ScoreEntry entry : result.scores()) {
                session.addScore(entry.category(), entry.score());
            }
            session = sessionRepository.save(session);
            return AnalysisSessionResponse.from(session);
        } finally {
            MediaFileUtils.deleteQuietly(video == null ? null : video.path());
            MediaFileUtils.deleteTree(framesDir);
        }
    }
}