package de.nicograef.iban.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * External IBAN validation via openiban.com API.
 * Used as fallback when local validation doesn't resolve the bank name.
 */
@Service
public class ExternalIbanApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIbanApiService.class);
    private static final String BASE_URL = "https://openiban.com/validate/";

    private final RestClient restClient;

    public ExternalIbanApiService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public record ExternalValidationResult(
            boolean valid,
            String bankName,
            String bic) {
    }

    /** Validate IBAN via openiban.com. Returns null if the service is unavailable. */
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
            log.warn("External IBAN validation failed for {}: {}", iban, e.getMessage());
            return null;
        }
    }

    // DTOs matching the openiban.com JSON response structure.

    private record OpenIbanResponse(
            boolean valid,
            BankData bankData) {
    }

    private record BankData(
            String name,
            String bic) {
    }
}
