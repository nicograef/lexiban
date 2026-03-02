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

import { validateIban } from './api'
import { getExpectedLength } from './constants'
import type { IbanValidationResponse } from './types'
import { cleanIban, formatIban } from './utils'

interface IbanInputProps {
  onSaved?: () => void
}

interface ValidationState {
  loading: boolean
  result: IbanValidationResponse | null
  error: string | null
}

const INITIAL_VALIDATION: ValidationState = {
  loading: false,
  result: null,
  error: null,
}

/** Displays the current / expected character count with colour feedback. */
function CharCounter({
  currentLength,
  expectedLength,
  countryCode,
}: {
  currentLength: number
  expectedLength: number | null
  countryCode: string
}) {
  if (currentLength === 0) return null

  const color = getCounterColor(currentLength, expectedLength)

  return (
    <p className={`text-xs ${color}`} data-testid="char-counter">
      {currentLength}
      {expectedLength
        ? ` / ${expectedLength.toString()} Zeichen (${countryCode})`
        : ' Zeichen'}
    </p>
  )
}

/** Shows the validation result card (success or failure). */
function ValidationResult({ result }: { result: IbanValidationResponse }) {
  const isValid = result.valid

  return (
    <Card
      className={
        isValid
          ? 'ring-success/30 bg-success/5'
          : 'ring-destructive/30 bg-destructive/5'
      }
    >
      <CardContent className="space-y-1">
        <p className="font-medium">
          {isValid ? '✓ IBAN gültig' : '✗ IBAN ungültig'}
        </p>
        {!isValid && result.reason && (
          <p className="text-sm text-destructive">{result.reason}</p>
        )}
        {result.bankName && <p className="text-sm">Bank: {result.bankName}</p>}
      </CardContent>
    </Card>
  )
}

function getCounterColor(current: number, expected: number | null): string {
  if (expected && current === expected) return 'text-success'
  if (expected && current > expected) return 'text-destructive'
  return 'text-muted-foreground'
}

export function IbanInput({ onSaved }: IbanInputProps) {
  const [input, setInput] = useState('')
  const [validation, setValidation] =
    useState<ValidationState>(INITIAL_VALIDATION)

  const cleaned = cleanIban(input)
  const countryCode = cleaned.length >= 2 ? cleaned.substring(0, 2) : ''
  const expectedLength = countryCode ? getExpectedLength(countryCode) : null
  const currentLength = cleaned.length

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInput(formatIban(e.target.value))
    setValidation(INITIAL_VALIDATION)
  }

  const handleClear = () => {
    setInput('')
    setValidation(INITIAL_VALIDATION)
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
          <CharCounter
            currentLength={currentLength}
            expectedLength={expectedLength}
            countryCode={countryCode}
          />
        </div>

        {validation.error && (
          <div className="rounded-lg bg-destructive/10 p-3 text-destructive text-sm ring-1 ring-destructive/20">
            {validation.error}
          </div>
        )}

        {validation.result && <ValidationResult result={validation.result} />}
      </CardContent>
    </Card>
  )
}
