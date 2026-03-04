package de.nicograef.lexiban.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the Mod97Validator — isolated algorithm tests. */
class Mod97ValidatorTest {

    private final Mod97Validator validator = new Mod97Validator();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "DE89370400440532013000", // Germany (22 chars)
                "NO9386011117947", // Norway (shortest: 15 chars)
                "MT84MALT011000012345MTLCAST001S" // Malta (longest: 31 chars, letters in BBAN)
            })
    void validIbans(String iban) {
        assertTrue(validator.isValid(iban));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "DE00370400440532013000", // wrong check digits
                "DE98370400440532013000" // swapped check digits
            })
    void invalidIbans(String iban) {
        assertFalse(validator.isValid(iban));
    }
}
