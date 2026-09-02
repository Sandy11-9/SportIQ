package com.sportiq.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Message sent to the SportIQ Assistant chatbot.
 */
public record ChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 2000, message = "message must be at most 2000 characters")
        String message) {
}