import { CheckIcon, Loader2Icon, XIcon } from 'lucide-react'
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
import { cn } from '@/lib/utils'

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
    <p
      className={`text-xs ${color}`}
      data-testid="char-counter"
      id="iban-counter"
      aria-live="polite"
    >
      {currentLength}
      {expectedLength
        ? ` / ${expectedLength.toString()} Zeichen (${countryCode})`
        : ' Zeichen'}
    </p>
  )
}

/** Shows the validation result (success or failure). */
function ValidationResult({ result }: { result: IbanValidationResponse }) {
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

  const handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (input.trim() && !validation.loading) {
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
        <form onSubmit={handleSubmit} className="space-y-2" role="search">
          <label htmlFor="iban-input" className="sr-only">
            IBAN eingeben
          </label>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Input
                id="iban-input"
                type="text"
                value={input}
                onChange={handleInputChange}
                placeholder="DE89 3704 0044 0532 0130 00"
                className="font-mono text-lg tracking-wider pr-8"
                maxLength={42}
                autoFocus
                aria-describedby="iban-counter iban-error"
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
              type="submit"
              disabled={
                validation.loading ||
                !input.trim() ||
                !!validation.result ||
                !!validation.error
              }
              className={cn(
                'select-none transition-colors duration-300',
                validation.result?.valid === true &&
                  'bg-success text-white hover:bg-success/90',
                (validation.result?.valid === false || validation.error) &&
                  'bg-destructive text-white hover:bg-destructive/90',
              )}
            >
              {validation.loading ? (
                <>
                  <Loader2Icon className="size-4 animate-spin" />
                  Prüfe…
                </>
              ) : validation.result?.valid === true ? (
                <>
                  <CheckIcon className="size-4" />
                  Gültig
                </>
              ) : validation.result?.valid === false || validation.error ? (
                <>
                  <XIcon className="size-4" />
                  Ungültig
                </>
              ) : (
                'IBAN Prüfen'
              )}
            </Button>
          </div>
          {!validation.result && !validation.error && (
            <CharCounter
              currentLength={currentLength}
              expectedLength={expectedLength}
              countryCode={countryCode}
            />
          )}
        </form>

        {validation.error && (
          <p
            id="iban-error"
            role="alert"
            className="rounded-lg bg-destructive/10 p-3 text-destructive text-sm ring-1 ring-destructive/20"
          >
            {validation.error}
          </p>
        )}

        {validation.result && <ValidationResult result={validation.result} />}
      </CardContent>
    </Card>
  )
}
