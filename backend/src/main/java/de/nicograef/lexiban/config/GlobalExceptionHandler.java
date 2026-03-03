package de.nicograef.lexiban.config;

import de.nicograef.lexiban.model.IbanFormatException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global error handler for all REST controllers — consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @Valid failures (e.g. @NotBlank) → 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(MethodArgumentNotValidException ex) {
        return Map.of(
                "error",
                "Validation failed",
                "details",
                ex.getBindingResult().getAllErrors().stream()
                        .map(e -> e.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Unknown validation error"));
    }

    /**
     * Structurally malformed IBAN → 400. Response shape matches IbanResponse for frontend
     * compatibility.
     */
    @ExceptionHandler(IbanFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIbanFormatError(IbanFormatException ex) {
        return Map.of(
                "valid", false,
                "iban", ex.getNormalizedIban(),
                "reason", ex.getMessage());
    }

    /** Catch-all → 500. Never exposes internal details. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericError(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Map.of("error", "Internal server error");
    }
}
