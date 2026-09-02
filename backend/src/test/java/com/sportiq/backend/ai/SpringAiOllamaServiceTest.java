package com.sportiq.backend.ai;

import com.sportiq.backend.entity.Skill;
import com.sportiq.backend.exception.LlmAnalysisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiOllamaServiceTest {

    private final PromptService promptService = new PromptService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Skill skill = new Skill(1, null, "Shooting", "football_shooting");

    private ChatClient visionChat;
    private ChatClient reasoningChat;
    private ChatClient.ChatClientRequestSpec visionSpec;
    private ChatClient.ChatClientRequestSpec reasoningSpec;
    private ChatClient.PromptUserSpec userSpec;
    private ChatClient.CallResponseSpec visionCall;
    private ChatClient.CallResponseSpec reasoningCall;

    @BeforeEach
    void setUp() {
        visionChat = mock(ChatClient.class);
        reasoningChat = mock(ChatClient.class);
        visionSpec = mock(ChatClient.ChatClientRequestSpec.class);
        reasoningSpec = mock(ChatClient.ChatClientRequestSpec.class);
        userSpec = mock(ChatClient.PromptUserSpec.class);
        visionCall = mock(ChatClient.CallResponseSpec.class);
        reasoningCall = mock(ChatClient.CallResponseSpec.class);

        stubChain(visionChat, visionSpec, visionCall);
        stubChain(reasoningChat, reasoningSpec, reasoningCall);
    }

    private void stubChain(ChatClient chat, ChatClient.ChatClientRequestSpec spec,
                           ChatClient.CallResponseSpec call) {
        when(chat.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(any(Consumer.class))).thenAnswer(inv -> {
            Consumer<ChatClient.PromptUserSpec> consumer = inv.getArgument(0);
            consumer.accept(userSpec);
            return spec;
        });
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(userSpec.text(anyString())).thenReturn(userSpec);
        when(userSpec.media(any(Media.class))).thenReturn(userSpec);
    }

    private SpringAiOllamaService service(long budgetMs) {
        return new SpringAiOllamaService(visionChat, reasoningChat, promptService, objectMapper, budgetMs);
    }

    private Path tempFrame() throws IOException {
        return Files.createTempFile("sportiq-frame-", ".jpg");
    }

    @Test
    void describesEveryFrame() throws IOException {
        when(visionCall.content()).thenReturn("arm extended");
        Path frame1 = tempFrame();
        Path frame2 = tempFrame();

        List<String> observations = service(60_000).describeFrames(skill, List.of(frame1, frame2));

        assertEquals(List.of("arm extended", "arm extended"), observations);
        verify(visionSpec, times(2)).user(any(Consumer.class));
        verify(userSpec, times(2)).text(anyString());
        verify(userSpec, times(2)).media(any(Media.class));
        verify(visionSpec, never()).system(anyString());
    }

    @Test
    void skipsFailingFrameAndContinues() throws IOException {
        when(visionCall.content()).thenThrow(new RuntimeException("vision model error"))
                .thenReturn("clean follow-through");
        Path frame1 = tempFrame();
        Path frame2 = tempFrame();

        List<String> observations = service(60_000).describeFrames(skill, List.of(frame1, frame2));

        assertEquals(List.of("Not readable", "clean follow-through"), observations);
    }

    @Test
    void abortsWhenBudgetIsExhausted() throws IOException {
        Path frame1 = tempFrame();

        assertThrows(LlmAnalysisException.class, () -> service(-1).describeFrames(skill, List.of(frame1)));
        verify(visionChat, never()).prompt();
    }

    @Test
    void aggregateUsesStructuredParse() {
        when(reasoningCall.entity(AnalysisResult.class)).thenReturn(validResult());

        AnalysisResult result = service(60_000).aggregate(skill, List.of("frame a"));

        assertEquals(75, result.scores().get(0).score());
        assertEquals("Placement", result.scores().get(0).category());
        verify(reasoningCall, times(1)).entity(AnalysisResult.class);
    }

    @Test
    void aggregateFallsBackToRawJsonParse() {
        when(reasoningCall.entity(AnalysisResult.class)).thenThrow(new RuntimeException("converter failed"));
        when(reasoningCall.content()).thenReturn(fencedJson());

        AnalysisResult result = service(60_000).aggregate(skill, List.of("frame a"));

        assertEquals(75, result.scores().get(0).score());
        assertEquals(List.of("nice"), result.goodPoints());
        verify(reasoningCall, times(1)).content();
    }

    @Test
    void aggregateThrowsWhenBothPathsFail() {
        when(reasoningCall.entity(AnalysisResult.class)).thenThrow(new RuntimeException("converter failed"));
        when(reasoningCall.content()).thenReturn("this is definitely not json");

        assertThrows(LlmAnalysisException.class, () -> service(60_000).aggregate(skill, List.of("frame a")));
    }

    private AnalysisResult validResult() {
        return new AnalysisResult(
                List.of(new AnalysisResult.ScoreEntry("Placement", 75)),
                List.of("nice"),
                List.of("sloppy"),
                List.of("follow through"));
    }

    private String fencedJson() {
        return "```json\n"
                + "{\"scores\":[{\"category\":\"Placement\",\"score\":75}],"
                + "\"goodPoints\":[\"nice\"],\"badPoints\":[\"sloppy\"],\"improvements\":[\"follow through\"]}\n"
                + "```";
    }
}