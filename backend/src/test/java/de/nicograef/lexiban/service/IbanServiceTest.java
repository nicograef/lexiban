package de.nicograef.lexiban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.nicograef.lexiban.model.Iban;
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

    @Test
    void structurallyInvalidInputReturnsInvalid() {
        var result = service.validateOrLookup("1");
        assertFalse(result.valid());
        assertEquals("1", result.iban());
        assertNull(result.bankName());
        assertEquals("IBAN zu kurz: 1 Zeichen (Minimum: 15)", result.reason());
    }

    @Test
    void returnsCachedResultWithoutCallingValidatorsOrPersisting() {
        Iban cached = new Iban("DE89370400440532013000", "Commerzbank", true, null);
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.of(cached));

        var result = service.validateOrLookup("DE89370400440532013000");

        assertTrue(result.valid());
        assertEquals("Commerzbank", result.bankName());
        verify(localValidator, never()).validate(any());
        verify(openIbanValidator, never()).validate(any());
        verify(ibanRepository, never()).save(any());
    }

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
        verify(ibanRepository).save(any(Iban.class));
    }

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
    void fallbackWhenBothValidatorsReturnEmpty() {
        when(ibanRepository.findById("DE89370400440532013000")).thenReturn(Optional.empty());
        when(localValidator.validate(any())).thenReturn(Optional.empty());
        when(openIbanValidator.validate(any())).thenReturn(Optional.empty());

        var result = service.validateOrLookup("DE89370400440532013000");

        assertTrue(result.valid());
        assertNull(result.bankName());
        assertNull(result.reason());
        assertEquals("DE89370400440532013000", result.iban());
    }
}
