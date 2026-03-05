package de.nicograef.lexiban.service;

import de.nicograef.lexiban.model.IbanNumber;
import java.util.Optional;

public interface IbanValidator {
    Optional<ValidationResult> validate(IbanNumber iban);
}
