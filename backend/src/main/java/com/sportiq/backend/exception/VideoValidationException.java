package com.sportiq.backend.exception;

/**
 * Uploaded video failed one of the file checks
 * (type, size, duration bounds, frame count). Maps to HTTP 422.
 */
public class VideoValidationException extends RuntimeException {

    public VideoValidationException(String message) {
        super(message);
    }

    public VideoValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}