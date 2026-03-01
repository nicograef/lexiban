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
 * 1. Parse raw input → IbanNumber (structural check, throws IbanFormatException
 * → 400).
 * 2. Validate an IBAN (country length, Mod-97) — returns valid/invalid → 200.
 * 3. Enrich with bank info (local BLZ map, then external API fallback).
 * 4. Persist the result in the database (cache).
 *
 * The pipeline is: Parse → Cache? → Validate → Enrich → Persist.
 * Each step has a clear single responsibility.
 *
 * The Controller is a thin HTTP ↔ Service adapter and delegates all business
 * logic to this service via a single method call.
 *
 * TS analogy: an Express service module that handles all business logic before
 * the route handler formats the response.
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
     * TS equivalent: { valid: boolean; iban: string; reason?: string; ... }
     */
    public record ValidationResult(
            boolean valid,
            String iban,
            String bankName,
            String reason) {
    }

    /**
     * Primary entry point: validate or look up an IBAN from raw string input.
     *
     * Pipeline (each step has a single responsibility):
     * 1. Parse — raw input → IbanNumber (normalize + structural check).
     * Throws IbanFormatException → caught by GlobalExceptionHandler → 400.
     * 2. Cache — is this IBAN already in the database? → early return.
     * 3. Validate — country-specific length check + Mod-97 check digit.
     * 4. Enrich — resolve bank name: local BLZ map, then external API fallback.
     * 5. Persist — save result to database (one row per IBAN, natural PK).
     *
     * @param rawIban Raw IBAN string (may contain spaces, hyphens, lowercase)
     * @return Validation result (from cache or freshly computed)
     * @throws IbanFormatException if the input is structurally not an IBAN (→ HTTP
     *                             400)
     */
    public ValidationResult validateOrLookup(String rawIban) {
        // Step 1: Parse + normalize → IbanNumber (structural validation).
        // IbanFormatException bubbles up → GlobalExceptionHandler → HTTP 400.
        IbanNumber ibanNumber = new IbanNumber(rawIban);

        // Step 2: Cache lookup — already in the database?
        Optional<Iban> cached = ibanRepository.findById(ibanNumber.value());
        if (cached.isPresent()) {
            Iban entity = cached.get();
            return new ValidationResult(
                    entity.isValid(),
                    entity.getIban(),
                    entity.getBankName(),
                    entity.getReason());
        }

        // Step 3: Validate — country length + Mod-97 (pure, no side effects)
        ValidationResult result = validate(ibanNumber);

        // Step 4: Enrich — resolve bank name (local map → external API fallback)
        if (result.valid()) {
            result = enrichBankInfo(ibanNumber, result);
        }

        // Step 5: Persist result
        ibanRepository.save(new Iban(
                result.iban(),
                result.bankName(),
                result.valid(),
                result.reason()));

        return result;
    }

    /**
     * Pure validation: country-specific length check + Mod-97 check digit.
     * No side effects (no DB, no external API, no bank name lookup).
     *
     * @param ibanNumber Normalized IBAN (IbanNumber guarantees structural validity)
     * @return Validation result with valid/invalid + reason (no bank info yet)
     */
    private ValidationResult validate(IbanNumber ibanNumber) {
        String iban = ibanNumber.value();

        // Country-specific length check (only for known countries)
        String country = ibanNumber.countryCode();
        Integer expectedLength = COUNTRY_LENGTHS.get(country);
        if (expectedLength != null && iban.length() != expectedLength) {
            return new ValidationResult(false, iban, null,
                    "Ungültige Länge: " + iban.length() + " statt " + expectedLength
                            + " Zeichen für " + country);
        }

        // Mod-97 check digit validation
        if (!mod97Validator.isValid(iban)) {
            return new ValidationResult(false, iban, null,
                    "Prüfziffern ungültig (Modulo-97-Prüfung fehlgeschlagen)");
        }

        // Structurally + semantically valid — bank info comes in enrichBankInfo()
        return new ValidationResult(true, iban, null, null);
    }

    /**
     * Enrich a valid IBAN with bank information.
     * Strategy: local BLZ map first → external API fallback if no match.
     *
     * Separated from validate() because looking up a bank name is enrichment,
     * not validation. This makes each method easier to test and reason about.
     *
     * @param ibanNumber Normalized IBAN
     * @param result     Validation result (must be valid)
     * @return Result with bank name + identifier filled in (if resolvable)
     */
    private ValidationResult enrichBankInfo(IbanNumber ibanNumber, ValidationResult result) {
        // Extract BLZ directly from the value object — transient, not stored
        String bankIdentifier = ibanNumber.bankIdentifier().orElse(null);

        // Try local BLZ map first (instant, no I/O)
        String bankName = bankIdentifier != null ? KNOWN_BANKS.get(bankIdentifier) : null;

        // Fallback: external API (only if local map had no match)
        if (bankName == null) {
            var external = externalApiService.validate(ibanNumber.value());
            if (external != null && external.bankName() != null) {
                bankName = external.bankName();
            }
        }

        // Return enriched result (or unchanged if no bank info found)
        if (bankName != null) {
            return new ValidationResult(
                    result.valid(),
                    result.iban(),
                    bankName,
                    result.reason());
        }
        return result;
    }

    /**
     * Resolve bank name from BLZ. Returns null if unknown.
     */
    public String resolveBankName(String blz) {
        return KNOWN_BANKS.get(blz);
    }
}
