package com.eduauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralised exception handler — converts all exceptions into the standard
 * API response format:
 *
 *   Success: { "success": true,  "message": "...", "data":   { } }
 *   Error:   { "success": false, "message": "...", "errors": { "field": "msg" } }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 422 Unprocessable Entity — Bean Validation failures ──────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>()).add(error.getDefaultMessage());
        }

        return buildError(HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation failed", fieldErrors);
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    @ExceptionHandler({ResourceNotFoundException.class, org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(
            Exception ex) {

        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // ── 400 Bad Request ───────────────────────────────────────────────────────
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            BadRequestException ex) {

        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex) {

        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return buildError(HttpStatus.FORBIDDEN, "Access denied", Map.of());
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Log stack trace in dev; swap for a proper logger later
        ex.printStackTrace();
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.toString(), Map.of());
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, Object errors) {

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("errors", errors);
        return ResponseEntity.status(status).body(body);
    }
}
