package de.nicograef.iban.service;

import java.math.BigInteger;
import org.springframework.stereotype.Service;

/**
 * Modulo-97 check digit validator (ISO 7064).
 *
 * <p>Extracted from IbanValidationService into its own class because: 1. It's a clearly defined
 * algorithm — a standalone domain concept. 2. Isolating it makes testing trivial (no Spring
 * context, no other dependencies). 3. The Single Responsibility Principle: one class, one job.
 *
 * <p>TS equivalent: a pure function `isValidMod97(iban: string): boolean` Go equivalent: `func
 * IsValidMod97(iban string) bool` in a `validation` package.
 *
 * <p>See docs/iban.md for the full algorithm explanation.
 */
@Service
public class Mod97Validator {

    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    /**
     * Validate IBAN check digits using Modulo-97 (ISO 7064).
     *
     * <p>Algorithm: 1. Move the first 4 characters (country code + check digits) to the end. 2.
     * Convert all letters to numbers: A=10, B=11, ..., Z=35. 3. The resulting number must satisfy:
     * number mod 97 == 1.
     *
     * <p>Uses BigInteger because the numeric string can be 30-60+ digits long — far beyond what
     * long (max 19 digits) can hold.
     *
     * @param iban Normalized IBAN string (uppercase, no separators)
     * @return true if the check digits are valid
     */
    public boolean isValid(String iban) {
        // Step 1: Rearrange — move first 4 chars to end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Step 2: Convert letters to numbers (A=10, B=11, ..., Z=35)
        // Character.getNumericValue() returns 10–35 for A–Z, which is exactly
        // what the ISO 7064 spec requires.
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.getNumericValue(c));
            } else {
                numeric.append(c);
            }
        }

        // Step 3: Compute mod 97 — must equal 1
        BigInteger value = new BigInteger(numeric.toString());
        return value.mod(NINETY_SEVEN).intValue() == 1;
    }
}
