package de.nicograef.iban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.nicograef.iban.model.IbanNumber;
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

    // ── Definitive invalid results ──

    @Test
    void wrongLengthForKnownCountry() {
        // Austrian IBAN should be 20 chars; this is 19
        var result = validator.validate(new IbanNumber("AT61190430023457320"));
        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertTrue(result.get().reason().contains("Ungültige Länge"));
    }

    @Test
    void invalidMod97CheckDigits() {
        var result = validator.validate(new IbanNumber("DE00370400440532013000"));
        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertEquals(
                "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)", result.get().reason());
    }

    @Test
    void wrongLengthForGermany() {
        // DE should be 22 chars; this is 21
        var result = validator.validate(new IbanNumber("DE8937040044053201300"));
        assertTrue(result.isPresent());
        assertFalse(result.get().valid());
        assertTrue(result.get().reason().contains("Ungültige Länge"));
        assertTrue(result.get().reason().contains("22"));
    }

    // ── Valid + known bank ──

    @Test
    void resolvesDeutscheBank() {
        // BLZ 50070010 = Deutsche Bank
        var result = validator.validate(new IbanNumber("DE92500700100092585702"));
        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("Deutsche Bank", result.get().bankName());
        assertNull(result.get().reason());
    }

    @Test
    void resolvesBerlinerSparkasse() {
        // BLZ 10050000 = Berliner Sparkasse
        var result = validator.validate(new IbanNumber("DE92100500000001065398"));
        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("Berliner Sparkasse", result.get().bankName());
    }

    // ── Empty (bank unknown → needs external) ──

    @Test
    void unknownGermanBankReturnsEmpty() {
        // Valid German IBAN, but BLZ 37040044 is not in KNOWN_BANKS
        var result = validator.validate(new IbanNumber("DE89370400440532013000"));
        assertTrue(result.isEmpty());
    }

    @Test
    void validAustrianIbanReturnsEmpty() {
        // Non-German IBAN → no BLZ lookup possible → empty
        var result = validator.validate(new IbanNumber("AT611904300234573201"));
        assertTrue(result.isEmpty());
    }

    @Test
    void validBritishIbanReturnsEmpty() {
        var result = validator.validate(new IbanNumber("GB29NWBK60161331926819"));
        assertTrue(result.isEmpty());
    }

    @Test
    void validNorwegianIbanReturnsEmpty() {
        var result = validator.validate(new IbanNumber("NO9386011117947"));
        assertTrue(result.isEmpty());
    }

    @Test
    void validFrenchIbanReturnsEmpty() {
        var result = validator.validate(new IbanNumber("FR7630006000011234567890189"));
        assertTrue(result.isEmpty());
    }

    @Test
    void validMalteseIbanReturnsEmpty() {
        var result = validator.validate(new IbanNumber("MT84MALT011000012345MTLCAST001S"));
        assertTrue(result.isEmpty());
    }

    // ── Edge: IBAN normalizes on construction ──

    @Test
    void handlesSpacesAndLowercase() {
        // Spaces + lowercase → IbanNumber normalizes → BLZ 50070010 = Deutsche Bank
        var result = validator.validate(new IbanNumber("de92 5007 0010 0092 5857 02"));
        assertTrue(result.isPresent());
        assertTrue(result.get().valid());
        assertEquals("Deutsche Bank", result.get().bankName());
    }
}
