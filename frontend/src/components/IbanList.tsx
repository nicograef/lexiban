import { useCallback, useEffect, useState } from 'react'

import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatIban } from '@/lib/utils'
import { getAllIbans, type IbanListEntry } from '@/services/api'

const mockIbans: IbanListEntry[] = [
  {
    iban: 'DE89370400440532013000',
    bankName: 'Commerzbank',
    valid: true,
    reason: null,
  },
  {
    iban: 'GB29NWBK60161331926819',
    bankName: 'NatWest',
    valid: true,
    reason: null,
  },
  {
    iban: 'DE00123456780000000001',
    bankName: null,
    valid: false,
    reason: 'Prüfsumme ungültig (Modulo-97-Check fehlgeschlagen)',
  },
  {
    iban: 'XX12345678',
    bankName: null,
    valid: false,
    reason: 'Unbekannter Ländercode: XX',
  },
  {
    iban: 'AT611904300234573201',
    bankName: 'Erste Bank',
    valid: true,
    reason: null,
  },
]

export function IbanList({ refreshKey }: { refreshKey?: number }) {
  const [ibans, setIbans] = useState<IbanListEntry[]>(mockIbans)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchIbans = useCallback(async () => {
    try {
      setLoading(true)
      const data = await getAllIbans()
      setIbans(data)
      setError(null)
    } catch {
      setError('IBANs konnten nicht geladen werden')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchIbans()
  }, [fetchIbans, refreshKey])

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

  if (ibans.length === 0) {
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
