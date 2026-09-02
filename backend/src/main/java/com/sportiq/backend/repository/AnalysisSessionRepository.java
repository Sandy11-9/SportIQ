package com.sportiq.backend.repository;

import com.sportiq.backend.entity.AnalysisSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisSessionRepository extends JpaRepository<AnalysisSession, Long> {

    Page<AnalysisSession> findByUserIdOrderByAnalyzedAtDesc(Long userId, Pageable pageable);

    Page<AnalysisSession> findByUserIdAndSportIdOrderByAnalyzedAtDesc(Long userId, Integer sportId, Pageable pageable);

    Page<AnalysisSession> findByUserIdAndSkillIdOrderByAnalyzedAtDesc(Long userId, Integer skillId, Pageable pageable);

    Page<AnalysisSession> findByUserIdAndSportIdAndSkillIdOrderByAnalyzedAtDesc(Long userId, Integer sportId,
                                                                               Integer skillId, Pageable pageable);

    Optional<AnalysisSession> findByIdAndUserId(Long id, Long userId);
}