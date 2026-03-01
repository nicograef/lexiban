package de.nicograef.iban.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.repository.IbanRepository;
import de.nicograef.iban.service.IbanValidationService;
import de.nicograef.iban.service.IbanValidationService.ValidationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for IBAN validation and persistence.
 * Base path: /api/ibans
 *
 * This controller is intentionally thin — it only handles:
 * - HTTP request/response mapping (DTOs ↔ domain objects)
 * - Delegating to the service layer for all business logic
 *
 * The orchestration (lookup → validate → external fallback → save) lives in
 * IbanValidationService.validateOrLookup(). The controller is a one-liner.
 */
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

    private final IbanValidationService validationService;
    private final IbanRepository ibanRepository;

    public IbanController(
            IbanValidationService validationService,
            IbanRepository ibanRepository) {
        this.validationService = validationService;
        this.ibanRepository = ibanRepository;
    }

    public record IbanRequest(@NotBlank String iban) {
    }

    public record IbanResponse(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String reason) {
    }

    public record IbanListEntry(
            String iban,
            String bankName,
            String bankIdentifier,
            boolean valid,
            String reason) {
    }

    /**
     * POST /api/ibans — Validate (or lookup cached result) and save the IBAN.
     *
     * Flow (handled by the service):
     * 1. Normalize input → IbanNumber value object.
     * 2. Check database for existing result (cache hit → return immediately).
     * 3. On cache miss: validate locally → external API fallback → save.
     *
     * Both valid and invalid IBANs are persisted. Each IBAN exists exactly once
     * in the database (natural primary key prevents duplicates).
     *
     * If the raw input fails structural validation (can't create IbanNumber),
     * we use validateRaw() which returns a descriptive error without saving.
     */
    @PostMapping
    public ResponseEntity<IbanResponse> validateAndSaveIban(@Valid @RequestBody IbanRequest request) {
        ValidationResult result;
        try {
            IbanNumber ibanNumber = new IbanNumber(request.iban());
            result = validationService.validateOrLookup(ibanNumber);
        } catch (IllegalArgumentException e) {
            // Input failed IbanNumber structural validation — return error without saving
            result = validationService.validateRaw(request.iban());
        }

        return ResponseEntity.ok(new IbanResponse(
                result.valid(),
                result.iban(),
                result.bankName(),
                result.bankIdentifier(),
                result.reason()));
    }

    /** GET /api/ibans — List all saved IBANs (unique, one per IBAN). */
    @GetMapping
    public ResponseEntity<List<IbanListEntry>> getAllIbans() {
        List<IbanListEntry> entries = ibanRepository.findAll().stream()
                .map(iban -> new IbanListEntry(
                        iban.getIban(),
                        iban.getBankName(),
                        iban.getBankIdentifier(),
                        iban.isValid(),
                        iban.getReason()))
                .toList();

        return ResponseEntity.ok(entries);
    }
}
