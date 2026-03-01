package de.nicograef.iban.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.nicograef.iban.model.Iban;
import de.nicograef.iban.repository.IbanRepository;
import de.nicograef.iban.service.ExternalIbanApiService;
import de.nicograef.iban.service.IbanValidationService;
import de.nicograef.iban.service.IbanValidationService.ValidationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for IBAN validation and persistence.
 * Base path: /api/ibans
 */
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

    private final IbanValidationService validationService;
    private final ExternalIbanApiService externalApiService;
    private final IbanRepository ibanRepository;

    public IbanController(
            IbanValidationService validationService,
            ExternalIbanApiService externalApiService,
            IbanRepository ibanRepository) {
        this.validationService = validationService;
        this.externalApiService = externalApiService;
        this.ibanRepository = ibanRepository;
    }

    public record IbanRequest(@NotBlank String iban) {
    }

    public record IbanResponse(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String validationMethod) {
    }

    public record IbanListEntry(
            Long id,
            String iban,
            String bankName,
            String bankIdentifier,
            boolean valid,
            String validationMethod) {
    }

    /** POST /api/ibans/validate — Validate IBAN without saving. */
    @PostMapping("/validate")
    public ResponseEntity<IbanResponse> validateIban(@Valid @RequestBody IbanRequest request) {
        return ResponseEntity.ok(buildResponse(request.iban()));
    }

    /** POST /api/ibans — Validate and save IBAN. */
    @PostMapping
    public ResponseEntity<IbanResponse> validateAndSaveIban(@Valid @RequestBody IbanRequest request) {
        IbanResponse response = buildResponse(request.iban());

        ibanRepository.save(new Iban(
                response.iban(),
                response.bankName(),
                response.bankIdentifier(),
                response.valid(),
                response.validationMethod()));

        return ResponseEntity.ok(response);
    }

    /** GET /api/ibans — List all saved IBANs. */
    @GetMapping
    public ResponseEntity<List<IbanListEntry>> getAllIbans() {
        List<IbanListEntry> entries = ibanRepository.findAll().stream()
                .map(iban -> new IbanListEntry(
                        iban.getId(),
                        iban.getIban(),
                        iban.getBankName(),
                        iban.getBankIdentifier(),
                        iban.isValid(),
                        iban.getValidationMethod()))
                .toList();

        return ResponseEntity.ok(entries);
    }

    /**
     * Shared validation logic used by both validate and validateAndSave.
     * Runs local Mod-97 check first, then falls back to the external API
     * if the IBAN is valid but the bank name couldn't be resolved locally.
     */
    private IbanResponse buildResponse(String rawIban) {
        ValidationResult result = validationService.validate(rawIban);

        String bankName = result.bankName();
        String validationMethod = result.validationMethod();

        // Fallback to external API if BLZ is not in KNOWN_BANKS
        if (result.valid() && bankName == null) {
            var external = externalApiService.validate(result.iban());
            if (external != null && external.bankName() != null) {
                bankName = external.bankName();
                validationMethod = "external";
            }
        }

        return new IbanResponse(
                result.valid(),
                result.iban(),
                bankName,
                result.bankIdentifier(),
                validationMethod);
    }
}
