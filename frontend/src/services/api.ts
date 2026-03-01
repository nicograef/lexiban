import { cleanIban } from '@/lib/utils'

// Base path for API calls. In dev, Vite/nginx proxies /api/* to the backend.
// In production, the nginx reverse proxy routes /api/* to backend:8080.
// This means no absolute URLs needed — just relative paths.
const API_BASE = '/api/ibans'

// Response types matching the Java Records (DTOs) defined in IbanController.java.
// IbanResponse        ↔ IbanController.IbanResponse record
// IbanListEntry       ↔ IbanController.IbanListEntry record
export interface IbanValidationResponse {
  valid: boolean
  iban: string
  bankName: string | null
  bankIdentifier: string | null
  validationMethod: string
  reason: string | null
}

export interface IbanListEntry {
  id: number
  iban: string
  bankName: string | null
  bankIdentifier: string | null
  valid: boolean
  validationMethod: string
}

/**
 * POST /api/ibans — Validate and save IBAN.
 * Calls IbanController.validateAndSaveIban() on the backend.
 * The IBAN is cleaned (non-alphanumeric chars removed) before sending.
 * Every IBAN is saved regardless of validity.
 */
export async function validateIban(
  rawIban: string,
): Promise<IbanValidationResponse> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ iban: cleanIban(rawIban) }),
  })
  if (!response.ok) {
    throw new Error(`Validation failed: ${response.status.toString()}`)
  }
  return (await response.json()) as IbanValidationResponse
}

/**
 * GET /api/ibans — List all saved IBANs.
 * Calls IbanController.getAllIbans() on the backend.
 */
export async function getAllIbans(): Promise<IbanListEntry[]> {
  const response = await fetch(API_BASE)
  if (!response.ok) {
    throw new Error(`Fetch failed: ${response.status.toString()}`)
  }
  return (await response.json()) as IbanListEntry[]
}
