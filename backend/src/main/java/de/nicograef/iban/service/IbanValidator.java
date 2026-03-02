package de.nicograef.iban.service;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;
import java.util.Optional;

/**
 * Strategy interface for IBAN validation.
 *
 * <p>Each implementation attempts to produce a complete ValidationResult (including bank name for
 * valid IBANs). Returns Optional.empty() if it cannot handle the given IBAN (e.g. unknown BLZ, API
 * unreachable).
 *
 * <p>Definitive failures (wrong length, bad Mod-97 check digits) MUST be returned as
 * Optional.of(invalidResult) — they are universal and don't need external confirmation.
 */
public interface IbanValidator {

    /**
     * Attempt to validate the given IBAN.
     *
     * @param iban Normalized, structurally valid IbanNumber
     * @return Present with a definitive result, or empty if this validator cannot handle the IBAN
     *     (another validator should try)
     */
    Optional<ValidationResult> validate(IbanNumber iban);
}
