import { useFetch } from '@/hooks/useFetch'

import { getAllIbans } from './api'
import type { IbanListEntry } from './types'

/**
 * Hook to fetch all saved IBANs.
 * Re-fetches whenever `refreshKey` changes (e.g. after a new IBAN is saved).
 */
export function useIbanList(refreshKey?: number) {
  return useFetch<IbanListEntry[]>(getAllIbans, [refreshKey])
}
