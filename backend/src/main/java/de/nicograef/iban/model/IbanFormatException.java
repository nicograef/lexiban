package de.nicograef.iban.model;

/**
 * Thrown when raw input is structurally not an IBAN (empty, too short/long, wrong character
 * positions). This maps to HTTP 400 — the request itself is malformed, not a valid validation
 * request.
 *
 * <p>Semantic errors (wrong check digit, wrong country length) are NOT format errors — those
 * produce a normal 200 response with valid=false.
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
