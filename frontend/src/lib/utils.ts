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
