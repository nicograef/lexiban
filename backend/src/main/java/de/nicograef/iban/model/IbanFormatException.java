package de.nicograef.iban.model;

/**
 * Thrown when raw input is structurally not an IBAN (empty, too short/long,
 * wrong character positions). This maps to HTTP 400 — the request itself
 * is malformed, not a valid validation request.
 *
 * Semantic errors (wrong check digit, wrong country length) are NOT format
 * errors — those produce a normal 200 response with valid=false.
 *
 * TS analogy: throwing a custom ValidationError that your Express error
 * middleware catches and maps to res.status(400).json({ ... }).
 *
 * Go analogy: returning a typed error that the handler checks with
 * errors.As() to decide the HTTP status code.
 */
public class IbanFormatException extends IllegalArgumentException {

    /** The normalized (or best-effort normalized) IBAN string. */
    private final String normalizedIban;

    public IbanFormatException(String message, String normalizedIban) {
        super(message);
        this.normalizedIban = normalizedIban;
    }

    public String getNormalizedIban() {
        return normalizedIban;
    }
}
