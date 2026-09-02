package com.sportiq.backend.exception;

/** Wrong email/password on login. Maps to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}