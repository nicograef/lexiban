package de.nicograef.iban.service;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * IBAN validation using Modulo-97 check digit algorithm (ISO 13616).
 * Also resolves German bank names by BLZ (Bankleitzahl).
 * See docs/iban.md for the algorithm details.
 */
@Service
public class IbanValidationService {

    /**
     * Demo subset of German BLZ → bank name mappings.
     * In production, this would be loaded from the Bundesbank BLZ file (~3,600
     * entries).
     * Only relevant for German IBANs — other countries use different bank
     * identifier schemes (Sort Code in GB, BC-Nummer in CH, etc.).
     */
    private static final Map<String, String> KNOWN_BANKS = Map.of(
            "50070010", "Deutsche Bank",
            "50040000", "Commerzbank",
            "10050000", "Berliner Sparkasse");

    /**
     * Expected IBAN length per country code (ISO 13616).
     * The shortest IBAN is Norway (15), the longest Malta (31), max possible is 34.
     * Countries not listed here skip the length check — only Mod-97 is applied.
     * Source: iban.md §5 + Wikipedia list of IBAN formats.
     */
    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.ofEntries(
            Map.entry("AT", 20), // Austria
            Map.entry("BE", 16), // Belgium
            Map.entry("CH", 21), // Switzerland
            Map.entry("DE", 22), // Germany
            Map.entry("DK", 18), // Denmark
            Map.entry("ES", 24), // Spain
            Map.entry("FI", 18), // Finland
            Map.entry("FR", 27), // France
            Map.entry("GB", 22), // United Kingdom
            Map.entry("IE", 22), // Ireland
            Map.entry("IT", 27), // Italy
            Map.entry("LU", 20), // Luxembourg
            Map.entry("MT", 31), // Malta (longest in Europe)
            Map.entry("NL", 18), // Netherlands
            Map.entry("NO", 15), // Norway (shortest in Europe)
            Map.entry("PL", 28), // Poland
            Map.entry("PT", 25), // Portugal
            Map.entry("SE", 24) // Sweden
    );

    /**
     * Structural regex for ISO 13616 IBAN format:
     * - Pos 1–2: Two uppercase letters (country code, ISO 3166-1)
     * - Pos 3–4: Two digits (check digits)
     * - Pos 5–34: 11–30 alphanumeric characters (BBAN)
     * Total: 15–34 characters (shortest: Norway=15, longest: Malta=31, max spec:
     * 34).
     *
     * TS equivalent: /^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/
     */
    private static final Pattern IBAN_STRUCTURE = Pattern.compile(
            "^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");

    /**
     * Result of an IBAN validation. The reason field provides a human-readable
     * explanation for why the IBAN is invalid (null when valid).
     * TS equivalent: { valid: boolean; iban: string; reason?: string; ... }
     */
    public record ValidationResult(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String validationMethod,
            String reason) {
    }

    /**
     * Validate an IBAN: normalize → structural check → length check → Mod-97 → BLZ
     * lookup.
     * See docs/iban.md for algorithm details.
     *
     * bankName and bankIdentifier in the result are null for non-DE IBANs or
     * unknown
     * German banks. The external API (called by the controller) may resolve these
     * as fallback.
     */
    public ValidationResult validate(String rawIban) {
        String iban = normalize(rawIban);

        // Step 1: Structural check — reject before expensive Mod-97 calculation.
        // Catches: too short/long, missing country code, non-digit check digits, etc.
        if (!isStructurallyValid(iban)) {
            return new ValidationResult(false, iban, null, null, "local",
                    describeStructuralError(iban));
        }

        // Step 2: Country-specific length check (only for known countries)
        String country = iban.substring(0, 2);
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && iban.length() != expectedLength) {
            return new ValidationResult(false, iban, null, null, "local",
                    "Ungültige Länge: " + iban.length() + " statt " + expectedLength
                            + " Zeichen für " + country);
        }

        // Step 3: Mod-97 check digit validation
        if (!isValidMod97(iban)) {
            return new ValidationResult(false, iban, null, null, "local",
                    "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)");
        }

        // Step 4: Extract BLZ for German IBANs.
        // Positions 5–12 in human-readable notation (1-based), = substring(4, 12) in
        // Java.
        // See iban.md §4 for the German IBAN structure.
        String bankIdentifier = null;
        String bankName = null;
        if ("DE".equals(country) && iban.length() >= 12) {
            bankIdentifier = iban.substring(4, 12);
            bankName = KNOWN_BANKS.get(bankIdentifier);
        }

        return new ValidationResult(true, iban, bankName, bankIdentifier, "local", null);
    }

    /**
     * Resolve bank name from BLZ. Returns null if unknown.
     */
    public String resolveBankName(String blz) {
        return KNOWN_BANKS.get(blz);
    }

    /** Remove all non-alphanumeric characters and convert to uppercase. */
    private String normalize(String input) {
        return input.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /**
     * Structural pre-validation against ISO 13616 format.
     * Checks the "shape" of an IBAN before running the expensive Mod-97 math.
     * This is like a zod .regex() schema check before hitting the business logic.
     */
    private boolean isStructurallyValid(String iban) {
        return IBAN_STRUCTURE.matcher(iban).matches();
    }

    /**
     * Provides a specific German-language reason why the structural check failed.
     * Inspects the IBAN step by step to return the most helpful error message.
     */
    private String describeStructuralError(String iban) {
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
            return "Stelle 3–4 müssen Ziffern sein (Prüfziffern)";
        }
        return "Ungültiges IBAN-Format";
    }

    /**
     * Modulo-97 check digit validation (ISO 7064).
     * Uses BigInteger for the resulting 30–60+ digit number.
     *
     * Character.getNumericValue() returns 10–35 for A–Z, which is exactly
     * what the ISO 7064 spec requires. TS equivalent: charCodeAt(0) - 55.
     *
     * TS equivalent using BigInt:
     * const num = BigInt(numericString);
     * return num % 97n === 1n;
     */
    private boolean isValidMod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Convert letters to numbers (A=10, B=11, ..., Z=35)
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.getNumericValue(c));
            } else {
                numeric.append(c);
            }
        }

        BigInteger value = new BigInteger(numeric.toString());
        return value.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
