package com.sportiq.backend.exception;

/** Registration with an already-taken email. Maps to HTTP 409. */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}