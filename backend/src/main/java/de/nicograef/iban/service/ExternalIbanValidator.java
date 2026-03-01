package de.nicograef.iban.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;

/**
 * External IBAN validator — calls the openiban.com REST API.
 *
 * Used as fallback when LocalIbanValidator cannot produce a complete result
 * (unknown BLZ or non-German IBAN). The external API performs its own
 * validation (including bank code verification) and returns bank info.
 *
 * Returns:
 * - Optional.of(result) with valid/invalid + bankName from the API
 * - Optional.empty() if the API is unreachable or returns null
 *
 * Implements IbanValidator so the IbanService (orchestrator) can treat
 * local and external validation uniformly via the Strategy Pattern.
 *
 * TS analogy: a validation function that calls fetch(url) and maps the
 * JSON response to a result type — returns null on network errors.
 *
 * The RestClient (since Spring 6.1) is the modern fluent HTTP client —
 * successor to RestTemplate. TS equivalent: fetch() or axios.
 */
@Service
public class ExternalIbanValidator implements IbanValidator {

    private static final Logger log = LoggerFactory.getLogger(ExternalIbanValidator.class);
    private static final String BASE_URL = "https://openiban.com/validate/";

    private final RestClient restClient;

    public ExternalIbanValidator() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * Validate the IBAN via openiban.com.
     *
     * Query params:
     * - getBIC=true → includes the BIC/SWIFT code in the response.
     * - validateBankCode=true → also verifies the bank code in the BBAN
     * actually exists (goes beyond Mod-97 which only checks the check digits).
     *
     * Returns Optional.empty() if the service is unavailable — the orchestrator
     * falls back to a valid-without-bank-name result (graceful degradation).
     *
     * @param iban Normalized, structurally valid IbanNumber
     * @return Present with the API's result, or empty if unreachable
     */
    @Override
    public Optional<ValidationResult> validate(IbanNumber iban) {
        try {
            var response = restClient.get()
                    .uri("{iban}?getBIC=true&validateBankCode=true", iban.value())
                    .retrieve()
                    .body(OpenIbanResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            String bankName = response.bankData() != null ? response.bankData().name() : null;

            return Optional.of(new ValidationResult(
                    response.valid(),
                    iban.value(),
                    bankName,
                    response.valid() ? null : "Externe Validierung: IBAN ist ungültig"));
        } catch (Exception e) {
            // Broad catch is intentional — the external API is best-effort.
            // A failure here must never block the local validation result.
            // TS equivalent: .catch(() => null)
            log.warn("External IBAN validation failed for {}: {}", iban.value(), e.getMessage());
            return Optional.empty();
        }
    }

    // ── DTOs matching the openiban.com JSON response structure ──

    private record OpenIbanResponse(
            boolean valid,
            BankData bankData) {
    }

    private record BankData(
            String name,
            String bic) {
    }
}
