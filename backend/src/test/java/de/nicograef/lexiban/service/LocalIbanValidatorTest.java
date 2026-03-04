package de.nicograef.lexiban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.nicograef.lexiban.model.IbanNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for LocalIbanValidator — three possible outcomes: definitive invalid, valid +
 * bankName, or empty (needs external). Uses a real Mod97Validator (pure algorithm, no mocks).
 */
class LocalIbanValidatorTest {

    private LocalIbanValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LocalIbanValidator(new Mod97Validator());
    }

    @Test
    void wrongLengthForKnownCountryRejects() {
        // Austrian IBAN should be 20 chars; this is 19
        var result = validator.validate(new IbanNumber("AT61190430023457320"));
        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertTrue(result.get().reason().contains("Ungültige Länge"));
    }

    @Test
    void invalidMod97CheckDigitsRejects() {
        var result = validator.validate(new IbanNumber("DE00370400440532013000"));
        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertEquals(
                "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)", result.get().reason());
    }

    @Test
    void knownGermanBankResolvesWithBankName() {
        var result = validator.validate(new IbanNumber("DE92500700100092585702"));
        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("Deutsche Bank", result.get().bankName());
        assertNull(result.get().reason());
    }

    @Test
    void unknownGermanBankReturnsEmpty() {
        // Valid German IBAN, but BLZ 37040044 not in KNOWN_BANKS
        assertTrue(validator.validate(new IbanNumber("DE89370400440532013000")).isEmpty());
    }

    @Test
    void nonGermanValidIbanReturnsEmpty() {
        // Non-German → no BLZ lookup → empty (needs external)
        assertTrue(validator.validate(new IbanNumber("AT611904300234573201")).isEmpty());
    }
}
