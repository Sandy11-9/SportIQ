package com.sportiq.backend.exception;

/**
 * Raised when the AI pipeline cannot deliver a valid structured analysis:
 * frame description failure, aggregation (phi4-mini) failure or an
 * unrecoverably malformed result ({@code hld.md} §4, {@code lld.md} §2.5).
 */
public class LlmAnalysisException extends RuntimeException {

    public LlmAnalysisException(String message) {
        super(message);
    }

    public LlmAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}