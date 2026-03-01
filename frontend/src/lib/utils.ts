import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Format an IBAN string into 4-character groups.
 * e.g. "DE89370400440532013000" → "DE89 3704 0044 0532 0130 00"
 */
export function formatIban(iban: string): string {
  const clean = iban.replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
  return clean.replace(/(.{4})/g, '$1 ').trim()
}

/**
 * Remove all non-alphanumeric characters from an IBAN string.
 */
export function cleanIban(iban: string): string {
  return iban.replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
}

/**
 * Expected IBAN lengths for 18 major SEPA countries (mirrors backend's COUNTRY_LENGTHS).
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
