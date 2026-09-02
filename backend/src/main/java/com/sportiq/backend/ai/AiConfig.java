package com.sportiq.backend.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires dedicated {@link ChatClient}s, one per pipeline stage
 * ({@code lld.md} §2.5) plus one for the SportIQ Assistant chatbot. The
 * shared {@link OllamaApi} comes from Spring AI autoconfiguration; each
 * client carries its own model and sampling options.
 */
@Configuration
public class AiConfig {

    @Bean("visionChatClient")
    ChatClient visionChatClient(OllamaApi ollamaApi,
                                @Value("${sportiq.ai.vision-model}") String visionModel) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(visionModel)
                .temperature(0.2)
                .numPredict(256)
                .build();
        return ChatClient.builder(ollamaChatModel(ollamaApi, options)).build();
    }

    @Bean("reasoningChatClient")
    ChatClient reasoningChatClient(OllamaApi ollamaApi,
                                   @Value("${sportiq.ai.reasoning-model}") String reasoningModel) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(reasoningModel)
                .temperature(0.0)
                .format("json")
                .numPredict(1024)
                .build();
        return ChatClient.builder(ollamaChatModel(ollamaApi, options)).build();
    }

    @Bean("assistantChatClient")
    ChatClient assistantChatClient(OllamaApi ollamaApi,
                                   @Value("${sportiq.ai.reasoning-model}") String reasoningModel) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(reasoningModel)
                .temperature(0.7)
                .numPredict(512)
                .build();
        return ChatClient.builder(ollamaChatModel(ollamaApi, options)).build();
    }

    @Bean("dietChatClient")
    ChatClient dietChatClient(OllamaApi ollamaApi,
                              @Value("${sportiq.ai.reasoning-model}") String reasoningModel) {
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(reasoningModel)
                .temperature(0.2)
                .format("json")
                .numPredict(1024)
                .build();
        return ChatClient.builder(ollamaChatModel(ollamaApi, options)).build();
    }

    private ChatModel ollamaChatModel(OllamaApi ollamaApi, OllamaChatOptions options) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(options)
                .build();
    }
}