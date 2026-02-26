package com.iban.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global error handler for all REST controllers.
 *
 * ── Analogy ──
 * In Express.js this is the error-handling middleware:
 *   app.use((err, req, res, next) => {
 *     if (err instanceof ValidationError) res.status(400).json({...})
 *     else res.status(500).json({ error: 'Internal server error' })
 *   })
 * In Go, this is centralized error wrapping in your HTTP handler.
 *
 * @RestControllerAdvice combines:
 *   - @ControllerAdvice — intercepts exceptions thrown by any @RestController
 *   - @ResponseBody — return values are serialized to JSON automatically
 * It catches exceptions BEFORE they become an ugly default error page.
 *
 * Each @ExceptionHandler method handles a specific exception type.
 * @ResponseStatus sets the HTTP status code for the response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors (e.g. @NotBlank on IbanRequest fails).
     * Returns HTTP 400 with a structured error message.
     *
     * MethodArgumentNotValidException is thrown by Spring when @Valid
     * validation fails — ≈ zod's ZodError in TypeScript.
     */
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
