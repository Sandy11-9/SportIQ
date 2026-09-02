package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.AnalysisSession;

import java.time.Instant;
import java.util.List;

public record AnalysisSessionResponse(
        Long id,
        Integer sportId,
        String sportName,
        Integer skillId,
        String skillName,
        Instant analyzedAt,
        List<ScoreResponse> scores,
        List<String> goodPoints,
        List<String> badPoints,
        List<String> improvements) {

    public static AnalysisSessionResponse from(AnalysisSession session) {
        return new AnalysisSessionResponse(
                session.getId(),
                session.getSport().getId(),
                session.getSport().getName(),
                session.getSkill().getId(),
                session.getSkill().getName(),
                session.getAnalyzedAt(),
                session.getScores().stream().map(ScoreResponse::from).toList(),
                session.getGoodPoints(),
                session.getBadPoints(),
                session.getImprovements());
    }
}