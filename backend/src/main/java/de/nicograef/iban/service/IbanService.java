package de.nicograef.iban.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.nicograef.iban.model.Iban;
import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;
import de.nicograef.iban.repository.IbanRepository;

/**
 * Orchestrator for IBAN validation — coordinates parsing, caching, and
 * the validator chain (local → external → fallback).
 *
 * This service contains NO validation logic itself. It only:
 * 1. Parses raw input → IbanNumber (structural check).
 * 2. Checks the DB cache for an existing result.
 * 3. Delegates to validators in order (Strategy Pattern).
 * 4. Persists the result.
 *
 * The validators implement the IbanValidator interface:
 * - LocalIbanValidator: length + Mod-97 + local BLZ map
 * - ExternalIbanValidator: openiban.com API fallback
 *
 * If both validators return Optional.empty() (unknown bank + API unreachable),
 * the service falls back to a valid-without-bank-name result — because the
 * local validator already confirmed length + Mod-97.
 *
 * TS analogy: an Express service that coordinates calls to strategy functions,
 * manages the database cache, and never contains business logic itself.
 */
@Service
public class IbanService {

    private static final Logger log = LoggerFactory.getLogger(IbanService.class);

    private final LocalIbanValidator localValidator;
    private final ExternalIbanValidator externalValidator;
    private final IbanRepository ibanRepository;

    /**
     * Constructor injection — the orchestrator depends on two validators and
     * the repository. No direct dependency on Mod97Validator or external API.
     */
    public IbanService(
            LocalIbanValidator localValidator,
            ExternalIbanValidator externalValidator,
            IbanRepository ibanRepository) {
        this.localValidator = localValidator;
        this.externalValidator = externalValidator;
        this.ibanRepository = ibanRepository;
    }

    /**
     * Validate or look up an IBAN from raw string input.
     *
     * Pipeline: Parse → Cache → Local → External → Fallback → Persist
     *
     * @param rawIban Raw IBAN string (may contain spaces, hyphens, lowercase)
     * @return Complete validation result (from cache or freshly computed)
     * @throws de.nicograef.iban.model.IbanFormatException if structurally not
     *                                                     an IBAN (→ HTTP 400)
     */
    public ValidationResult validateOrLookup(String rawIban) {
        // Step 1: Parse + normalize → IbanNumber (structural validation).
        // IbanFormatException bubbles up → GlobalExceptionHandler → HTTP 400.
        IbanNumber ibanNumber = new IbanNumber(rawIban);

        // Step 2: Cache lookup — already in the database?
        Optional<Iban> cached = ibanRepository.findById(ibanNumber.value());
        if (cached.isPresent()) {
            log.debug("Cache hit for IBAN {}", ibanNumber.value());
            Iban entity = cached.get();
            return new ValidationResult(
                    entity.isValid(),
                    entity.getIban(),
                    entity.getBankName(),
                    entity.getReason());
        }

        // Step 3: Local validation (length + Mod-97 + BLZ lookup).
        // Returns definitive invalid, definitive valid+bankName, or empty.
        Optional<ValidationResult> result = localValidator.validate(ibanNumber);

        // Step 4: External fallback — only called if local returned empty
        // (= IBAN passed checks but bank is unknown to local validator).
        if (result.isEmpty()) {
            result = externalValidator.validate(ibanNumber);
        }

        // Step 5: Fallback — both validators couldn't produce a result.
        // We know length + Mod-97 passed (local would have returned invalid).
        // Return valid without bank name (graceful degradation).
        if (result.isEmpty()) {
            log.info("No validator produced a result for {} — falling back to valid without bank name",
                    ibanNumber.value());
        }
        ValidationResult finalResult = result.orElse(
                new ValidationResult(true, ibanNumber.value(), null, null));

        // Step 6: Persist result to database (one row per IBAN, natural PK).
        ibanRepository.save(new Iban(
                finalResult.iban(),
                finalResult.bankName(),
                finalResult.valid(),
                finalResult.reason()));

        return finalResult;
    }
}
