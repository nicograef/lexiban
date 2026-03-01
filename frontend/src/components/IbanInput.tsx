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
import { cleanIban, formatIban, getExpectedLength } from '@/lib/utils'
import {
  getAllIbans,
  type IbanListEntry,
  type IbanValidationResponse,
  validateIban,
} from '@/services/api'

interface ValidationState {
  loading: boolean
  result: IbanValidationResponse | null
  error: string | null
}

export function IbanInput({ onSaved }: { onSaved?: () => void }) {
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

  const handleClear = () => {
    setInput('')
    setValidation({ loading: false, result: null, error: null })
  }

  const handleValidate = async () => {
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

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && input.trim() && !validation.loading) {
      void handleValidate()
    }
  }

  // Character counter: show current length vs. expected length for the detected country
  const cleaned = cleanIban(input)
  const countryCode = cleaned.length >= 2 ? cleaned.substring(0, 2) : ''
  const expectedLength = countryCode ? getExpectedLength(countryCode) : null
  const currentLength = cleaned.length

  return (
    <Card>
      <CardHeader>
        <CardTitle>IBAN prüfen</CardTitle>
        <CardDescription>
          IBAN eingeben — unterstützt alle IBAN-Länder (DE, AT, CH, GB, FR,
          ...). Leerzeichen und Trennzeichen werden automatisch formatiert.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="iban-input">IBAN</Label>
          <div className="relative">
            <Input
              id="iban-input"
              type="text"
              value={input}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              placeholder="DE89 3704 0044 0532 0130 00"
              className="font-mono text-lg tracking-wider pr-8"
              maxLength={42}
              autoFocus
            />
            {input && (
              <button
                type="button"
                onClick={handleClear}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground text-sm"
                aria-label="Eingabe löschen"
              >
                ✕
              </button>
            )}
          </div>
          {currentLength > 0 && (
            <p
              className={`text-xs ${
                expectedLength && currentLength === expectedLength
                  ? 'text-green-600'
                  : expectedLength && currentLength > expectedLength
                    ? 'text-red-500'
                    : 'text-muted-foreground'
              }`}
              data-testid="char-counter"
            >
              {currentLength}
              {expectedLength
                ? ` / ${expectedLength.toString()} Zeichen (${countryCode})`
                : ' Zeichen'}
            </p>
          )}
        </div>

        <Button
          onClick={() => void handleValidate()}
          disabled={validation.loading || !input.trim()}
        >
          {validation.loading ? 'Prüfe...' : 'Prüfen'}
        </Button>

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
              {!validation.result.valid && validation.result.reason && (
                <p className="text-sm text-red-700">
                  {validation.result.reason}
                </p>
              )}
              <p className="text-sm font-mono">{validation.result.iban}</p>
              {validation.result.bankName && (
                <p className="text-sm">Bank: {validation.result.bankName}</p>
              )}
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  )
}

export function IbanList({ refreshKey }: { refreshKey?: number }) {
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
            <Badge variant={entry.valid ? 'default' : 'destructive'}>
              {entry.valid ? 'gültig' : 'ungültig'}
            </Badge>
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
