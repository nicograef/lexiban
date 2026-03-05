package de.nicograef.lexiban.service;

import de.nicograef.lexiban.model.IbanNumber;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Local IBAN validator — country-specific length check, Mod-97 check digit verification, and German
 * BLZ → bank name lookup.
 *
 * <p>Returns definitive failures (wrong length, bad Mod-97) immediately. Returns a complete result
 * for known German banks. Returns {@code Optional.empty()} for unknown banks — signaling that
 * another validator should try.
 */
@Service
public class LocalIbanValidator implements IbanValidator {

    /** Demo subset of German BLZ → bank name mappings (production: Bundesbank BLZ file). */
    private static final Map<String, String> KNOWN_BANKS =
            Map.of(
                    "50070010", "Deutsche Bank",
                    "50040000", "Commerzbank",
                    "10050000", "Berliner Sparkasse");

    /**
     * Expected IBAN length per country code (ISO 13616). Unlisted countries skip the length check.
     */
    private static final Map<String, Integer> COUNTRY_LENGTHS =
            Map.ofEntries(
                    Map.entry("AT", 20), // Austria
                    Map.entry("BE", 16), // Belgium
                    Map.entry("CH", 21), // Switzerland
                    Map.entry("DE", 22), // Germany
                    Map.entry("DK", 18), // Denmark
                    Map.entry("ES", 24), // Spain
                    Map.entry("FI", 18), // Finland
                    Map.entry("FR", 27), // France
                    Map.entry("GB", 22), // United Kingdom
                    Map.entry("IE", 22), // Ireland
                    Map.entry("IT", 27), // Italy
                    Map.entry("LU", 20), // Luxembourg
                    Map.entry("MT", 31), // Malta (longest in Europe)
                    Map.entry("NL", 18), // Netherlands
                    Map.entry("NO", 15), // Norway (shortest in Europe)
                    Map.entry("PL", 28), // Poland
                    Map.entry("PT", 25), // Portugal
                    Map.entry("SE", 24) // Sweden
                    );

    private final Mod97Validator mod97Validator;

    public LocalIbanValidator(Mod97Validator mod97Validator) {
        this.mod97Validator = mod97Validator;
    }

    @Override
    public Optional<ValidationResult> validate(IbanNumber iban) {
        String value = iban.value();

        // Country-specific length check
        String country = iban.countryCode();
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && value.length() != expectedLength) {
            return Optional.of(
                    new ValidationResult(
                            false,
                            value,
                            null,
                            "Ungültige Länge: "
                                    + value.length()
                                    + " statt "
                                    + expectedLength
                                    + " Zeichen für "
                                    + country));
        }

        // Mod-97 check digit validation
        if (!mod97Validator.isValid(value)) {
            return Optional.of(
                    new ValidationResult(
                            false,
                            value,
                            null,
                            "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)"));
        }

        // BLZ lookup (German IBANs only)
        String bankIdentifier = iban.bankIdentifier().orElse(null);
        String bankName = bankIdentifier != null ? KNOWN_BANKS.get(bankIdentifier) : null;

        if (bankName != null) {
            return Optional.of(new ValidationResult(true, value, bankName, null));
        }

        // Bank unknown → let another validator try
        return Optional.empty();
    }
}
