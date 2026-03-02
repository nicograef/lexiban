package de.nicograef.iban.service;

import java.math.BigInteger;
import org.springframework.stereotype.Service;

/**
 * Modulo-97 check digit validator (ISO 7064). Extracted as its own class for isolated testability
 * and SRP. See docs/iban.md for the full algorithm explanation.
 */
@Service
public class Mod97Validator {

    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    /**
     * Validate IBAN check digits using Modulo-97 (ISO 7064). Uses BigInteger because the numeric
     * string can exceed long's 19-digit limit.
     *
     * @param iban Normalized IBAN string (uppercase, no separators)
     * @return true if the check digits are valid
     */
    public boolean isValid(String iban) {
        // Move first 4 chars (country code + check digits) to end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Convert letters to numbers: A=10, B=11, ..., Z=35
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(Character.getNumericValue(c));
            } else {
                numeric.append(c);
            }
        }

        // Valid IBAN ⟹ number mod 97 == 1
        BigInteger value = new BigInteger(numeric.toString());
        return value.mod(NINETY_SEVEN).intValue() == 1;
    }
}
