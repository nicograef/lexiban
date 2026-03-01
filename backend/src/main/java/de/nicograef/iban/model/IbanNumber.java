package de.nicograef.iban.model;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value Object for a normalized IBAN string.
 *
 * Responsibilities:
 * 1. Normalize raw input (strip non-alphanumeric chars, uppercase).
 * 2. Validate structural format (2 letters + 2 digits + 11–30 alphanumeric).
 * 3. Provide human-readable error messages for invalid input.
 * 4. Expose derived properties (countryCode, checkDigits, bban,
 * etc.).
 *
 * A valid IbanNumber instance is guaranteed to be normalized and structurally
 * valid. The constructor throws IbanFormatException (a subclass of
 * IllegalArgumentException) with a descriptive German-language message if
 * the input is invalid — callers can use e.getMessage() directly as an
 * error reason. The exception also carries the normalized IBAN string so
 * error handlers can include it in the HTTP response.
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
     * Normalizes a raw IBAN string: removes all non-alphanumeric characters
     * and converts to uppercase. Returns an empty string for null input.
     *
     * Public so callers (e.g. error handlers) can normalize without creating
     * an IbanNumber instance. Used internally by the compact constructor.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /**
     * Compact constructor: normalizes and validates on creation.
     * If you hold an IbanNumber, you know it's normalized and structurally valid.
     */
    public IbanNumber {
        value = normalize(value);

        if (value.isEmpty()) {
            throw new IbanFormatException("IBAN ist leer", value);
        }

        if (!IBAN_STRUCTURE.matcher(value).matches()) {
            throw new IbanFormatException(describeStructuralError(value), value);
        }
    }

    /**
     * Provides a specific German-language reason why the structural check failed.
     * Inspects the normalized IBAN step by step to return the most helpful error.
     *
     * TS analogy: a private helper that builds a user-facing error string.
     */
    private static String describeStructuralError(String iban) {
        if (iban.isEmpty()) {
            return "IBAN ist leer";
        }

        if (iban.length() < 15) {
            return "IBAN zu kurz: " + iban.length() + " Zeichen (Minimum: 15)";
        }

        if (iban.length() > 34) {
            return "IBAN zu lang: " + iban.length() + " Zeichen (Maximum: 34)";
        }

        if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))) {
            return "IBAN muss mit 2 Buchstaben (Ländercode) beginnen";
        }

        if (!Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3))) {
            return "Stelle 3-4 müssen Ziffern sein (Prüfziffern)";
        }

        return "Ungültiges IBAN-Format";
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
