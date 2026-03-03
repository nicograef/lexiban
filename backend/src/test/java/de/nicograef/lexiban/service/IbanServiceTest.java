package de.nicograef.lexiban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.nicograef.lexiban.model.Iban;
import de.nicograef.lexiban.model.IbanFormatException;
import de.nicograef.lexiban.model.ValidationResult;
import de.nicograef.lexiban.repository.IbanRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for IbanService — the orchestrator. Both validators are mocked to test orchestration
 * logic in isolation (call order, fallback behavior, caching).
 */
@ExtendWith(MockitoExtension.class)
class IbanServiceTest {

    private IbanService service;

    @Mock private LocalIbanValidator localValidator;

    @Mock private OpenIbanValidator openIbanValidator;

    @Mock private IbanRepository ibanRepository;

    @BeforeEach
    void setUp() {
        service = new IbanService(localValidator, openIbanValidator, ibanRepository);
    }

    // ── Structural errors (IbanFormatException) ──

    @Test
    void emptyInputThrowsFormatException() {
        assertThrows(IbanFormatException.class, () -> service.validateOrLookup(""));
    }

    @Test
    void tooShortInputThrowsFormatException() {
        var ex = assertThrows(IbanFormatException.class, () -> service.validateOrLookup("DE68"));
        assertTrue(ex.getMessage().contains("zu kurz"));
    }

    @Test
    void tooLongInputThrowsFormatException() {
        var ex =
                assertThrows(
                        IbanFormatException.class,
                        () -> service.validateOrLookup("DE89370400440532013000EXTRA1234567890"));
        assertTrue(ex.getMessage().contains("zu lang"));
    }

    // ── Cache hit ──

    @Test
    void returnsCachedResult() {
        Iban cached = new Iban("DE89370400440532013000", "Commerzbank", true, null);
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.of(cached));

        var result = service.validateOrLookup("DE89370400440532013000");

        assertTrue(result.valid());
        assertEquals("Commerzbank", result.bankName());
        // Validators should not be called when cache hits
        verify(localValidator, never()).validate(any());
        verify(openIbanValidator, never()).validate(any());
    }

    // ── Local validator returns definitive result ──

    @Test
    void localInvalidStopsChain() {
        when(ibanRepository.findById("DE00370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any()))
                .thenReturn(
                        Optional.of(
                                new ValidationResult(
                                        false,
                                        "DE00370400440532013000",
                                        null,
                                        "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)")));

        var result = service.validateOrLookup("DE00370400440532013000");

        assertFalse(result.valid());
        // External should NOT be called — local gave a definitive answer
        verify(openIbanValidator, never()).validate(any());
    }

    @Test
    void localValidWithBankNameStopsChain() {
        when(ibanRepository.findById("DE92500700100092585702")).thenReturn(Optional.empty());
        when(localValidator.validate(any()))
                .thenReturn(
                        Optional.of(
                                new ValidationResult(
                                        true, "DE92500700100092585702", "Deutsche Bank", null)));

        var result = service.validateOrLookup("DE92500700100092585702");

        assertTrue(result.valid());
        assertEquals("Deutsche Bank", result.bankName());
        verify(openIbanValidator, never()).validate(any());
    }

    // ── Local returns empty → external fallback ──

    @Test
    void fallsBackToExternalWhenLocalReturnsEmpty() {
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any())).thenReturn(Optional.empty());
        when(openIbanValidator.validate(any()))
                .thenReturn(
                        Optional.of(
                                new ValidationResult(
                                        true, "DE89370400440532013000", "Commerzbank", null)));

        var result = service.validateOrLookup("DE89370400440532013000");

        assertTrue(result.valid());
        assertEquals("Commerzbank", result.bankName());
        verify(openIbanValidator).validate(any());
    }

    @Test
    void externalInvalidResultIsUsed() {
        when(ibanRepository.findById("GB29NWBK60161331926819")).thenReturn(Optional.empty());
        when(localValidator.validate(any())).thenReturn(Optional.empty());
        when(openIbanValidator.validate(any()))
                .thenReturn(
                        Optional.of(
                                new ValidationResult(
                                        false,
                                        "GB29NWBK60161331926819",
                                        null,
                                        "Validation failed")));

        var result = service.validateOrLookup("GB29NWBK60161331926819");

        assertFalse(result.valid());
    }

    // ── Both return empty → graceful fallback ──

    @Test
    void fallbackWhenBothValidatorsReturnEmpty() {
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any())).thenReturn(Optional.empty());
        when(openIbanValidator.validate(any())).thenReturn(Optional.empty());

        var result = service.validateOrLookup("DE89370400440532013000");

        // Graceful degradation: valid (Mod-97 passed in local) but no bank name
        assertTrue(result.valid());
        assertNull(result.bankName());
        assertNull(result.reason());
        assertEquals("DE89370400440532013000", result.iban());
    }

    // ── Persistence ──

    @Test
    void persistsResultToDatabase() {
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any()))
                .thenReturn(
                        Optional.of(
                                new ValidationResult(
                                        true, "DE89370400440532013000", "Commerzbank", null)));

        service.validateOrLookup("DE89370400440532013000");

        verify(ibanRepository).save(any(Iban.class));
    }

    @Test
    void cachedResultIsNotPersistedAgain() {
        Iban cached = new Iban("DE89370400440532013000", "Commerzbank", true, null);
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.of(cached));

        service.validateOrLookup("DE89370400440532013000");

        verify(ibanRepository, never()).save(any());
    }

    // ── Input normalization ──

    @Test
    void normalizesInputBeforeProcessing() {
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any())).thenReturn(Optional.empty());
        when(openIbanValidator.validate(any())).thenReturn(Optional.empty());

        var result = service.validateOrLookup("de89 3704 0044 0532 0130 00");

        assertEquals("DE89370400440532013000", result.iban());
    }
}
