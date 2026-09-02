package com.sportiq.backend.web;

import java.time.Instant;

/** Stable JSON error body returned by {@link GlobalExceptionHandler}. */
public record ApiError(int status, String error, String message, Instant timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}