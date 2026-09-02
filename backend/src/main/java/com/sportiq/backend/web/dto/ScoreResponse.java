package com.sportiq.backend.web.dto;

import com.sportiq.backend.entity.AnalysisScore;

public record ScoreResponse(String category, Integer score) {

    public static ScoreResponse from(AnalysisScore score) {
        return new ScoreResponse(score.getCategory(), score.getValue());
    }
}