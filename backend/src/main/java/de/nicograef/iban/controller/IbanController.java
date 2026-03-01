package de.nicograef.iban.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.nicograef.iban.model.ValidationResult;
import de.nicograef.iban.repository.IbanRepository;
import de.nicograef.iban.service.IbanService;
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
 * IbanService.validateOrLookup(). The controller is a one-liner.
 */
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

        private final IbanService ibanService;
        private final IbanRepository ibanRepository;

        public IbanController(
                        IbanService ibanService,
                        IbanRepository ibanRepository) {
                this.ibanService = ibanService;
                this.ibanRepository = ibanRepository;
        }

        public record IbanRequest(@NotBlank String iban) {
        }

        public record IbanResponse(
                        boolean valid,
                        String iban,
                        String bankName,
                        String reason) {
        }

        public record IbanListEntry(
                        String iban,
                        String bankName,
                        boolean valid,
                        String reason) {
        }

        /**
         * POST /api/ibans — Validate (or lookup cached result) and save the IBAN.
         *
         * HTTP status semantics:
         * - 400 Bad Request: structurally malformed input (too short, wrong format).
         * IbanFormatException is thrown by IbanNumber → caught by
         * GlobalExceptionHandler.
         * - 200 OK: structurally valid IBAN — response contains valid=true/false
         * (e.g. wrong check digit → valid=false with reason, but still 200).
         *
         * Valid and semantically-invalid IBANs (wrong Mod-97, wrong length) are
         * persisted. Structurally malformed input is rejected with 400 and never
         * touches the database.
         */
        @PostMapping
        public ResponseEntity<IbanResponse> validateAndSaveIban(@Valid @RequestBody IbanRequest request) {
                ValidationResult result = ibanService.validateOrLookup(request.iban());

                return ResponseEntity.ok(new IbanResponse(
                                result.valid(),
                                result.iban(),
                                result.bankName(),
                                result.reason()));
        }

        /** GET /api/ibans — List all saved IBANs (unique, one per IBAN). */
        @GetMapping
        public ResponseEntity<List<IbanListEntry>> getAllIbans() {
                List<IbanListEntry> entries = ibanRepository.findAll().stream()
                                .map(iban -> new IbanListEntry(
                                                iban.getIban(),
                                                iban.getBankName(),
                                                iban.isValid(),
                                                iban.getReason()))
                                .toList();

                return ResponseEntity.ok(entries);
        }
}
