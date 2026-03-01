package de.nicograef.iban.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.nicograef.iban.model.Iban;
import de.nicograef.iban.model.IbanNumber;
import de.nicograef.iban.repository.IbanRepository;

/**
 * IBAN validation and orchestration service.
 *
 * Responsibilities:
 * 1. Validate an IBAN (structural check, country length, Mod-97, BLZ lookup).
 * 2. Orchestrate the full "validate or lookup" use case:
 * - Check if the IBAN is already cached in the database.
 * - If not: validate locally → fallback to external API → save result.
 *
 * This service now owns the orchestration logic that previously lived in the
 * controller. The controller becomes a thin HTTP ↔ Service adapter.
 *
 * TS analogy: an Express middleware that handles all business logic before the
 * route handler formats the response.
 *
 * See docs/iban.md for the algorithm details.
 */
@Service
public class IbanValidationService {

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
    private final ExternalIbanApiService externalApiService;
    private final IbanRepository ibanRepository;

    /**
     * Constructor injection — all dependencies are explicit and final.
     * TS analogy: constructor(private readonly mod97: Mod97Validator, ...) {}
     */
    public IbanValidationService(
            Mod97Validator mod97Validator,
            ExternalIbanApiService externalApiService,
            IbanRepository ibanRepository) {
        this.mod97Validator = mod97Validator;
        this.externalApiService = externalApiService;
        this.ibanRepository = ibanRepository;
    }

    /**
     * Result of an IBAN validation. The reason field provides a human-readable
     * explanation for why the IBAN is invalid (null when valid).
     *
     * Removed: validationMethod — was semantically fragwürdig (the validation
     * method is an internal implementation detail, not a business concern).
     *
     * TS equivalent: { valid: boolean; iban: string; reason?: string; ... }
     */
    public record ValidationResult(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String reason) {
    }

    /**
     * Full use-case orchestration: lookup cached result OR validate + save.
     *
     * Flow:
     * 1. Check database for existing IBAN (cache hit → return immediately).
     * 2. On cache miss: validate locally (Mod-97, BLZ lookup).
     * 3. If valid but no bank name: fallback to external API.
     * 4. Save result to database.
     * 5. Return validation result.
     *
     * This logic was previously split between Controller and Service.
     * Now it's all here — the Controller just calls this one method.
     *
     * @param ibanNumber Normalized IBAN (guaranteed by IbanNumber value object)
     * @return Validation result (from cache or freshly computed)
     */
    public ValidationResult validateOrLookup(IbanNumber ibanNumber) {
        // Step 1: Lookup — is this IBAN already in the database?
        Optional<Iban> cached = ibanRepository.findById(ibanNumber.value());
        if (cached.isPresent()) {
            Iban entity = cached.get();
            return new ValidationResult(
                    entity.isValid(),
                    entity.getIban(),
                    entity.getBankName(),
                    entity.getBankIdentifier(),
                    entity.getReason());
        }

        // Step 2: Cache miss — validate locally
        ValidationResult result = validate(ibanNumber);

        // Step 3: If valid but no bank name, try external API as fallback
        String bankName = result.bankName();
        if (result.valid() && bankName == null) {
            var external = externalApiService.validate(ibanNumber.value());
            if (external != null && external.bankName() != null) {
                bankName = external.bankName();
                // Rebuild result with external bank name
                result = new ValidationResult(
                        result.valid(),
                        result.iban(),
                        bankName,
                        result.bankIdentifier(),
                        result.reason());
            }
        }

        // Step 4: Save to database (one row per IBAN, no duplicates)
        ibanRepository.save(new Iban(
                result.iban(),
                result.bankName(),
                result.bankIdentifier(),
                result.valid(),
                result.reason()));

        return result;
    }

    /**
     * Validate an IBAN: structural check → length check → Mod-97 → BLZ lookup.
     * Pure validation with no side effects (no DB, no external API).
     *
     * @param ibanNumber Normalized IBAN (IbanNumber guarantees normalization)
     * @return Validation result
     */
    public ValidationResult validate(IbanNumber ibanNumber) {
        String iban = ibanNumber.value();

        // Step 1: Country-specific length check (only for known countries)
        String country = ibanNumber.countryCode();
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && iban.length() != expectedLength) {
            return new ValidationResult(false, iban, null, null,
                    "Ungültige Länge: " + iban.length() + " statt " + expectedLength
                            + " Zeichen für " + country);
        }

        // Step 2: Mod-97 check digit validation (delegated to Mod97Validator)
        if (!mod97Validator.isValid(iban)) {
            return new ValidationResult(false, iban, null, null,
                    "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)");
        }

        // Step 3: Extract bank identifier for German IBANs
        String bankIdentifier = ibanNumber.bankIdentifier().orElse(null);
        String bankName = bankIdentifier != null ? KNOWN_BANKS.get(bankIdentifier) : null;

        return new ValidationResult(true, iban, bankName, bankIdentifier, null);
    }

    /**
     * Validate from raw string input. Creates IbanNumber internally.
     * Returns an invalid result if the raw input fails structural validation.
     *
     * Used when the caller has a raw String that may not be structurally valid.
     * The IbanNumber constructor would throw for invalid input, so we catch that.
     */
    public ValidationResult validateRaw(String rawIban) {
        if (rawIban == null || rawIban.isBlank()) {
            return new ValidationResult(false, "", null, null, "IBAN ist leer");
        }

        // Normalize for error messages even if structurally invalid
        String normalized = rawIban.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        try {
            IbanNumber ibanNumber = new IbanNumber(rawIban);
            return validate(ibanNumber);
        } catch (IllegalArgumentException e) {
            // IbanNumber constructor rejected the input — describe why
            return new ValidationResult(false, normalized, null, null,
                    describeStructuralError(normalized));
        }
    }

    /**
     * Resolve bank name from BLZ. Returns null if unknown.
     */
    public String resolveBankName(String blz) {
        return KNOWN_BANKS.get(blz);
    }

    /**
     * Provides a specific German-language reason why the structural check failed.
     * Inspects the IBAN step by step to return the most helpful error message.
     */
    private String describeStructuralError(String iban) {
        if (iban.isEmpty()) {
            return "IBAN ist leer";
        }
        if (iban.length() < 15) {
            return "IBAN zu kurz: " + iban.length() + " Zeichen (Minimum: 15)";
        }
        if (iban.length() > 34) {
            return "IBAN zu lang: " + iban.length() + " Zeichen (Maximum: 34)";
        }
        if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))) {
            return "IBAN muss mit 2 Buchstaben (Ländercode) beginnen";
        }
        if (!Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3))) {
            return "Stelle 3–4 müssen Ziffern sein (Prüfziffern)";
        }
        return "Ungültiges IBAN-Format";
    }
}
