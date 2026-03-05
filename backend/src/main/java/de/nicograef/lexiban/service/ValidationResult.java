package de.nicograef.lexiban.service;

/** Immutable result of an IBAN validation. Reason is null when valid. */
public record ValidationResult(boolean valid, String iban, String bankName, String reason) {}
