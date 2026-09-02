package com.sportiq.backend.exception;

/** SMTP transport failed while sending the verification email. Maps to HTTP 503. */
public class MailSendException extends RuntimeException {

    public MailSendException(String message) {
        super(message);
    }
}
