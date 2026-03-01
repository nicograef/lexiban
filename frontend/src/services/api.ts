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
  reason: string | null
}

export interface IbanListEntry {
  iban: string
  bankName: string | null
  valid: boolean
  reason: string | null
}

/**
 * POST /api/ibans — Validate and save IBAN.
 * Calls IbanController.validateAndSaveIban() on the backend.
 * The IBAN is cleaned (non-alphanumeric chars removed) before sending.
 *
 * HTTP status semantics:
 * - 200: structurally valid IBAN → response has valid=true/false.
 * - 400: structurally malformed input → response has the same shape
 *         (valid=false, iban, reason) so the UI can display it uniformly.
 * - Other errors: network failure etc. → thrown as Error.
 */
export async function validateIban(
  rawIban: string,
): Promise<IbanValidationResponse> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ iban: cleanIban(rawIban) }),
  })

  // Both 200 (semantic result) and 400 (structural format error) return
  // the same JSON shape { valid, iban, reason } — parse and return both.
  if (response.ok || response.status === 400) {
    return (await response.json()) as IbanValidationResponse
  }

  throw new Error(`Validation failed: ${response.status.toString()}`)
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
