import { useState } from 'react'

import { useFetch } from '@/hooks/useFetch'

import { getAllIbans, validateIban } from './api'
import type { IbanListEntry, IbanValidationResponse } from './types'

/**
 * Hook to fetch all saved IBANs.
 */
export function useIbanList() {
  return useFetch<IbanListEntry[]>(getAllIbans, [])
}

export interface ValidationState {
  loading: boolean
  result: IbanValidationResponse | null
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
export function useIbanValidation(onSaved?: () => void) {
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
      onSaved?.()
    } catch {
      setValidation({
        loading: false,
        result: null,
        error: 'Validierung fehlgeschlagen',
      })
    }
  }

  return { validation, validate, reset } as const
}
