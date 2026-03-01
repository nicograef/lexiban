package de.nicograef.iban.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global error handler for all REST controllers.
 * Catches exceptions and returns consistent JSON error responses.
 * See lernfragen.md → "@RestControllerAdvice" for the pattern.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Handles @Valid validation failures → HTTP 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(MethodArgumentNotValidException ex) {
        return Map.of(
                "error", "Validation failed",
                "details", ex.getBindingResult().getAllErrors().stream()
                        .map(e -> e.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Unknown validation error"));
    }

    /**
     * Catch-all for unhandled exceptions.
     * Returns HTTP 500 — never exposes internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericError(Exception ex) {
        return Map.of("error", "Internal server error");
    }
}
