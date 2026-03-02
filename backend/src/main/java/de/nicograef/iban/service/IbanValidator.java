package de.nicograef.iban.service;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;
import java.util.Optional;

/**
 * Strategy interface for IBAN validation. Returns {@code Optional.empty()} if this validator cannot
 * handle the given IBAN (another validator should try). Definitive failures must always be
 * returned.
 */
public interface IbanValidator {

    Optional<ValidationResult> validate(IbanNumber iban);
}
