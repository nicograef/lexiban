import { Badge } from '@/components/ui/badge'

import { IbanListEntry } from './types'
import { formatIban } from './utils'

export interface IbanListProps {
  loading: boolean
  error: string | null
  ibans: IbanListEntry[] | null
}

function IbanListItem({ entry }: { entry: IbanListEntry }) {
  return (
    <li className="flex items-center justify-between rounded-md border bg-card p-3 transition-colors hover:bg-muted/50 animate-slide-in">
      <div className="min-w-0">
        <p className="truncate font-mono text-sm tracking-wide">
          {formatIban(entry.iban)}
        </p>
        {entry.bankName && (
          <p className="mt-0.5 text-xs text-muted-foreground">
            {entry.bankName}
          </p>
        )}
        {entry.reason && (
          <p className="mt-0.5 text-xs text-muted-foreground italic">
            {entry.reason}
          </p>
        )}
      </div>
      <Badge
        className="ml-3 shrink-0"
        variant={entry.valid ? 'success' : 'destructive'}
      >
        {entry.valid ? 'gültig' : 'ungültig'}
      </Badge>
    </li>
  )
}

export function IbanList({ ibans, loading, error }: IbanListProps) {
  if (loading) {
    return (
      <p className="text-muted-foreground text-sm" role="status">
        Lade gespeicherte IBANs...
      </p>
    )
  }

  if (error) {
    return (
      <p className="text-destructive text-sm" role="alert">
        {error}
      </p>
    )
  }

  if (!ibans || ibans.length === 0) {
    return (
      <p className="text-muted-foreground text-sm">
        Noch keine IBANs gespeichert.
      </p>
    )
  }

  return (
    <section aria-label="Gespeicherte IBANs">
      <h3 className="mb-3 text-sm font-semibold tracking-tight">
        Gespeicherte IBANs
        <span className="ml-2 text-xs font-normal text-muted-foreground">
          ({ibans.length})
        </span>
      </h3>
      <ul className="max-h-100 space-y-2 overflow-y-auto pr-1" role="list">
        {ibans.map((entry) => (
          <IbanListItem key={entry.iban} entry={entry} />
        ))}
      </ul>
    </section>
  )
}
