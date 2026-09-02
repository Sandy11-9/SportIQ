package com.sportiq.backend.service;

import com.sportiq.backend.exception.ChatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private ChatClient chat;
    private ChatClient.ChatClientRequestSpec spec;
    private ChatClient.CallResponseSpec call;
    private ChatService service;

    @BeforeEach
    void setUp() {
        chat = mock(ChatClient.class);
        spec = mock(ChatClient.ChatClientRequestSpec.class);
        call = mock(ChatClient.CallResponseSpec.class);

        when(chat.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);

        service = new ChatService(chat, 5_000);
    }

    @Test
    void answersSportsQuestionThroughOllama() {
        when(call.content()).thenReturn("Keep your head still and watch the ball through the release.");

        String reply = service.reply("How can I improve my cricket batting?");

        assertEquals("Keep your head still and watch the ball through the release.", reply);
        verify(chat).prompt();
    }

    @Test
    void passesSportRelatedOffTopicTermsToModel() {
        when(call.content()).thenReturn("Swing depends on the seam, the shine and the weather.");

        String reply = service.reply("How does the weather affect swing bowling?");

        assertEquals("Swing depends on the seam, the shine and the weather.", reply);
        verify(chat).prompt();
    }

    @Test
    void refusesNonSportQuestionWithoutCallingOllama() {
        String reply = service.reply("What is Java?");

        assertEquals(ChatService.NON_SPORT_REPLY, reply);
        verify(chat, never()).prompt();
    }

    @Test
    void refusesProgrammingRequest() {
        assertEquals(ChatService.NON_SPORT_REPLY, service.reply("Write Python code."));
        verify(chat, never()).prompt();
    }

    @Test
    void refusesGeneralKnowledgeQuestion() {
        assertEquals(ChatService.NON_SPORT_REPLY, service.reply("What is the capital of India?"));
        verify(chat, never()).prompt();
    }

    @Test
    void refusesCookingQuestion() {
        assertEquals(ChatService.NON_SPORT_REPLY, service.reply("How do I cook biryani?"));
        verify(chat, never()).prompt();
    }

    @Test
    void refusesJokeRequest() {
        assertEquals(ChatService.NON_SPORT_REPLY, service.reply("Tell me a joke."));
        verify(chat, never()).prompt();
    }

    @Test
    void rejectsEmptyMessage() {
        assertThrows(IllegalArgumentException.class, () -> service.reply("   "));
        verify(chat, never()).prompt();
    }

    @Test
    void rejectsOversizedMessage() {
        String message = "a".repeat(2001);
        assertThrows(IllegalArgumentException.class, () -> service.reply(message));
        verify(chat, never()).prompt();
    }

    @Test
    void mapsOllamaFailureToUnavailable() {
        when(call.content()).thenThrow(new RuntimeException("connection refused"));

        assertThrows(ChatUnavailableException.class, () -> service.reply("How do I improve my serve?"));
    }

    @Test
    void mapsBlankModelResponseToUnavailable() {
        when(call.content()).thenReturn(" ");

        assertThrows(ChatUnavailableException.class, () -> service.reply("How do I improve my serve?"));
    }

    @Test
    void mapsSlowModelToUnavailable() {
        when(call.content()).thenAnswer(invocation -> {
            Thread.sleep(200);
            return "late reply";
        });
        ChatService slow = new ChatService(chat, 20);

        assertThrows(ChatUnavailableException.class,
                () -> slow.reply("How do I improve my free throw?"));
    }
}