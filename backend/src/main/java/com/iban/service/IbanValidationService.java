package com.iban.service;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;

/**
 * IBAN validation using Modulo-97 check digit algorithm (ISO 13616).
 * Also resolves German bank names by BLZ (Bankleitzahl).
 *
 * ── Analogy ──
 * This is a plain service class — think of it as a module that exports
 * pure validation functions. In TS: export function validate(iban: string):
 * Result
 * In Go: func (s *IbanValidationService) Validate(iban string) ValidationResult
 *
 * @Service tells Spring: "create a single instance (singleton) of this class
 *          and
 *          make it available for dependency injection." It's like registering a
 *          provider
 *          in Angular's DI or exporting a singleton from a module.
 *          When the controller's constructor asks for an IbanValidationService,
 *          Spring
 *          automatically provides this instance — no manual wiring needed.
 */
@Service
public class IbanValidationService {

    // ── Known German banks mapped by BLZ ──
    // Map.of() creates an immutable Map (≈ Object.freeze({...}) in JS / a const map
    // in Go).
    // "static final" means this is a class-level constant shared by all instances.
    private static final Map<String, String> KNOWN_BANKS = Map.of(
            "50070010", "Deutsche Bank",
            "50040000", "Commerzbank",
            "10050000", "Berliner Sparkasse");

    // Expected IBAN lengths per country code (extendable for future countries)
    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.of(
            "DE", 22);

    // ── Result DTO as a Java Record ──
    // Java Records are immutable data holders — like TS `type` or Go `struct`.
    // The compiler auto-generates: constructor, getters (e.g. valid(), iban()),
    // equals(), hashCode(), toString(). No boilerplate needed.
    //
    // TS equivalent: type ValidationResult = { valid: boolean; iban: string; ... }
    // Go equivalent: type ValidationResult struct { Valid bool; Iban string; ... }
    public record ValidationResult(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String validationMethod) {
    }

    /**
     * Validate an IBAN string using the Modulo-97 algorithm (ISO 13616).
     *
     * Algorithm steps:
     * 1. Normalize: remove separators (spaces, hyphens, dots) and convert to
     * uppercase
     * 2. Check country-specific length (DE = 22 characters)
     * 3. Modulo-97 check:
     * a) Move first 4 chars (country code + check digits) to end
     * b) Convert each letter to a number: A=10, B=11, ..., Z=35
     * c) The resulting number mod 97 must equal 1
     * 4. Extract BLZ (positions 5–12 for DE-IBANs) and look up bank name
     */
    public ValidationResult validate(String rawIban) {
        String iban = normalize(rawIban);

        // Basic format check — only letters and digits allowed
        if (iban.length() < 2 || !iban.matches("[A-Z0-9]+")) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        // Country-specific length check (DE must be exactly 22 chars)
        String country = iban.substring(0, 2);
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && iban.length() != expectedLength) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        // Modulo-97 check — the core validation algorithm
        if (!isValidMod97(iban)) {
            return new ValidationResult(false, iban, null, null, "local");
        }

        // Extract BLZ for German IBANs (positions 5–12, 0-indexed: 4–11)
        // DE IBAN structure: DE[check digits][BLZ 8 digits][account number 10 digits]
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

    /**
     * Remove spaces, hyphens, dots and convert to uppercase.
     * ≈ iban.replace(/[\s\-.]/g, '').toUpperCase() in TypeScript.
     */
    private String normalize(String input) {
        return input.replaceAll("[\\s\\-.]", "").toUpperCase();
    }

    /**
     * Core Modulo-97 check digit validation (ISO 7064).
     *
     * ── How it works ──
     * Example IBAN: DE89370400440532013000
     *
     * 1) Move first 4 chars to end: 370400440532013000DE89
     * 2) Convert letters → numbers: 370400440532013000131489
     * (D=13, E=14)
     * 3) Compute: 370400440532013000131489 mod 97 == 1 ✓
     *
     * ── BigInteger ──
     * java.math.BigInteger is Java's equivalent of JavaScript's BigInt.
     * We need it because the numeric string can be 30+ digits — far beyond
     * what a long (64-bit) can hold.
     * ≈ BigInt("370400440532013000131489") % 97n === 1n in JavaScript.
     */
    private boolean isValidMod97(String iban) {
        // Step 1: Move first 4 characters (country + check digits) to the end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Step 2: Convert letters to numbers (A=10, B=11, ..., Z=35)
        // Character.getNumericValue('A') returns 10, 'B' returns 11, etc.
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.getNumericValue(c));
            } else {
                numeric.append(c);
            }
        }

        // Step 3: Check mod 97 == 1
        BigInteger value = new BigInteger(numeric.toString());
        return value.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
