package de.nicograef.lexiban.model;

/** Immutable result of an IBAN validation. Reason is null when valid. */
public record ValidationResult(boolean valid, String iban, String bankName, String reason) {}
