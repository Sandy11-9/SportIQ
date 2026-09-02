package com.sportiq.backend.exception;

/** OTP is missing, wrong, expired, or already verified. Maps to HTTP 400. */
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
