package com.sportiq.backend.ai;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptServiceTest {

    private final PromptService promptService = new PromptService();

    private Skill skill(String promptKey) {
        return new Skill(1, null, "x", promptKey);
    }

    @Test
    void resolvesEverySeededPromptKey() {
        List.of("cricket_bowling", "cricket_batting", "football_shooting",
                        "football_passing", "football_dribbling")
                .forEach(key -> assertTrue(SkillPrompt.fromKey(key).isPresent(),
                        "missing prompt for " + key));
    }

    @Test
    void unknownKeyThrowsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> promptService.resolve(skill("hockey_slap")));
    }

    @Test
    void visionPromptIsASingleDirectDescriptiveSentence() {
        String vision = promptService.visionPrompt(skill("cricket_bowling"));
        assertTrue(vision.startsWith("Describe the bowler's"));
        assertTrue(vision.endsWith("in this frame."));
        assertTrue(vision.contains("arm and wrist position"));
        assertTrue(!vision.contains("Return ONLY a single JSON object"));
        assertTrue(!vision.contains("No JSON"));
        assertTrue(!vision.contains("plain text"));
    }

    @Test
    void reasoningPromptEmbedsObservationsAndJsonShape() {
        String reasoning = promptService.reasoningPrompt(skill("football_passing"),
                List.of("plant foot is wide", "head is down"));

        assertTrue(reasoning.contains("- plant foot is wide"));
        assertTrue(reasoning.contains("- head is down"));
        assertTrue(reasoning.contains("goodPoints"));
        assertTrue(reasoning.contains("\"category\""));
        assertTrue(reasoning.contains("Never mention frame numbers"));
        assertEquals(5, SkillPrompt.values().length);
    }
}