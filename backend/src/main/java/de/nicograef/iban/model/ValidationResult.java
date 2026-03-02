package de.nicograef.iban.model;

/**
 * Immutable result of an IBAN validation.
 *
 * Used across all layers: validators produce it, the orchestrator (IbanService)
 * passes it through, and the controller maps it to an HTTP response.
 *
 * The reason field provides a human-readable (German) explanation for why
 * the IBAN is invalid — null when the IBAN is valid.
 */
public record ValidationResult(
                boolean valid,
                String iban,
                String bankName,
                String reason) {
}
