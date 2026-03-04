package de.nicograef.lexiban.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the IbanNumber value object — normalization, structural validation, derived
 * properties.
 */
class IbanNumberTest {

    @Test
    void normalizesWhitespaceHyphensAndCase() {
        assertEquals(
                "DE89370400440532013000", new IbanNumber("DE89 3704 0044 0532 0130 00").value());
        assertEquals(
                "DE89370400440532013000", new IbanNumber("DE89-3704-0044-0532-0130-00").value());
        assertEquals("DE89370400440532013000", new IbanNumber("de89370400440532013000").value());
    }

    @Test
    void derivedProperties() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertAll(
                () -> assertEquals("DE", iban.countryCode()),
                () -> assertEquals("89", iban.checkDigits()),
                () -> assertEquals("370400440532013000", iban.bban()),
                () -> assertEquals("37040044", iban.bankIdentifier().orElseThrow()),
                () -> assertEquals("DE89 3704 0044 0532 0130 00", iban.formatted()),
                () -> assertEquals("DE89370400440532013000", iban.toString()));
    }

    @Test
    void nonGermanIbanHasNoBankIdentifier() {
        assertTrue(new IbanNumber("AT611904300234573201").bankIdentifier().isEmpty());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
            strings = {"", "DE89", "DE89370400440532013000EXTRA1234567890", "12345678901234567890"})
    void rejectsInvalidInput(String input) {
        assertThrows(IbanFormatException.class, () -> new IbanNumber(input));
    }

    @Test
    void equalityByValueAfterNormalization() {
        assertEquals(
                new IbanNumber("DE89 3704 0044 0532 0130 00"),
                new IbanNumber("de89370400440532013000"));
    }
}
