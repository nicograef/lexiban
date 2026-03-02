package de.nicograef.iban.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the Mod97Validator — isolated algorithm tests. */
class Mod97ValidatorTest {

    private Mod97Validator validator;

    @BeforeEach
    void setUp() {
        validator = new Mod97Validator();
    }

    @Test
    void validGermanIban() {
        assertTrue(validator.isValid("DE89370400440532013000"));
    }

    @Test
    void invalidCheckDigits() {
        assertFalse(validator.isValid("DE00370400440532013000"));
    }

    @Test
    void validAustrianIban() {
        assertTrue(validator.isValid("AT611904300234573201"));
    }

    @Test
    void validBritishIban() {
        assertTrue(validator.isValid("GB29NWBK60161331926819"));
    }

    @Test
    void validNorwegianIban() {
        assertTrue(validator.isValid("NO9386011117947"));
    }

    @Test
    void validFrenchIban() {
        assertTrue(validator.isValid("FR7630006000011234567890189"));
    }

    @Test
    void validMalteseIban() {
        assertTrue(validator.isValid("MT84MALT011000012345MTLCAST001S"));
    }

    @Test
    void swappedDigitsDetected() {
        // DE89 → DE98 (swapped check digits) should fail
        assertFalse(validator.isValid("DE98370400440532013000"));
    }
}
