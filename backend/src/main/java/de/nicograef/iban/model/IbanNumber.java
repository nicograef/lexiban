package de.nicograef.iban.model;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value Object for a normalized IBAN string.
 *
 * Self-normalizing: removes all non-alphanumeric characters and converts to
 * uppercase.
 * A valid IbanNumber instance is guaranteed to be normalized and structurally
 * valid
 * (correct format: 2 letters + 2 digits + 11-30 alphanumeric characters).
 *
 * This is a DDD Value Object — defined by its value, immutable, with behavior.
 * TS equivalent: a class that validates in the constructor and exposes readonly
 * properties.
 * Go equivalent: a validated string type with methods.
 *
 * Why this matters: When the IBAN is the primary key in the database, every
 * INSERT and SELECT must use the exact same normalized form. Without
 * IbanNumber,
 * normalization would have to happen at every call site — a bug waiting to
 * happen.
 */
public record IbanNumber(String value) {

    /**
     * Structural regex for ISO 13616 IBAN format:
     * - Pos 1–2: Two uppercase letters (country code)
     * - Pos 3–4: Two digits (check digits)
     * - Pos 5–34: 11–30 alphanumeric characters (BBAN)
     */
    private static final Pattern IBAN_STRUCTURE = Pattern.compile(
            "^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");

    /**
     * Compact constructor: normalizes and validates on creation.
     * If you hold an IbanNumber, you know it's normalized and structurally valid.
     */
    public IbanNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IBAN darf nicht leer sein");
        }
        // Normalize: remove all non-alphanumeric characters, uppercase
        value = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        if (!IBAN_STRUCTURE.matcher(value).matches()) {
            throw new IllegalArgumentException("Ungültiges IBAN-Format: " + value);
        }
    }

    /**
     * Factory method that creates an IbanNumber without structural validation.
     * Used when loading already-normalized IBANs from the database.
     * The DB guarantees the value is already normalized (it was validated on first
     * save).
     */
    public static IbanNumber ofNormalized(String normalized) {
        // Skip re-validation for trusted sources (database)
        return new IbanNumber(normalized);
    }

    /** Country code as a string (first 2 characters), e.g. "DE", "AT", "GB". */
    public String countryCode() {
        return value.substring(0, 2);
    }

    /** Check digits (positions 3-4), e.g. "89" for DE89... */
    public String checkDigits() {
        return value.substring(2, 4);
    }

    /**
     * BBAN (Basic Bank Account Number) — everything after the first 4 characters.
     * Country-specific structure. For DE: 8-digit BLZ + 10-digit account number.
     */
    public String bban() {
        return value.substring(4);
    }

    /**
     * Bank identifier (BLZ for German IBANs, positions 5-12).
     * Returns empty Optional for non-German IBANs or IBANs too short.
     * Other countries have different bank identifier schemes (Sort Code in GB,
     * etc.)
     */
    public Optional<String> bankIdentifier() {
        if ("DE".equals(countryCode()) && value.length() >= 12) {
            return Optional.of(value.substring(4, 12));
        }
        return Optional.empty();
    }

    /**
     * Formatted display string with 4-character groups (DIN 5008).
     * "DE89370400440532013000" → "DE89 3704 0044 0532 0130 00"
     */
    public String formatted() {
        return value.replaceAll("(.{4})", "$1 ").trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
