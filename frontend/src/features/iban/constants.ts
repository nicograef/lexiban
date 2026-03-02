/**
 * Expected IBAN lengths for 18 major SEPA countries.
 * Mirrors the backend's COUNTRY_LENGTHS map.
 *
 * @see docs/iban.md — IBAN-Aufbau und Länderlängen
 */
export const COUNTRY_LENGTHS: Record<string, number> = {
  DE: 22,
  AT: 20,
  CH: 21,
  LI: 21,
  LU: 20,
  BE: 16,
  NL: 18,
  FR: 27,
  IT: 27,
  ES: 24,
  PT: 25,
  IE: 22,
  GB: 22,
  SE: 24,
  DK: 18,
  FI: 18,
  NO: 15,
  MT: 31,
}

/**
 * Returns the expected IBAN length for a given country code, or null if unknown.
 */
export function getExpectedLength(countryCode: string): number | null {
  return COUNTRY_LENGTHS[countryCode.toUpperCase()] ?? null
}
