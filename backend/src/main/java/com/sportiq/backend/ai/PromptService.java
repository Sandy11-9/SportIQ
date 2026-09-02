package com.sportiq.backend.ai;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the prompt pair for a {@link Skill} and assembles the reasoning
 * message from the per-frame observations, telling the model how long the
 * clip was and how many frames were sampled so it coaches the whole action
 * rather than treating frames as unrelated stills. Observations are embedded
 * as an unlabelled, unordered list and the reasoning prompts forbid repeating
 * labels, so the model never reports results per frame.
 */
@Service
public class PromptService {

    public SkillPrompt resolve(Skill skill) {
        return SkillPrompt.fromKey(skill.getPromptKey())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No prompt configured for skill: " + skill.getPromptKey()));
    }

    public String visionPrompt(Skill skill) {
        return resolve(skill).getVisionPrompt();
    }

    public String reasoningPrompt(Skill skill, List<String> frameObservations) {
        return reasoningPrompt(skill, frameObservations, frameObservations.size(), 0);
    }

    public String reasoningPrompt(Skill skill, List<String> frameObservations, int frameCount, double durationSeconds) {
        StringBuilder observations = new StringBuilder();
        for (int i = 0; i < frameObservations.size(); i++) {
            observations.append("- ")
                    .append(frameObservations.get(i))
                    .append(System.lineSeparator());
        }
        StringBuilder context = new StringBuilder();
        if (frameCount > 0) {
            context.append("Video length: ").append(frameCount).append(" sampled frames");
        }
        if (durationSeconds > 0) {
            context.append(context.isEmpty() ? "" : ", ")
                    .append(String.format(Locale.ROOT, "%.1f", durationSeconds))
                    .append(" seconds of footage");
        }
        return resolve(skill).getReasoningPrompt()
                + System.lineSeparator()
                + (context.isEmpty() ? "" : context + System.lineSeparator())
                + "Raw observations (unordered, anonymous):"
                + System.lineSeparator()
                + observations;
    }
}
