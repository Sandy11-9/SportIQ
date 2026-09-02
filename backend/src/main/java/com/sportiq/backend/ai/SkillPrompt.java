package com.sportiq.backend.ai;

import java.util.Arrays;
import java.util.Optional;

/**
 * Per-skill prompt pair. {@code visionPrompt} drives the vision model
 * (moondream) on a single frame; {@code reasoningPrompt} drives the reasoning
 * model (phi4-mini) on the whole video's worth of observations. The two models
 * cannot share one prompt: moondream outputs free text, phi4-mini must emit
 * the strict {@link AnalysisResult} JSON ({@code lld.md} §2.5). Reasoning
 * prompts treat the input as one continuous clip (dynamic 1.5 fps frame count)
 * and are written so the model stays effective regardless of clip length.
 */
public enum SkillPrompt {

    CRICKET_BOWLING("cricket_bowling",
            "Describe the bowler's run-up posture, front foot placement, arm and wrist position, balance and follow-through in this frame.",
            "You are a cricket bowling coach. You receive observations sampled from a single continuous "
                    + "bowling action video at 1.5 frames per second. Study the whole sequence as one delivery "
                    + "and coach the bowler. Aggregate everything into one structured analysis. "
                    + "Scores must be integers from 0 to 100 covering the categories "
                    + "\"Action\", \"Timing\", \"Balance\" and \"Result\". "
                    + "Return ONLY a single JSON object with this exact shape: "
                    + "{\"scores\":[{\"category\":\"Action\",\"score\":0}],"
                    + "\"goodPoints\":[],\"badPoints\":[],\"improvements\":[]}. "
                    + "goodPoints, badPoints and improvements must each contain 3 to 5 short, concrete, "
                    + "coaching bullets about the delivery as a whole. "
                    + "Never mention frame numbers, sample numbers, timestamps, observation counts or any reference "
                    + "to individual observations. Discuss only the overall delivery. "
                    + "No markdown, no text before or after the JSON."),

    CRICKET_BATTING("cricket_batting",
            "Describe the batsman's stance and grip, head position, back-lift, footwork, balance and shot intent in this frame.",
            "You are a cricket batting coach. You receive observations sampled from a single continuous "
                    + "batting shot video at 1.5 frames per second. Study the whole sequence as one shot "
                    + "and coach the batsman. Aggregate everything into one structured analysis. "
                    + "Scores must be integers from 0 to 100 covering the categories "
                    + "\"Stance\", \"Timing\", \"Footwork\" and \"Result\". "
                    + "Return ONLY a single JSON object with this exact shape: "
                    + "{\"scores\":[{\"category\":\"Stance\",\"score\":0}],"
                    + "\"goodPoints\":[],\"badPoints\":[],\"improvements\":[]}. "
                    + "goodPoints, badPoints and improvements must each contain 3 to 5 short, concrete, "
                    + "coaching bullets about the shot as a whole. "
                    + "Never mention frame numbers, sample numbers, timestamps, observation counts or any reference "
                    + "to individual observations. Discuss only the overall shot. "
                    + "No markdown, no text before or after the JSON."),

    FOOTBALL_SHOOTING("football_shooting",
            "Describe the shooter's plant foot position, kicking leg swing, ankle lock, body lean and contact point in this frame.",
            "You are a football shooting coach. You receive observations sampled from a single continuous "
                    + "shooting video at 1.5 frames per second. Study the whole sequence as one attempt "
                    + "and coach the shooter. Aggregate everything into one structured analysis. "
                    + "Scores must be integers from 0 to 100 covering the categories "
                    + "\"Placement\", \"Power\", \"Timing\" and \"Follow-through\". "
                    + "Return ONLY a single JSON object with this exact shape: "
                    + "{\"scores\":[{\"category\":\"Placement\",\"score\":0}],"
                    + "\"goodPoints\":[],\"badPoints\":[],\"improvements\":[]}. "
                    + "goodPoints, badPoints and improvements must each contain 3 to 5 short, concrete, "
                    + "coaching bullets about the attempt as a whole. "
                    + "Never mention frame numbers, sample numbers, timestamps, observation counts or any reference "
                    + "to individual observations. Discuss only the overall attempt. "
                    + "No markdown, no text before or after the JSON."),

    FOOTBALL_PASSING("football_passing",
            "Describe the passer's body position, kicking surface used, head position and accuracy of contact in this frame.",
            "You are a football passing coach. You receive observations sampled from a single continuous "
                    + "passing video at 1.5 frames per second. Study the whole sequence as one pass "
                    + "and coach the passer. Aggregate everything into one structured analysis. "
                    + "Scores must be integers from 0 to 100 covering the categories "
                    + "\"Accuracy\", \"Weight\", \"Timing\" and \"Control\". "
                    + "Return ONLY a single JSON object with this exact shape: "
                    + "{\"scores\":[{\"category\":\"Accuracy\",\"score\":0}],"
                    + "\"goodPoints\":[],\"badPoints\":[],\"improvements\":[]}. "
                    + "goodPoints, badPoints and improvements must each contain 3 to 5 short, concrete, "
                    + "coaching bullets about the pass as a whole. "
                    + "Never mention frame numbers, sample numbers, timestamps, observation counts or any reference "
                    + "to individual observations. Discuss only the overall pass. "
                    + "No markdown, no text before or after the JSON."),

    FOOTBALL_DRIBBLING("football_dribbling",
            "Describe the player's touch distance, balance over the ball, ball control and change of direction in this frame.",
            "You are a football dribbling coach. You receive observations sampled from a single continuous "
                    + "dribbling video at 1.5 frames per second. Study the whole sequence as one run "
                    + "and coach the player. Aggregate everything into one structured analysis. "
                    + "Scores must be integers from 0 to 100 covering the categories "
                    + "\"Control\", \"Balance\", \"Touch\" and \"Speed\". "
                    + "Return ONLY a single JSON object with this exact shape: "
                    + "{\"scores\":[{\"category\":\"Control\",\"score\":0}],"
                    + "\"goodPoints\":[],\"badPoints\":[],\"improvements\":[]}. "
                    + "goodPoints, badPoints and improvements must each contain 3 to 5 short, concrete, "
                    + "coaching bullets about the run as a whole. "
                    + "Never mention frame numbers, sample numbers, timestamps, observation counts or any reference "
                    + "to individual observations. Discuss only the overall run. "
                    + "No markdown, no text before or after the JSON.");

    private final String promptKey;
    private final String visionPrompt;
    private final String reasoningPrompt;

    SkillPrompt(String promptKey, String visionPrompt, String reasoningPrompt) {
        this.promptKey = promptKey;
        this.visionPrompt = visionPrompt;
        this.reasoningPrompt = reasoningPrompt;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getVisionPrompt() {
        return visionPrompt;
    }

    public String getReasoningPrompt() {
        return reasoningPrompt;
    }

    public static Optional<SkillPrompt> fromKey(String promptKey) {
        return Arrays.stream(values())
                .filter(skill -> skill.promptKey.equals(promptKey))
                .findFirst();
    }
}
