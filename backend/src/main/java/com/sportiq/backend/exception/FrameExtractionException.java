package com.sportiq.backend.exception;

/**
 * ffmpeg could not extract the required frames from a validated video.
 * Maps to HTTP 422.
 */
public class FrameExtractionException extends RuntimeException {

    public FrameExtractionException(String message) {
        super(message);
    }

    public FrameExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}