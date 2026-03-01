package de.nicograef.iban.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.model.ValidationResult;

/**
 * Local IBAN validator — validates using country-specific length checks,
 * Mod-97 check digit verification, and a local German BLZ → bank name map.
 *
 * This validator can only produce a complete result for IBANs whose bank
 * it recognizes (BLZ in KNOWN_BANKS). For unknown banks or non-German IBANs,
 * it returns Optional.empty() — signaling that another validator should try.
 *
 * However, definitive failures (wrong country length, bad Mod-97) are always
 * returned — those checks are universal and don't need external confirmation.
 *
 * Pipeline:
 * 1. Country length check → wrong → definitive invalid
 * 2. Mod-97 check digits → wrong → definitive invalid
 * 3. BLZ in KNOWN_BANKS? → hit → valid + bankName
 * → miss → empty (another validator should try)
 *
 * TS analogy: a validation function that returns ValidationResult | null,
 * where null means "I can't handle this one, try another strategy."
 *
 * Go analogy: func (v *LocalValidator) Validate(iban IbanNumber)
 * (*ValidationResult, error)
 * returning nil result (not error) when the bank is unknown.
 */
@Service
public class LocalIbanValidator implements IbanValidator {

    /**
     * Demo subset of German BLZ → bank name mappings.
     * In production, this would be loaded from the Bundesbank BLZ file (~3,600
     * entries).
     * Only relevant for German IBANs — other countries use different bank
     * identifier schemes (Sort Code in GB, BC-Nummer in CH, etc.).
     */
    private static final Map<String, String> KNOWN_BANKS = Map.of(
            "50070010", "Deutsche Bank",
            "50040000", "Commerzbank",
            "10050000", "Berliner Sparkasse");

    /**
     * Expected IBAN length per country code (ISO 13616).
     * The shortest IBAN is Norway (15), the longest Malta (31), max possible is 34.
     * Countries not listed here skip the length check — only Mod-97 is applied.
     * Source: iban.md §5 + Wikipedia list of IBAN formats.
     */
    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.ofEntries(
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

    /**
     * Constructor injection — only depends on the pure Mod-97 algorithm.
     * No I/O, no repository, no external services.
     */
    public LocalIbanValidator(Mod97Validator mod97Validator) {
        this.mod97Validator = mod97Validator;
    }

    /**
     * Validate the IBAN locally: length + Mod-97 + BLZ lookup.
     *
     * @param iban Normalized, structurally valid IbanNumber
     * @return Definitive invalid result, definitive valid+bankName, or empty
     *         if the bank is unknown (= another validator should try)
     */
    @Override
    public Optional<ValidationResult> validate(IbanNumber iban) {
        String value = iban.value();

        // Step 1: Country-specific length check (definitive if wrong)
        String country = iban.countryCode();
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && value.length() != expectedLength) {
            return Optional.of(new ValidationResult(false, value, null,
                    "Ungültige Länge: " + value.length() + " statt " + expectedLength
                            + " Zeichen für " + country));
        }

        // Step 2: Mod-97 check digit validation (definitive if wrong)
        if (!mod97Validator.isValid(value)) {
            return Optional.of(new ValidationResult(false, value, null,
                    "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)"));
        }

        // Step 3: BLZ lookup — only for German IBANs with known banks
        String bankIdentifier = iban.bankIdentifier().orElse(null);
        String bankName = bankIdentifier != null ? KNOWN_BANKS.get(bankIdentifier) : null;

        if (bankName != null) {
            // Known bank → complete, definitive result
            return Optional.of(new ValidationResult(true, value, bankName, null));
        }

        // Bank unknown (BLZ not in local map, or non-German IBAN)
        // → can't produce a complete result → let another validator try
        return Optional.empty();
    }
}
