import { useState } from 'react'

import { useFetch } from '@/hooks/useFetch'

import { getAllIbans, validateIban } from './api'
import type { ValidationResult } from './types'

/**
 * Hook to fetch all saved IBANs.
 */
export function useIbanList() {
  return useFetch<ValidationResult[]>(getAllIbans, [])
}

export interface ValidationState {
  loading: boolean
  result: ValidationResult | null
  error: string | null
}

const INITIAL_VALIDATION: ValidationState = {
  loading: false,
  result: null,
  error: null,
}

/**
 * Manages the IBAN validation lifecycle (idle → loading → result/error).
 * Delegates the actual API call to `validateIban`.
 */
export function useIbanValidation(onSaved: () => void) {
  const [validation, setValidation] =
    useState<ValidationState>(INITIAL_VALIDATION)

  const reset = () => {
    setValidation(INITIAL_VALIDATION)
  }

  const validate = async (input: string) => {
    if (!input.trim()) return
    setValidation({ loading: true, result: null, error: null })

    try {
      const result = await validateIban(input)
      setValidation({ loading: false, result, error: null })
      onSaved()
    } catch {
      setValidation({
        loading: false,
        result: null,
        error:
          'Validierung fehlgeschlagen. Überprüfe eine Eingabe und deine Netzwerkverbindung.',
      })
    }
  }

  return { validation, validate, reset } as const
}
