package de.nicograef.iban.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.lang.NonNull;

/**
 * DDD Value Object for a normalized, structurally valid IBAN string.
 *
 * <p>Normalizes raw input (strip non-alphanumeric, uppercase), validates structural format, and
 * exposes derived properties (countryCode, checkDigits, bban, bankIdentifier).
 *
 * <p>Throws {@link IbanFormatException} with a German-language message if input is structurally
 * invalid. A valid instance is guaranteed to be normalized — essential since the IBAN is the
 * database primary key.
 */
public record IbanNumber(@NonNull String value) {

    /** ISO 13616 structural pattern: 2 letters + 2 digits + 11–30 alphanumeric (BBAN). */
    private static final Pattern IBAN_STRUCTURE =
            Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");

    /** Strip non-alphanumeric characters and uppercase. Public for use in error handlers. */
    public static @NonNull String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return Objects.requireNonNull(raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase());
    }

    /** Compact constructor — normalizes and validates on creation. */
    public IbanNumber {
        value = normalize(value);

        if (value.isEmpty()) {
            throw new IbanFormatException("IBAN ist leer", value);
        }

        if (!IBAN_STRUCTURE.matcher(value).matches()) {
            throw new IbanFormatException(describeStructuralError(value), value);
        }
    }

    /** Returns a specific German-language reason for a structural format failure. */
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

    /** BBAN (Basic Bank Account Number) — everything after the first 4 characters. */
    public String bban() {
        return value.substring(4);
    }

    /** BLZ for German IBANs (positions 5–12). Empty for non-German IBANs. */
    public Optional<String> bankIdentifier() {
        if ("DE".equals(countryCode()) && value.length() >= 12) {
            return Optional.of(value.substring(4, 12));
        }
        return Optional.empty();
    }

    /** Formatted with 4-character groups (DIN 5008). */
    public String formatted() {
        return value.replaceAll("(.{4})", "$1 ").trim();
    }

    @Override
    public String toString() {
        return value;
    }
}
