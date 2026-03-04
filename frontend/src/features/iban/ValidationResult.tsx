import { cn } from '@/lib/utils'

import type { IbanValidationResponse } from './types'

/** Shows the validation result (success or failure). */
export function ValidationResult({
  result,
}: {
  result: IbanValidationResponse
}) {
  const isValid = result.valid

  return (
    <div
      className={cn(
        'animate-fade-in flex items-baseline gap-2 text-sm',
        isValid ? 'text-success' : 'text-destructive',
      )}
    >
      <span>{isValid ? '✓ Gültig!' : '✗ Ungültig'}</span>
      <span className="font-bold text-foreground">
        {isValid ? result.bankName : result.reason}
      </span>
    </div>
  )
}
