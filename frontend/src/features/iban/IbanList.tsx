import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

import { useIbanList } from './hooks'
import { formatIban } from './utils'

export function IbanList({ refreshKey }: { refreshKey?: number }) {
  const { data: ibans, loading, error } = useIbanList(refreshKey)

  if (loading) {
    return (
      <p className="text-muted-foreground text-sm">
        Lade gespeicherte IBANs...
      </p>
    )
  }

  if (error) {
    return <p className="text-destructive text-sm">{error}</p>
  }

  if (!ibans || ibans.length === 0) {
    return (
      <p className="text-muted-foreground text-sm">
        Noch keine IBANs gespeichert.
      </p>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Gespeicherte IBANs</CardTitle>
        <CardDescription>
          {ibans.length.toString()} {ibans.length === 1 ? 'IBAN' : 'IBANs'}{' '}
          gespeichert
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-2">
        {ibans.map((entry) => (
          <div
            key={entry.iban}
            className="flex items-center justify-between rounded-lg border p-3"
          >
            <div>
              <p className="font-mono text-sm">{formatIban(entry.iban)}</p>
              {entry.bankName && (
                <p className="text-xs text-muted-foreground">
                  {entry.bankName}
                </p>
              )}
            </div>
            <Badge variant={entry.valid ? 'success' : 'destructive'}>
              {entry.valid ? 'gültig' : 'ungültig'}
            </Badge>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
