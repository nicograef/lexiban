package de.nicograef.iban.service;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * External IBAN validator — calls the openiban.com REST API (source code:
 * https://github.com/fourcube/goiban)
 *
 * <p>Used as fallback when LocalIbanValidator cannot produce a complete result (unknown BLZ or
 * non-German IBAN). The external API performs its own validation (including bank code verification)
 * and returns bank info.
 *
 * <p>Returns: - Optional.of(result) with valid/invalid + bankName from the API - Optional.empty()
 * if the API is unreachable or returns null
 *
 * <p>Implements IbanValidator so the IbanService (orchestrator) can treat local and external
 * validation uniformly via the Strategy Pattern.
 *
 * <p>TS analogy: a validation function that calls fetch(url) and maps the JSON response to a result
 * type — returns null on network errors.
 *
 * <p>The RestClient (since Spring 6.1) is the modern fluent HTTP client — successor to
 * RestTemplate. TS equivalent: fetch() or axios.
 */
@Service
public class ExternalIbanValidator implements IbanValidator {

    private static final Logger log = LoggerFactory.getLogger(ExternalIbanValidator.class);
    private static final String BASE_URL = "https://openiban.com/validate/";

    private final RestClient restClient;

    public ExternalIbanValidator() {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * Validate the IBAN via openiban.com.
     *
     * <p>Query params: - getBIC=true → includes the BIC/SWIFT code in the response. -
     * validateBankCode=true → also verifies the bank code in the BBAN actually exists (goes beyond
     * Mod-97 which only checks the check digits).
     *
     * <p>Returns Optional.empty() if the service is unavailable — the orchestrator falls back to a
     * valid-without-bank-name result (graceful degradation).
     *
     * @param iban Normalized, structurally valid IbanNumber
     * @return Present with the API's result, or empty if unreachable
     */
    @Override
    public Optional<ValidationResult> validate(IbanNumber iban) {
        try {
            var response =
                    restClient
                            .get()
                            .uri("{iban}?getBIC=true&validateBankCode=true", iban.value())
                            .retrieve()
                            .body(OpenIbanResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            String bankName = response.bankData() != null ? response.bankData().name() : null;

            return Optional.of(
                    new ValidationResult(
                            response.valid(),
                            iban.value(),
                            bankName,
                            response.valid() ? null : String.join("; ", response.messages())));
        } catch (Exception e) {
            // Broad catch is intentional — the external API is best-effort.
            // A failure here must never block the local validation result.
            // TS equivalent: .catch(() => null)
            log.warn("External IBAN validation failed for {}: {}", iban.value(), e.getMessage());
            return Optional.empty();
        }
    }

    // ── DTOs matching the openiban.com JSON response structure ──

    /**
     * Response object from the OpenIBAN API containing IBAN validation results.
     *
     * @param valid indicates whether the IBAN is valid
     * @param messages array of messages providing details about the validation result; contains the
     *     reason for validation failure when {@code valid} is false
     * @param bankData bank information associated with the IBAN
     */
    private record OpenIbanResponse(boolean valid, String[] messages, BankData bankData) {}

    private record BankData(String name) {}
}
