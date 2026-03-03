package de.nicograef.lexiban.service;

import de.nicograef.lexiban.model.IbanNumber;
import de.nicograef.lexiban.model.ValidationResult;
import java.util.Optional;

/**
 * Strategy interface for IBAN validation. Returns {@code Optional.empty()} if this validator cannot
 * handle the given IBAN (another validator should try). Definitive failures must always be
 * returned.
 */
public interface IbanValidator {

    Optional<ValidationResult> validate(IbanNumber iban);
}
