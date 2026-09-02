package com.sportiq.backend.ai;

import java.util.List;

/**
 * Structured result produced only by the reasoning model (phi4-mini).
 * Frames are never persisted ({@code lld.md} §2.5); these lists are.
 */
public record AnalysisResult(
        List<ScoreEntry> scores,
        List<String> goodPoints,
        List<String> badPoints,
        List<String> improvements) {

    public record ScoreEntry(String category, Integer score) {
    }
}