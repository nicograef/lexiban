package de.nicograef.iban.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.nicograef.iban.model.IbanFormatException;

/**
 * Global error handler for all REST controllers.
 * Catches exceptions and returns consistent JSON error responses.
 * See lernfragen.md → "@RestControllerAdvice" for the pattern.
 *
 * TS analogy: Express error-handling middleware that checks the error type
 * and sets the appropriate HTTP status code + JSON body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Handles @Valid validation failures (e.g. @NotBlank) → HTTP 400. */
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
     * Handles structurally malformed IBAN input → HTTP 400.
     *
     * This is thrown by IbanNumber's constructor when the raw input doesn't
     * match the ISO 13616 structural pattern (too short, too long, wrong
     * character positions). The request itself is invalid — it's not a
     * legitimate validation query.
     *
     * Semantic errors (wrong Mod-97 check digit, wrong country length) are
     * NOT format errors and return 200 with valid=false instead.
     *
     * Response shape matches IbanController.IbanResponse so the frontend
     * can handle both 200 and 400 responses with the same TS interface.
     */
    @ExceptionHandler(IbanFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIbanFormatError(IbanFormatException ex) {
        return Map.of(
                "valid", false,
                "iban", ex.getNormalizedIban(),
                "reason", ex.getMessage());
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
