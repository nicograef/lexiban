package com.iban.controller;

import com.iban.model.Iban;
import com.iban.repository.IbanRepository;
import com.iban.service.ExternalIbanApiService;
import com.iban.service.IbanValidationService;
import com.iban.service.IbanValidationService.ValidationResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for IBAN validation and persistence.
 *
 * ── Analogy ──
 * This is the equivalent of an Express Router or a Go http.Handler.
 * In Express: const router = express.Router(); router.post('/validate', ...)
 * In Go: http.HandleFunc("/api/ibans/validate", handler)
 *
 * @RestController combines two annotations:
 *                 - @Controller — marks this class as a web request handler (≈
 *                 router)
 *                 - @ResponseBody — every method return value is serialized to
 *                 JSON
 *                 automatically (≈ res.json(...) in Express)
 *
 *                 @RequestMapping("/api/ibans") — sets the base path for all
 *                 endpoints in this
 *                 controller, like Express's router.use('/api/ibans', ...) or a
 *                 Go route group.
 */
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

    // ── Dependency Injection (Constructor Injection) ──
    // Spring automatically passes ("injects") these dependencies when creating
    // the controller. This is similar to Angular's constructor DI or manually
    // wiring dependencies in a Go main() function:
    //
    // // Go equivalent:
    // validationSvc := service.NewIbanValidationService()
    // externalSvc := service.NewExternalIbanApiService()
    // controller := NewIbanController(validationSvc, externalSvc, repo)
    //
    // Spring does this automatically because those classes are annotated with
    // @Service / @Repository (which registers them as "beans" — managed objects).
    // The "final" keyword means these fields can only be assigned once (≈ readonly
    // in TS).

    private final IbanValidationService validationService;
    private final ExternalIbanApiService externalApiService;
    private final IbanRepository ibanRepository;

    public IbanController(
            IbanValidationService validationService,
            ExternalIbanApiService externalApiService,
            IbanRepository ibanRepository) {
        this.validationService = validationService;
        this.externalApiService = externalApiService;
        this.ibanRepository = ibanRepository;
    }

    // ── DTOs as Java Records ──
    // Java Records are immutable data classes — like TypeScript `type` /
    // `interface`
    // or Go `struct`. The compiler auto-generates constructor, getters, equals(),
    // hashCode(), and toString(). No Lombok needed.
    //
    // TS equivalent: type IbanRequest = { iban: string }
    // Go equivalent: type IbanRequest struct { Iban string `json:"iban"` }
    //
    // @NotBlank is a validation constraint (from Jakarta Validation, ≈ zod's
    // z.string().min(1)).
    // Combined with @Valid on the parameter, Spring rejects blank values with HTTP
    // 400.

    record IbanRequest(@NotBlank String iban) {
    }

    record IbanResponse(
            boolean valid,
            String iban,
            String bankName,
            String bankIdentifier,
            String validationMethod) {
    }

    record IbanListEntry(
            Long id,
            String iban,
            String bankName,
            String bankIdentifier,
            boolean valid,
            String validationMethod) {
    }

    // ── Endpoints ──

    /**
     * POST /api/ibans/validate — Validate IBAN without saving.
     *
     * @PostMapping marks this method as a POST handler.
     *              ≈ Express: router.post('/validate', async (req, res) => { ... })
     *
     * @Valid triggers validation of the request body against the constraints
     *        defined in IbanRequest (e.g. @NotBlank). If validation fails, Spring
     *        returns 400 and the error is handled by GlobalExceptionHandler.
     *        ≈ In Express: a zod .parse() in a middleware.
     *
     * @RequestBody tells Spring to deserialize the JSON request body into the
     *              IbanRequest record. ≈ req.body in Express (after express.json()
     *              middleware).
     *
     *              ResponseEntity<T> wraps the response with an HTTP status code.
     *              ResponseEntity.ok(...) → HTTP 200 with JSON body.
     *              ≈ res.status(200).json({...}) in Express.
     */
    @PostMapping("/validate")
    public ResponseEntity<IbanResponse> validateIban(@Valid @RequestBody IbanRequest request) {
        return ResponseEntity.ok(buildResponse(request.iban()));
    }

    /**
     * POST /api/ibans — Validate and save IBAN.
     *
     * Same logic as validateIban(), but additionally persists the IBAN entity
     * to the database via the repository.
     */
    @PostMapping
    public ResponseEntity<IbanResponse> validateAndSaveIban(@Valid @RequestBody IbanRequest request) {
        IbanResponse response = buildResponse(request.iban());

        // Persist to PostgreSQL via Spring Data JPA.
        // ibanRepository.save() ≈ prisma.iban.create({...}) or db.Exec("INSERT ...")
        ibanRepository.save(new Iban(
                response.iban(),
                response.bankName(),
                response.bankIdentifier(),
                response.valid(),
                response.validationMethod()));

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/ibans — List all saved IBANs.
     *
     * @GetMapping marks this as a GET handler.
     *             ≈ Express: router.get('/', async (req, res) => { ... })
     *
     *             ibanRepository.findAll() ≈ prisma.iban.findMany() — Spring Data
     *             JPA
     *             auto-generates the SQL query from the method name.
     *
     *             .stream().map(...).toList() ≈ ibans.map(iban => ({...})) in
     *             TypeScript.
     *             We map the JPA entity to a DTO record so we control exactly which
     *             fields
     *             are included in the JSON response.
     */
    @GetMapping
    public ResponseEntity<List<IbanListEntry>> getAllIbans() {
        List<IbanListEntry> entries = ibanRepository.findAll().stream()
                .map(iban -> new IbanListEntry(
                        iban.getId(),
                        iban.getIban(),
                        iban.getBankName(),
                        iban.getBankIdentifier(),
                        iban.isValid(),
                        iban.getValidationMethod()))
                .toList();

        return ResponseEntity.ok(entries);
    }

    // ── Private helpers ──

    /**
     * Shared validation logic used by both validate and validateAndSave.
     * Runs local Mod-97 check first, then falls back to the external API
     * if the IBAN is valid but the bank name couldn't be resolved locally.
     */
    private IbanResponse buildResponse(String rawIban) {
        ValidationResult result = validationService.validate(rawIban);

        String bankName = result.bankName();
        String validationMethod = result.validationMethod();

        // Fallback to external API if local validation passed but no bank name was
        // resolved (i.e. the BLZ is not in our local KNOWN_BANKS map)
        if (result.valid() && bankName == null) {
            var external = externalApiService.validate(result.iban());
            if (external != null && external.bankName() != null) {
                bankName = external.bankName();
                validationMethod = "external";
            }
        }

        return new IbanResponse(
                result.valid(),
                result.iban(),
                bankName,
                result.bankIdentifier(),
                validationMethod);
    }
}
