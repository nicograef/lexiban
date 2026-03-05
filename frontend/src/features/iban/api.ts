import { z } from 'zod'

import { cleanIban } from '@/features/iban/utils'

import { type ValidationResult, ValidationResultSchema } from './types'

// path for API caSchemalls. In dev, Vite/nginx proxies /api/* to the backend.
// In production, the nginx reverse proxy routes /api/* to backend:8080.
// This means no absolute URLs needed — just relative paths.
const API_BASE = '/api/ibans'

/**
 * POST /api/ibans — Validate and save IBAN.
 * The IBAN is cleaned (non-alphanumeric chars removed) before sending.
 */
export async function validateIban(rawIban: string): Promise<ValidationResult> {
  const response = await fetch(API_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ iban: cleanIban(rawIban) }),
  })

  if (response.ok) {
    const json: unknown = await response.json()
    return ValidationResultSchema.parse(json)
  }

  throw new Error(`Validation failed: ${response.status.toString()}`)
}

/**
 * GET /api/ibans — List all saved IBANs.
 */
export async function getAllIbans(): Promise<ValidationResult[]> {
  const response = await fetch(API_BASE)
  if (!response.ok) {
    throw new Error(`Fetch failed: ${response.status.toString()}`)
  }
  const json: unknown = await response.json()
  return z.array(ValidationResultSchema).parse(json)
}
