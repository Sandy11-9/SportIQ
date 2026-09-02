package com.sportiq.backend.ai;

import com.sportiq.backend.exception.LlmAnalysisException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates and normalizes a {@link AnalysisResult} produced by the reasoning
 * model before anything touches the database ({@code lld.md} §2.4).
 * Contract violations surface as an {@link LlmAnalysisException} (HTTP 422).
 */
public final class AnalysisValidator {

    private AnalysisValidator() {
    }

    /**
     * @return a normalized copy with trimmed, blank-free lists.
     * @throws LlmAnalysisException when the result violates the contract.
     */
    public static AnalysisResult validate(AnalysisResult result) {
        Objects.requireNonNull(result, "result");

        if (result.scores() == null || result.scores().isEmpty()) {
            throw new LlmAnalysisException("AnalysisResult.scores must contain at least one entry");
        }

        if (result.goodPoints() == null || result.badPoints() == null || result.improvements() == null) {
            throw new LlmAnalysisException("AnalysisResult point lists must not be null");
        }

        List<AnalysisResult.ScoreEntry> scores = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (AnalysisResult.ScoreEntry entry : result.scores()) {
            if (entry == null || entry.category() == null || entry.category().isBlank()) {
                throw new LlmAnalysisException("AnalysisResult score category must not be blank");
            }
            if (entry.score() == null || entry.score() < 0 || entry.score() > 100) {
                throw new LlmAnalysisException("AnalysisResult score must be between 0 and 100");
            }
            if (seen.contains(entry.category())) {
                throw new LlmAnalysisException("AnalysisResult contains duplicate category: " + entry.category());
            }
            seen.add(entry.category());
            scores.add(new AnalysisResult.ScoreEntry(entry.category().trim(), entry.score()));
        }

        return new AnalysisResult(scores, sanitize(result.goodPoints()), sanitize(result.badPoints()),
                sanitize(result.improvements()));
    }

    private static List<String> sanitize(List<String> points) {
        List<String> result = new ArrayList<>();
        for (String point : points) {
            if (point != null && !point.isBlank()) {
                result.add(point.trim());
            }
        }
        return result;
    }
}