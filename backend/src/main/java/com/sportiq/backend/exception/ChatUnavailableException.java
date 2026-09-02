package com.sportiq.backend.exception;

/**
 * Raised when the SportIQ Assistant cannot reach Ollama in time or the
 * model returns an unusable response. Details are never leaked to clients.
 */
public class ChatUnavailableException extends RuntimeException {

    public ChatUnavailableException(String message) {
        super(message);
    }

    public ChatUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}