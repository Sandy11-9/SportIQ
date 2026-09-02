package com.sportiq.backend.ai;

import com.sportiq.backend.exception.LlmAnalysisException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisValidatorTest {

    private AnalysisResult valid() {
        return new AnalysisResult(
                List.of(new AnalysisResult.ScoreEntry("Action", 85), new AnalysisResult.ScoreEntry("Balance", 70)),
                List.of("good grip"),
                List.of("too short"),
                List.of("follow through"));
    }

    private AnalysisResult withScores(AnalysisResult.ScoreEntry... entries) {
        return new AnalysisResult(List.of(entries), List.of("good grip"), List.of("too short"), List.of("follow through"));
    }

    @Test
    void acceptsValidResult() {
        AnalysisResult out = AnalysisValidator.validate(valid());
        assertEquals(2, out.scores().size());
    }

    @Test
    void rejectsNullScores() {
        assertThrows(LlmAnalysisException.class,
                () -> AnalysisValidator.validate(new AnalysisResult(null, List.of(), List.of(), List.of())));
    }

    @Test
    void rejectsEmptyScores() {
        assertThrows(LlmAnalysisException.class, () -> AnalysisValidator.validate(withScores()));
    }

    @Test
    void rejectsScoreAboveRange() {
        AnalysisResult bad = withScores(new AnalysisResult.ScoreEntry("Action", 101));
        assertThrows(LlmAnalysisException.class, () -> AnalysisValidator.validate(bad));
    }

    @Test
    void rejectsNegativeScore() {
        AnalysisResult bad = withScores(new AnalysisResult.ScoreEntry("Action", -1));
        assertThrows(LlmAnalysisException.class, () -> AnalysisValidator.validate(bad));
    }

    @Test
    void rejectsBlankCategory() {
        AnalysisResult bad = withScores(new AnalysisResult.ScoreEntry("  ", 50));
        assertThrows(LlmAnalysisException.class, () -> AnalysisValidator.validate(bad));
    }

    @Test
    void rejectsDuplicateCategories() {
        AnalysisResult bad = withScores(
                new AnalysisResult.ScoreEntry("Action", 80), new AnalysisResult.ScoreEntry("Action", 60));
        assertThrows(LlmAnalysisException.class, () -> AnalysisValidator.validate(bad));
    }

    @Test
    void rejectsNullPointLists() {
        assertThrows(LlmAnalysisException.class,
                () -> AnalysisValidator.validate(new AnalysisResult(
                        List.of(new AnalysisResult.ScoreEntry("A", 50)), null, List.of(), List.of())));
    }

    @Test
    void trimsAndDropsBlankPoints() {
        AnalysisResult out = AnalysisValidator.validate(new AnalysisResult(
                List.of(new AnalysisResult.ScoreEntry(" Action ", 85)),
                Arrays.asList("  good grip ", "   ", null),
                List.of(),
                List.of("follow through")));

        assertEquals(List.of("good grip"), out.goodPoints());
        assertEquals("Action", out.scores().get(0).category());
    }
}