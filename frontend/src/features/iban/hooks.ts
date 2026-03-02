import { useFetch } from '@/hooks/useFetch'

import { getAllIbans } from './api'
import type { IbanListEntry } from './types'

/**
 * Hook to fetch all saved IBANs.
 */
export function useIbanList() {
  return useFetch<IbanListEntry[]>(getAllIbans, [])
}
