package com.sportiq.backend.ai;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.exception.LlmAnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Two-stage pipeline ({@code hld.md} §4, {@code lld.md} §2.5):
 * <ol>
 *   <li>moondream describes each extracted frame in plain text;</li>
 *   <li>phi4-mini aggregates all observations into one structured
 *       {@link AnalysisResult}.</li>
 * </ol>
 * A single unreadable frame is logged and skipped; a 10 minute budget bounds
 * the whole run; a structured-parse failure falls back to a manual JSON parse
 * with a single retry.
 */
@Service
public class SpringAiOllamaService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiOllamaService.class);

    private final ChatClient visionChatClient;
    private final ChatClient reasoningChatClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final long totalBudgetMs;

    public SpringAiOllamaService(@Qualifier("visionChatClient") ChatClient visionChatClient,
                                 @Qualifier("reasoningChatClient") ChatClient reasoningChatClient,
                                 PromptService promptService,
                                 ObjectMapper objectMapper,
                                 @Value("${sportiq.ai.budget-ms:600000}") long totalBudgetMs) {
        this.visionChatClient = visionChatClient;
        this.reasoningChatClient = reasoningChatClient;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
        this.totalBudgetMs = totalBudgetMs;
    }

    public List<String> describeFrames(Skill skill, List<Path> frames) {
        List<String> observations = new ArrayList<>();
        long start = System.nanoTime();
        for (Path frame : frames) {
            if (elapsedMs(start) > totalBudgetMs) {
                throw new LlmAnalysisException("Analysis exceeded the 10 minute budget");
            }
            try {
                observations.add(describeFrame(skill, frame));
            } catch (RuntimeException ex) {
                log.warn("Skipping unreadable frame {}: {}", frame, ex.getMessage());
                observations.add("Not readable");
            }
        }
        return observations;
    }

    private String describeFrame(Skill skill, Path frame) {
        Media media = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(new FileSystemResource(frame))
                .build();
        return visionChatClient.prompt()
                .user(user -> user
                        .text(promptService.visionPrompt(skill))
                        .media(media))
                .call()
                .content();
    }

    public AnalysisResult aggregate(Skill skill, List<String> observations) {
        return aggregate(skill, observations, observations.size(), 0);
    }

    public AnalysisResult aggregate(Skill skill, List<String> observations, int frameCount, double durationSeconds) {
        String prompt = promptService.reasoningPrompt(skill, observations, frameCount, durationSeconds);
        try {
            AnalysisResult result = reasoningChatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AnalysisResult.class);
            return AnalysisValidator.validate(result);
        } catch (Exception structuredFailure) {
            log.warn("Structured aggregation failed ({}); retrying with raw parse",
                    structuredFailure.getMessage());
            try {
                String content = reasoningChatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
                return AnalysisValidator.validate(parseManual(content));
            } catch (Exception rawFailure) {
                throw new LlmAnalysisException("Reasoning model did not return a valid analysis", rawFailure);
            }
        }
    }

    private AnalysisResult parseManual(String content) {
        String json = content;
        int fence = json.indexOf("```");
        if (fence != -1) {
            json = json.substring(json.indexOf('\n', fence) + 1);
            int endFence = json.lastIndexOf("```");
            if (endFence != -1) {
                json = json.substring(0, endFence);
            }
        }
        try {
            return objectMapper.readValue(json.trim(), AnalysisResult.class);
        } catch (tools.jackson.core.JacksonException ex) {
            throw new LlmAnalysisException("Reasoning model returned unparseable JSON", ex);
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}