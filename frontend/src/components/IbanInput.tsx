import { useCallback, useEffect, useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { formatIban } from '@/lib/utils'
import {
  getAllIbans,
  type IbanListEntry,
  type IbanValidationResponse,
  validateAndSaveIban,
  validateIban,
} from '@/services/api'

interface ValidationState {
  loading: boolean
  result: IbanValidationResponse | null
  error: string | null
}

export function IbanInput() {
  const [input, setInput] = useState('')
  const [validation, setValidation] = useState<ValidationState>({
    loading: false,
    result: null,
    error: null,
  })

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatIban(e.target.value)
    setInput(formatted)
    // Clear previous result when input changes
    setValidation({ loading: false, result: null, error: null })
  }

  const handleValidate = async () => {
    if (!input.trim()) return
    setValidation({ loading: true, result: null, error: null })
    try {
      const result = await validateIban(input)
      setValidation({ loading: false, result, error: null })
    } catch {
      setValidation({
        loading: false,
        result: null,
        error: 'Validierung fehlgeschlagen',
      })
    }
  }

  const handleSave = async () => {
    if (!input.trim()) return
    setValidation({ loading: true, result: null, error: null })
    try {
      const result = await validateAndSaveIban(input)
      setValidation({ loading: false, result, error: null })
    } catch {
      setValidation({
        loading: false,
        result: null,
        error: 'Speichern fehlgeschlagen',
      })
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>IBAN prüfen</CardTitle>
        <CardDescription>
          IBAN eingeben — Leerzeichen und Trennzeichen werden automatisch
          formatiert.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="iban-input">IBAN</Label>
          <Input
            id="iban-input"
            type="text"
            value={input}
            onChange={handleInputChange}
            placeholder="DE89 3704 0044 0532 0130 00"
            className="font-mono text-lg tracking-wider"
          />
        </div>

        <div className="flex gap-2">
          <Button
            onClick={() => void handleValidate()}
            disabled={validation.loading || !input.trim()}
          >
            {validation.loading ? 'Prüfe...' : 'Validieren'}
          </Button>
          <Button
            variant="secondary"
            onClick={() => void handleSave()}
            disabled={validation.loading || !input.trim()}
          >
            {validation.loading ? 'Speichere...' : 'Validieren & Speichern'}
          </Button>
        </div>

        {validation.error && (
          <div className="rounded-lg bg-red-50 border border-red-200 p-3 text-destructive text-sm">
            {validation.error}
          </div>
        )}

        {validation.result && (
          <Card
            className={
              validation.result.valid
                ? 'border-green-300 bg-green-50'
                : 'border-red-300 bg-red-50'
            }
          >
            <CardContent className="space-y-1 pt-4">
              <p className="font-medium">
                {validation.result.valid ? '✓ IBAN gültig' : '✗ IBAN ungültig'}
              </p>
              <p className="text-sm font-mono">{validation.result.iban}</p>
              {validation.result.bankName && (
                <p className="text-sm">Bank: {validation.result.bankName}</p>
              )}
              {validation.result.bankIdentifier && (
                <p className="text-sm text-muted-foreground">
                  BLZ: {validation.result.bankIdentifier}
                </p>
              )}
              <p className="text-xs text-muted-foreground">
                Validierung: {validation.result.validationMethod}
              </p>
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  )
}

export function IbanList() {
  const [ibans, setIbans] = useState<IbanListEntry[]>([])
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
  }, [fetchIbans])

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
            key={entry.id}
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
            <Badge variant={entry.valid ? 'default' : 'destructive'}>
              {entry.valid ? 'gültig' : 'ungültig'}
            </Badge>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
