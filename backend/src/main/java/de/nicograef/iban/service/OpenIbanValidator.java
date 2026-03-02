package de.nicograef.iban.service;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Fallback IBAN validator — calls the openiban.com REST API when LocalIbanValidator cannot produce
 * a result. Returns {@code Optional.empty()} if the API is unreachable.
 *
 * @see <a href="https://openiban.com/">openiban.com</a>
 */
@Service
public class OpenIbanValidator implements IbanValidator {

    private static final Logger log = LoggerFactory.getLogger(OpenIbanValidator.class);
    static final String BASE_URL = "https://openiban.com/validate/";

    private final RestClient restClient;

    public OpenIbanValidator(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

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
            String reason = response.valid() ? null : String.join("; ", response.messages());

            return Optional.of(
                    new ValidationResult(response.valid(), iban.value(), bankName, reason));
        } catch (Exception e) {
            log.warn("External IBAN validation failed for {}: {}", iban.value(), e.getMessage());
            return Optional.empty();
        }
    }

    /** Relevant subset of the openiban.com JSON response. */
    record OpenIbanResponse(boolean valid, String[] messages, BankData bankData) {}

    record BankData(String name) {}
}
