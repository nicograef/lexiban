package de.nicograef.lexiban.service;

import de.nicograef.lexiban.model.Iban;
import de.nicograef.lexiban.model.IbanFormatException;
import de.nicograef.lexiban.model.IbanNumber;
import de.nicograef.lexiban.repository.IbanRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for IBAN validation — coordinates parsing, DB caching, and the validator chain
 * (local → external → fallback). Contains no validation logic itself.
 */
@Service
public class IbanService {

    private static final Logger log = LoggerFactory.getLogger(IbanService.class);

    private final LocalIbanValidator localValidator;
    private final OpenIbanValidator openIbanValidator;
    private final IbanRepository ibanRepository;

    public IbanService(
            LocalIbanValidator localValidator,
            OpenIbanValidator openIbanValidator,
            IbanRepository ibanRepository) {
        this.localValidator = localValidator;
        this.openIbanValidator = openIbanValidator;
        this.ibanRepository = ibanRepository;
    }

    /**
     * Validate or look up an IBAN. Pipeline: Parse → Cache → Local → External → Fallback → Persist.
     */
    public ValidationResult validateOrLookup(String rawIban) {
        IbanNumber ibanNumber;
        try {
            ibanNumber = new IbanNumber(rawIban);
        } catch (IbanFormatException e) {
            return new ValidationResult(false, e.getNormalizedIban(), null, e.getMessage());
        }

        // Cache lookup
        Optional<Iban> cached = ibanRepository.findById(ibanNumber.value());
        if (cached.isPresent()) {
            log.debug("Cache hit for IBAN {}", ibanNumber.value());
            Iban entity = cached.get();
            return new ValidationResult(
                    entity.isValid(), entity.getIban(), entity.getBankName(), entity.getReason());
        }

        // Local validation (length + Mod-97 + BLZ)
        Optional<ValidationResult> result = localValidator.validate(ibanNumber);

        // External fallback (only if local returned empty)
        if (result.isEmpty()) {
            result = openIbanValidator.validate(ibanNumber);
        }

        // Fallback: both validators empty → valid without bank name (length + Mod-97
        // passed)
        if (result.isEmpty()) {
            log.info(
                    "No validator produced a result for {} — falling back to valid without bank name",
                    ibanNumber.value());
        }

        ValidationResult finalResult =
                result.orElse(new ValidationResult(true, ibanNumber.value(), null, null));

        // Persist
        ibanRepository.save(
                new Iban(
                        finalResult.iban(),
                        finalResult.bankName(),
                        finalResult.valid(),
                        finalResult.reason()));

        return finalResult;
    }
}
