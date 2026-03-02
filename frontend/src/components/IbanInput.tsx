import { XIcon } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { cleanIban, formatIban, getExpectedLength } from '@/lib/utils'
import { type IbanValidationResponse, validateIban } from '@/services/api'

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
        <CardTitle>
          IBAN Validierung für Selbstständige und kleine Unternehmen.
        </CardTitle>
        <CardDescription>
          Unterstützt alle IBAN-Länder und Formate. Leerzeichen und Trennzeichen
          werden automatisch formatiert. Gib einfach deine IBAN ein.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex gap-2">
            <div className="relative flex-1">
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
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  aria-label="Eingabe löschen"
                >
                  <XIcon className="size-4" />
                </button>
              )}
            </div>
            <Button
              onClick={() => void handleValidate()}
              disabled={validation.loading || !input.trim()}
              className="cursor-pointer select-none"
            >
              {validation.loading ? 'Prüfe...' : 'IBAN Prüfen'}
            </Button>
          </div>
          {currentLength > 0 && (
            <p
              className={`text-xs ${
                expectedLength && currentLength === expectedLength
                  ? 'text-success'
                  : expectedLength && currentLength > expectedLength
                    ? 'text-destructive'
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

        {validation.error && (
          <div className="rounded-lg bg-destructive/10 p-3 text-destructive text-sm ring-1 ring-destructive/20">
            {validation.error}
          </div>
        )}

        {validation.result && (
          <Card
            className={
              validation.result.valid
                ? 'ring-success/30 bg-success/5'
                : 'ring-destructive/30 bg-destructive/5'
            }
          >
            <CardContent className="space-y-1">
              <p className="font-medium">
                {validation.result.valid ? '✓ IBAN gültig' : '✗ IBAN ungültig'}
              </p>
              {!validation.result.valid && validation.result.reason && (
                <p className="text-sm text-destructive">
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
