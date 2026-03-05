package de.nicograef.lexiban.model;

/**
 * Thrown when raw input is structurally not an IBAN (empty, too short/long, wrong character
 * positions).
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
