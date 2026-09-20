package com.acme.salary.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Translates exceptions into the uniform error envelope with the appropriate HTTP status. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String message = "Invalid value for parameter '%s'".formatted(exception.getName());
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, "Request validation failed", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody() {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, "Request body is missing or malformed"));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND, exception.getMessage()));
    }

    /** Requests are validated up front, so a remaining integrity violation is a uniqueness clash. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ErrorCode.CONFLICT, "An employee with this email already exists"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Unexpected server error"));
    }
}
