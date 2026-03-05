package de.nicograef.lexiban.controller;

import de.nicograef.lexiban.repository.IbanRepository;
import de.nicograef.lexiban.service.IbanService;
import de.nicograef.lexiban.service.ValidationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for IBAN validation. Base path: /api/ibans. Thin layer — delegates to service.
 */
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

    private static final Logger log = LoggerFactory.getLogger(IbanController.class);

    private final IbanService ibanService;
    private final IbanRepository ibanRepository;

    public IbanController(IbanService ibanService, IbanRepository ibanRepository) {
        this.ibanService = ibanService;
        this.ibanRepository = ibanRepository;
    }

    public record IbanRequest(@NotBlank String iban) {}

    public record IbanResponse(boolean valid, String iban, String bankName, String reason) {}

    public record IbanListEntry(String iban, String bankName, boolean valid, String reason) {}

    /**
     * POST /api/ibans — validate (or return cached result). Returns 400 for structurally malformed
     * input (IbanFormatException), 200 with valid=true/false otherwise.
     */
    @PostMapping
    public ResponseEntity<IbanResponse> validateAndSaveIban(
            @Valid @RequestBody IbanRequest request) {
        log.info("POST /api/ibans — validating IBAN: {}", request.iban());
        ValidationResult result = ibanService.validateOrLookup(request.iban());

        return ResponseEntity.ok(
                new IbanResponse(
                        result.valid(), result.iban(), result.bankName(), result.reason()));
    }

    /** GET /api/ibans — list all saved IBANs. */
    @GetMapping
    public ResponseEntity<List<IbanListEntry>> getAllIbans() {
        List<IbanListEntry> entries =
                ibanRepository.findAllByOrderByCreatedAtDesc().stream()
                        .map(
                                iban ->
                                        new IbanListEntry(
                                                iban.getIban(),
                                                iban.getBankName(),
                                                iban.isValid(),
                                                iban.getReason()))
                        .toList();

        return ResponseEntity.ok(entries);
    }
}
