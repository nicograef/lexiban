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
 * a result (unknown BLZ or non-German IBAN).
 *
 * <p>Returns {@code Optional.empty()} if the API is unreachable, allowing the orchestrator to fall
 * back to a valid-without-bank-name result.
 *
 * @see <a href="https://openiban.com/">openiban.com</a>
 * @see <a href="https://github.com/fourcube/goiban-service">goiban-service (Go source)</a>
 */
@Service
public class ExternalIbanValidator implements IbanValidator {

    private static final Logger log = LoggerFactory.getLogger(ExternalIbanValidator.class);
    static final String BASE_URL = "https://openiban.com/validate/";

    private final RestClient restClient;

    public ExternalIbanValidator(RestClient.Builder restClientBuilder) {
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

    /** Subset of the openiban.com JSON response — unknown fields are ignored by Jackson. */
    record OpenIbanResponse(boolean valid, String[] messages, BankData bankData) {}

    record BankData(String name) {}
}
