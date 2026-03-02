package de.nicograef.iban.model;

/**
 * Thrown when raw input is structurally not an IBAN (empty, too short/long, wrong character
 * positions). Maps to HTTP 400. Semantic errors (wrong check digit) produce 200 with valid=false.
 */
public class IbanFormatException extends IllegalArgumentException {

    private final String normalizedIban;

    public IbanFormatException(String message, String normalizedIban) {
        super(message);
        this.normalizedIban = normalizedIban;
    }

    public String getNormalizedIban() {
        return normalizedIban;
    }
}
