package com.sportiq.backend.exception;

/** Login attempted before the email OTP was confirmed. Maps to HTTP 403. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
