package de.nicograef.iban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the IbanNumber value object — normalization, structural validation, derived
 * properties.
 */
class IbanNumberTest {

    @Test
    void normalizesWhitespace() {
        var iban = new IbanNumber("DE89 3704 0044 0532 0130 00");
        assertEquals("DE89370400440532013000", iban.value());
    }

    @Test
    void normalizesHyphens() {
        var iban = new IbanNumber("DE89-3704-0044-0532-0130-00");
        assertEquals("DE89370400440532013000", iban.value());
    }

    @Test
    void normalizesLowercase() {
        var iban = new IbanNumber("de89370400440532013000");
        assertEquals("DE89370400440532013000", iban.value());
    }

    @Test
    void extractsCountryCode() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertEquals("DE", iban.countryCode());
    }

    @Test
    void extractsCheckDigits() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertEquals("89", iban.checkDigits());
    }

    @Test
    void extractsBban() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertEquals("370400440532013000", iban.bban());
    }

    @Test
    void extractsGermanBankIdentifier() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertTrue(iban.bankIdentifier().isPresent());
        assertEquals("37040044", iban.bankIdentifier().get());
    }

    @Test
    void nonGermanIbanHasNoBankIdentifier() {
        var iban = new IbanNumber("AT611904300234573201");
        assertTrue(iban.bankIdentifier().isEmpty());
    }

    @Test
    void formatsInFourCharGroups() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertEquals("DE89 3704 0044 0532 0130 00", iban.formatted());
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> new IbanNumber(""));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new IbanNumber(null));
    }

    @Test
    void rejectsTooShortInput() {
        assertThrows(IllegalArgumentException.class, () -> new IbanNumber("DE89"));
    }

    @Test
    void rejectsTooLongInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IbanNumber("DE89370400440532013000EXTRA1234567890"));
    }

    @Test
    void rejectsNonLetterCountryCode() {
        assertThrows(IllegalArgumentException.class, () -> new IbanNumber("12345678901234567890"));
    }

    @Test
    void toStringReturnsValue() {
        var iban = new IbanNumber("DE89370400440532013000");
        assertEquals("DE89370400440532013000", iban.toString());
    }

    @Test
    void equalityByValue() {
        var a = new IbanNumber("DE89 3704 0044 0532 0130 00");
        var b = new IbanNumber("de89370400440532013000");
        assertEquals(a, b);
    }
}
