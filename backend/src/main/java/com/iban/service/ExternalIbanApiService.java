package com.iban.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * External IBAN validation via openiban.com API.
 * Used as fallback when local validation doesn't resolve the bank name.
 *
 * ── Analogy ──
 * This is like a service module that wraps an external HTTP call.
 * In TS/Node:  const response = await fetch(`https://openiban.com/validate/${iban}`)
 * In Go:       resp, err := http.Get("https://openiban.com/validate/" + iban)
 *
 * @Service — same as in IbanValidationService: registers this class as a
 * Spring-managed singleton bean available for dependency injection.
 */
@Service
public class ExternalIbanApiService {

    // ── Logging ──
    // SLF4J + Logback is the standard Java logging stack.
    // ≈ console.log / console.warn in Node.js, or log.Println in Go.
    // LoggerFactory.getLogger() creates a logger scoped to this class name.
    private static final Logger log = LoggerFactory.getLogger(ExternalIbanApiService.class);
    private static final String BASE_URL = "https://openiban.com/validate/";

    // ── RestClient ──
    // RestClient is Spring 6.1+'s modern, fluent HTTP client.
    // Think of it as a pre-configured fetch() or axios instance.
    // ≈ const apiClient = axios.create({ baseURL: 'https://openiban.com/validate/' })
    // ≈ In Go: &http.Client{} with a base URL helper.
    private final RestClient restClient;

    public ExternalIbanApiService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    // Result DTO — see IbanValidationService for explanation of Java Records.
    public record ExternalValidationResult(
            boolean valid,
            String bankName,
            String bic) {
    }

    /**
     * Validate IBAN via openiban.com.
     * Returns null if the external service is unavailable.
     *
     * The call chain:
     *   restClient.get()                     — start a GET request
     *     .uri("{iban}?...", iban)            — expand the URI template (≈ template literal)
     *     .retrieve()                         — execute the HTTP request
     *     .body(OpenIbanResponse.class)       — deserialize JSON → Java Record
     *
     * ── TS equivalent ──
     *   const res = await fetch(`${BASE_URL}${iban}?getBIC=true&validateBankCode=true`)
     *   const data: OpenIbanResponse = await res.json()
     */
    public ExternalValidationResult validate(String iban) {
        try {
            var response = restClient.get()
                    .uri("{iban}?getBIC=true&validateBankCode=true", iban)
                    .retrieve()
                    .body(OpenIbanResponse.class);

            if (response == null) {
                return null;
            }

            String bankName = response.bankData() != null ? response.bankData().name() : null;
            String bic = response.bankData() != null ? response.bankData().bic() : null;

            return new ExternalValidationResult(response.valid(), bankName, bic);
        } catch (Exception e) {
            // Graceful degradation: if the external API is down, log a warning
            // and return null so the caller can continue without it.
            log.warn("External IBAN validation failed for {}: {}", iban, e.getMessage());
            return null;
        }
    }

    // ── Private DTOs for JSON deserialization ──
    // These records model the openiban.com response structure.
    // Spring uses Jackson (a JSON library, ≈ JSON.parse with type mapping) to
    // automatically map JSON fields to record fields by name.
    // "private record" means these are only visible inside this class.

    private record OpenIbanResponse(
            boolean valid,
            BankData bankData) {
    }

    private record BankData(
            String name,
            String bic) {
    }
}
