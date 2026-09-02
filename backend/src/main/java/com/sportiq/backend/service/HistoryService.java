package com.sportiq.backend.service;

import com.sportiq.backend.entity.AnalysisSession;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.repository.AnalysisSessionRepository;
import com.sportiq.backend.web.dto.AnalysisSessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final AnalysisSessionRepository sessionRepository;

    public HistoryService(AnalysisSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public Page<AnalysisSessionResponse> list(Long userId, Integer sportId, Integer skillId, Pageable pageable) {
        Page<AnalysisSession> page;
        if (sportId != null && skillId != null) {
            page = sessionRepository.findByUserIdAndSportIdAndSkillIdOrderByAnalyzedAtDesc(userId, sportId, skillId, pageable);
        } else if (sportId != null) {
            page = sessionRepository.findByUserIdAndSportIdOrderByAnalyzedAtDesc(userId, sportId, pageable);
        } else if (skillId != null) {
            page = sessionRepository.findByUserIdAndSkillIdOrderByAnalyzedAtDesc(userId, skillId, pageable);
        } else {
            page = sessionRepository.findByUserIdOrderByAnalyzedAtDesc(userId, pageable);
        }
        return page.map(AnalysisSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public AnalysisSessionResponse get(Long userId, Long sessionId) {
        return AnalysisSessionResponse.from(sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis session not found")));
    }
}