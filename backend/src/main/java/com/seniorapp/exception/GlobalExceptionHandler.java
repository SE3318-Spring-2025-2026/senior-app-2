package com.seniorapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>All error responses follow the same JSON shape:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2025-01-01T00:00:00Z",
 *   "status":    403,
 *   "error":     "Forbidden",
 *   "message":   "...",
 *   "path":      "/api/..."
 * }
 * }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------
    // 403 – Student not on the whitelist
    // -------------------------------------------------------

    /**
     * Handles the case where a student's GitHub e-mail is not pre-approved.
     */
    @ExceptionHandler(StudentNotWhitelistedException.class)
    public ResponseEntity<Map<String, Object>> handleStudentNotWhitelisted(
            StudentNotWhitelistedException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // -------------------------------------------------------
    // 403 – Spring Security access denied
    // -------------------------------------------------------

    /**
     * Handles Spring Security {@link AccessDeniedException} (role mismatch, etc.).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.", request);
    }

    // -------------------------------------------------------
    // 400 – Bean Validation failures
    // -------------------------------------------------------

    /**
     * Handles {@code @Valid} constraint violations and returns field-level details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, details, request);
    }

    // -------------------------------------------------------
    // 400 – Generic RuntimeException (business-logic errors)
    // -------------------------------------------------------

    /**
     * Catch-all handler for {@link RuntimeException} thrown by service classes.
     * Returns 400 Bad Request with the exception message.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // -------------------------------------------------------
    // 500 – Unexpected exceptions
    // -------------------------------------------------------

    /**
     * Last-resort handler for any unhandled {@link Exception}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    /**
     * Builds a consistent JSON error response body.
     *
     * @param status  HTTP status to return
     * @param message human-readable description
     * @param request the current HTTP request (used for path)
     * @return a {@link ResponseEntity} with the error body
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
