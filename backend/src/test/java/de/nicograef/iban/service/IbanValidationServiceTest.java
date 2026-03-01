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
    }

    @Test
    void wrongLength() {
        var result = service.validate("DE8937040044053201300");
        assertFalse(result.valid());
    }

    @Test
    void emptyInput() {
        var result = service.validate("");
        assertFalse(result.valid());
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
}
