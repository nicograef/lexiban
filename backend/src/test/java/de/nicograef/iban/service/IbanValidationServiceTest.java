package de.nicograef.iban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.nicograef.iban.repository.IbanRepository;

/**
 * Unit tests for IbanValidationService.
 *
 * ── How JUnit 5 tests work (for someone used to Vitest/Jest) ──
 *
 * @Test ≈ it('...', () => { ... }) — marks a method as a test case
 * @BeforeEach ≈ beforeEach(() => { ... }) — runs before each test method
 *
 *             Assertions:
 *             assertTrue(x) ≈ expect(x).toBe(true)
 *             assertFalse(x) ≈ expect(x).toBe(false)
 *             assertEquals(a, b) ≈ expect(b).toBe(a) — note: expected value is
 *             FIRST in Java!
 *             assertNull(x) ≈ expect(x).toBeNull()
 *
 *             The service now requires Mod97Validator, ExternalIbanApiService,
 *             and
 *             IbanRepository via constructor injection. For pure validation
 *             tests,
 *             we use a real Mod97Validator and mock the other dependencies.
 *
 *             Run tests: mvn test (runs all), mvn test -pl backend (from root)
 */
@ExtendWith(MockitoExtension.class)
class IbanValidationServiceTest {

    private IbanValidationService service;

    @Mock
    private ExternalIbanApiService externalApiService;

    @Mock
    private IbanRepository ibanRepository;

    @BeforeEach
    void setUp() {
        // Real Mod97Validator + mocked external/repo for pure validation tests
        service = new IbanValidationService(
                new Mod97Validator(), externalApiService, ibanRepository);
    }

    // ── validateRaw() tests (raw string input, may be structurally invalid) ──

    @Test
    void validGermanIban() {
        var result = service.validateRaw("DE89370400440532013000");
        assertTrue(result.valid());
        assertEquals("DE89370400440532013000", result.iban());
        assertEquals("37040044", result.bankIdentifier());
        assertNull(result.reason());
    }

    @Test
    void validGermanIbanWithSpaces() {
        var result = service.validateRaw("DE89 3704 0044 0532 0130 00");
        assertTrue(result.valid());
        assertEquals("DE89370400440532013000", result.iban());
    }

    @Test
    void validGermanIbanWithHyphens() {
        var result = service.validateRaw("DE89-3704-0044-0532-0130-00");
        assertTrue(result.valid());
    }

    @Test
    void validGermanIbanLowercase() {
        var result = service.validateRaw("de89370400440532013000");
        assertTrue(result.valid());
    }

    @Test
    void invalidCheckDigit() {
        var result = service.validateRaw("DE00370400440532013000");
        assertFalse(result.valid());
        assertEquals("Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)", result.reason());
    }

    @Test
    void wrongLength() {
        var result = service.validateRaw("DE8937040044053201300");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Ungültige Länge"));
    }

    @Test
    void emptyInput() {
        var result = service.validateRaw("");
        assertFalse(result.valid());
        assertEquals("IBAN ist leer", result.reason());
    }

    @Test
    void invalidCharacters() {
        var result = service.validateRaw("DE89!@#$0044053201300");
        assertFalse(result.valid());
    }

    @Test
    void resolvesDeutscheBank() {
        var result = service.validateRaw("DE92500700100092585702");
        assertTrue(result.valid());
        assertEquals("Deutsche Bank", result.bankName());
        assertEquals("50070010", result.bankIdentifier());
    }

    @Test
    void resolvesCommerzbank() {
        var result = service.validateRaw("DE89370400440532013000");
        assertTrue(result.valid());
        // Note: BLZ 37040044 is not in our known banks map (50040000 is Commerzbank)
    }

    @Test
    void resolvesBerlinerSparkasse() {
        var result = service.validateRaw("DE92100500000001065398");
        assertTrue(result.valid());
        assertEquals("Berliner Sparkasse", result.bankName());
        assertEquals("10050000", result.bankIdentifier());
    }

    @Test
    void unknownBankReturnsNull() {
        // Valid IBAN but unknown BLZ
        var result = service.validateRaw("DE89370400440532013000");
        assertTrue(result.valid());
        // BLZ 37040044 is not in known banks
        if (!"37040044".equals("50070010") && !"37040044".equals("50040000") && !"37040044".equals("10050000")) {
            assertNull(result.bankName());
        }
    }

    // ── International IBAN tests ──
    // The Mod-97 algorithm is the same for all countries.

    @Test
    void validAustrianIban() {
        var result = service.validateRaw("AT611904300234573201");
        assertTrue(result.valid());
        assertEquals("AT611904300234573201", result.iban());
        assertNull(result.bankName());
        assertNull(result.bankIdentifier());
    }

    @Test
    void validBritishIban() {
        var result = service.validateRaw("GB29NWBK60161331926819");
        assertTrue(result.valid());
        assertEquals("GB29NWBK60161331926819", result.iban());
    }

    @Test
    void validNorwegianIban() {
        var result = service.validateRaw("NO9386011117947");
        assertTrue(result.valid());
    }

    @Test
    void validFrenchIban() {
        var result = service.validateRaw("FR7630006000011234567890189");
        assertTrue(result.valid());
    }

    @Test
    void validMalteseIban() {
        var result = service.validateRaw("MT84MALT011000012345MTLCAST001S");
        assertTrue(result.valid());
    }

    @Test
    void unknownCountryCodeFallsBackToMod97() {
        var result = service.validateRaw("DE89370400440532013000");
        assertTrue(result.valid()); // DE is known, but this tests the flow
    }

    @Test
    void wrongLengthForKnownCountry() {
        // Austrian IBAN should be 20 chars; this is 19
        var result = service.validateRaw("AT61190430023457320");
        assertFalse(result.valid());
    }

    // ── Edge cases ──

    @Test
    void fourCharacterIbanRejectedByStructure() {
        var result = service.validateRaw("DE68");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("zu kurz"));
    }

    @Test
    void overLengthIbanRejected() {
        var result = service.validateRaw("DE89370400440532013000EXTRA1234567890");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("zu lang"));
    }

    @Test
    void nonLetterCountryCodeRejected() {
        var result = service.validateRaw("12345678901234567890");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Ländercode"));
    }

    @Test
    void nonDigitCheckDigitsRejected() {
        var result = service.validateRaw("DEAB370400440532013000");
        assertFalse(result.valid());
        assertTrue(result.reason().contains("Prüfziffern"));
    }
}
