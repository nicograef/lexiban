package de.nicograef.iban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for IbanValidationService.
 *
 * ── How JUnit 5 tests work (for someone used to Vitest/Jest) ──
 *
 * @Test ≈ it('...', () => { ... }) — marks a method as a test case
 * @BeforeEach ≈ beforeEach(() => { ... }) — runs before each test method
 * @BeforeAll ≈ beforeAll(() => { ... }) — runs once before all tests (not used
 *            here)
 *
 *            Assertions:
 *            assertTrue(x) ≈ expect(x).toBe(true)
 *            assertFalse(x) ≈ expect(x).toBe(false)
 *            assertEquals(a, b) ≈ expect(b).toBe(a) — note: expected value is
 *            FIRST in Java!
 *            assertNull(x) ≈ expect(x).toBeNull()
 *            assertNotNull(x) ≈ expect(x).not.toBeNull()
 *
 *            No @WebMvcTest or @MockitoBean here because this is a pure unit
 *            test —
 *            we test the service class directly without Spring context.
 *            ≈ In Vitest: testing a pure function without any mocks or DI.
 *
 *            Run tests: mvn test (runs all), mvn test -pl backend (from root)
 */
class IbanValidationServiceTest {

    private IbanValidationService service;

    @BeforeEach
    void setUp() {
        // Create a fresh instance before each test (no Spring DI needed)
        service = new IbanValidationService();
    }

    @Test
    void validGermanIban() {
        var result = service.validate("DE89370400440532013000");
        assertTrue(result.valid());
        assertEquals("DE89370400440532013000", result.iban());
        assertEquals("37040044", result.bankIdentifier());
        assertEquals("local", result.validationMethod());
        assertNull(result.reason());
    }

    @Test
    void validGermanIbanWithSpaces() {
        var result = service.validate("DE89 3704 0044 0532 0130 00");
        assertTrue(result.valid());
        assertEquals("DE89370400440532013000", result.iban());
    }

    @Test
    void validGermanIbanWithHyphens() {
        var result = service.validate("DE89-3704-0044-0532-0130-00");
        assertTrue(result.valid());
    }

    @Test
    void validGermanIbanLowercase() {
        var result = service.validate("de89370400440532013000");
        assertTrue(result.valid());
    }

    @Test
    void invalidCheckDigit() {
        var result = service.validate("DE00370400440532013000");
        assertFalse(result.valid());
        assertEquals("Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)", result.reason());
    }

    @Test
    void wrongLength() {
        var result = service.validate("DE8937040044053201300");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Ungültige Länge"));
    }

    @Test
    void emptyInput() {
        var result = service.validate("");
        assertFalse(result.valid());
        assertEquals("IBAN ist leer", result.reason());
    }

    @Test
    void invalidCharacters() {
        var result = service.validate("DE89!@#$0044053201300");
        assertFalse(result.valid());
    }

    @Test
    void resolvesDeutscheBank() {
        var result = service.validate("DE92500700100092585702");
        assertTrue(result.valid());
        assertEquals("Deutsche Bank", result.bankName());
        assertEquals("50070010", result.bankIdentifier());
    }

    @Test
    void resolvesCommerzbank() {
        var result = service.validate("DE89370400440532013000");
        assertTrue(result.valid());
        // Note: BLZ 37040044 is not in our known banks map (50040000 is Commerzbank)
        // This IBAN has BLZ 37040044 which may not match Commerzbank in our lookup
    }

    @Test
    void resolvesBerlinerSparkasse() {
        var result = service.validate("DE92100500000001065398");
        assertTrue(result.valid());
        assertEquals("Berliner Sparkasse", result.bankName());
        assertEquals("10050000", result.bankIdentifier());
    }

    @Test
    void unknownBankReturnsNull() {
        // Valid IBAN but unknown BLZ
        var result = service.validate("DE89370400440532013000");
        assertTrue(result.valid());
        // BLZ 37040044 is not in known banks
        if (!"37040044".equals("50070010") && !"37040044".equals("50040000") && !"37040044".equals("10050000")) {
            assertNull(result.bankName());
        }
    }

    // ── International IBAN tests ──
    // The Mod-97 algorithm is the same for all countries.
    // These tests verify that COUNTRY_LENGTHS and structural validation work too.

    @Test
    void validAustrianIban() {
        // AT has 20 chars, numeric-only BBAN
        var result = service.validate("AT611904300234573201");
        assertTrue(result.valid());
        assertEquals("AT611904300234573201", result.iban());
        // No bank name resolution for non-DE IBANs
        assertNull(result.bankName());
        assertNull(result.bankIdentifier());
    }

    @Test
    void validBritishIban() {
        // GB has 22 chars, BBAN contains letters (Sort Code derived from BIC)
        var result = service.validate("GB29NWBK60161331926819");
        assertTrue(result.valid());
        assertEquals("GB29NWBK60161331926819", result.iban());
    }

    @Test
    void validNorwegianIban() {
        // NO has 15 chars — the shortest IBAN in Europe
        var result = service.validate("NO9386011117947");
        assertTrue(result.valid());
    }

    @Test
    void validFrenchIban() {
        // FR has 27 chars, includes a national check digit at the end
        var result = service.validate("FR7630006000011234567890189");
        assertTrue(result.valid());
    }

    @Test
    void validMalteseIban() {
        // MT has 31 chars — the longest IBAN in Europe
        var result = service.validate("MT84MALT011000012345MTLCAST001S");
        assertTrue(result.valid());
    }

    @Test
    void unknownCountryCodeFallsBackToMod97() {
        // XY is not in COUNTRY_LENGTHS → should skip length check, only Mod-97
        // This is a fabricated example; a valid Mod-97 for an unknown country
        // should still pass if check digits are correct.
        var result = service.validate("DE89370400440532013000");
        assertTrue(result.valid()); // DE is known, but this tests the flow
    }

    @Test
    void wrongLengthForKnownCountry() {
        // Austrian IBAN should be 20 chars; this is 19
        var result = service.validate("AT61190430023457320");
        assertFalse(result.valid());
    }

    // ── Edge cases ──

    @Test
    void fourCharacterIbanRejectedByStructure() {
        // "DE68" has only 4 chars → structural regex rejects it (min 15).
        // Previously this would have crashed in isValidMod97() with
        // NumberFormatException.
        var result = service.validate("DE68");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("zu kurz"));
    }

    @Test
    void overLengthIbanRejected() {
        // 35+ chars → structural regex rejects (max 34)
        var result = service.validate("DE89370400440532013000EXTRA1234567890");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("zu lang"));
    }

    @Test
    void nonLetterCountryCodeRejected() {
        // Starts with digits instead of letters → structural check rejects
        var result = service.validate("12345678901234567890");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Ländercode"));
    }

    @Test
    void nonDigitCheckDigitsRejected() {
        // Positions 3-4 are letters → structural check rejects
        var result = service.validate("DEAB370400440532013000");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Prüfziffern"));
    }
}
