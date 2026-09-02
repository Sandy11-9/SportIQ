package com.sportiq.backend.web;

import com.sportiq.backend.exception.ChatUnavailableException;
import com.sportiq.backend.exception.DuplicateEmailException;
import com.sportiq.backend.exception.EmailNotVerifiedException;
import com.sportiq.backend.exception.FrameExtractionException;
import com.sportiq.backend.exception.InvalidCredentialsException;
import com.sportiq.backend.exception.LlmAnalysisException;
import com.sportiq.backend.exception.MailSendException;
import com.sportiq.backend.exception.OtpException;
import com.sportiq.backend.exception.ResourceNotFoundException;
import com.sportiq.backend.exception.VideoValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({VideoValidationException.class, FrameExtractionException.class, LlmAnalysisException.class})
    public ResponseEntity<ApiError> handleUnprocessable(RuntimeException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtp(OtpException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MailSendException.class)
    public ResponseEntity<ApiError> handleMailSend(MailSendException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Could not send the verification email. Please try again later.");
    }

    @ExceptionHandler(ChatUnavailableException.class)
    public ResponseEntity<ApiError> handleChatUnavailable(RuntimeException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "SportIQ Assistant is temporarily unavailable. Please try again.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleInvalidArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .orElse("Validation failed");
        } else {
            message = ex.getMessage();
        }
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handlePayloadTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}