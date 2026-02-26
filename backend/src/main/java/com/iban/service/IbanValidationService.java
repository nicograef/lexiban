package com.iban.service;

import java.math.BigInteger;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * IBAN validation using Modulo-97 check digit algorithm (ISO 13616).
 * Also resolves German bank names by BLZ (Bankleitzahl).
 * See docs/iban.md for the algorithm details.
 */
@Service
public class IbanValidationService {

    private static final Map<String, String> KNOWN_BANKS = Map.of(
            "50070010", "Deutsche Bank",
            "50040000", "Commerzbank",
            "10050000", "Berliner Sparkasse");

    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.of(
            "DE", 22);

    public record ValidationResult(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String validationMethod) {
    }

    /**
     * Validate an IBAN: normalize → length check → Mod-97 → BLZ lookup.
     * See docs/iban.md for algorithm details.
     */
    public ValidationResult validate(String rawIban) {
        String iban = normalize(rawIban);

        if (iban.length() < 2 || !iban.matches("[A-Z0-9]+")) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        String country = iban.substring(0, 2);
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && iban.length() != expectedLength) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        if (!isValidMod97(iban)) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        // Extract BLZ for German IBANs (positions 4–11)
        String bankIdentifier = null;
        String bankName = null;
        if ("DE".equals(country) && iban.length() >= 12) {
            bankIdentifier = iban.substring(4, 12);
            bankName = KNOWN_BANKS.get(bankIdentifier);
        }

        return new ValidationResult(true, iban, bankName, bankIdentifier, "local");
    }

    /**
     * Resolve bank name from BLZ. Returns null if unknown.
     */
    public String resolveBankName(String blz) {
        return KNOWN_BANKS.get(blz);
    }

    /** Remove separators and convert to uppercase. */
    private String normalize(String input) {
        return input.replaceAll("[\\s\\-.]", "").toUpperCase();
    }

    /** Modulo-97 check digit validation (ISO 7064). Uses BigInteger for 30+ digit numbers. */
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
