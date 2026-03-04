import { CheckIcon, Loader2Icon, XIcon } from 'lucide-react'
import { useRef, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

import { CharCounter } from './CharCounter'
import { getExpectedLength } from './constants'
import { useIbanValidation } from './hooks'
import { cleanIban, formatIban } from './utils'
import { ValidationResult } from './ValidationResult'

interface IbanInputProps {
  onSaved?: () => void
}

export function IbanInput({ onSaved }: IbanInputProps) {
  const [input, setInput] = useState('')
  const { validation, validate, reset } = useIbanValidation(onSaved)
  const inputRef = useRef<HTMLInputElement>(null)
  const cleaned = cleanIban(input)
  const countryCode = cleaned.length >= 2 ? cleaned.substring(0, 2) : ''
  const expectedLength = countryCode ? getExpectedLength(countryCode) : null
  const currentLength = cleaned.length

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const el = e.target
    const cursorPos = el.selectionStart ?? 0

    // Count alphanumeric chars before the cursor in the old (raw) value
    const rawBefore = el.value.slice(0, cursorPos)
    const lengthBefore = cleanIban(rawBefore).length

    const formatted = formatIban(el.value)
    setInput(formatted)
    reset()

    // Restore cursor: find position in formatted string where the same
    // number of alphanumeric chars have been passed
    let count = 0
    let newPos = 0
    for (let i = 0; i < formatted.length; i++) {
      if (/[a-zA-Z0-9]/.test(formatted.charAt(i))) count++
      if (count === lengthBefore) {
        newPos = i + 1
        break
      }
    }
    // If we deleted chars and lengthBefore is 0, cursor stays at 0
    if (lengthBefore === 0) newPos = 0

    requestAnimationFrame(() => {
      el.setSelectionRange(newPos, newPos)
    })
  }

  const handleClear = () => {
    setInput('')
    reset()
  }

  const handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (input.trim() && !validation.loading) {
      void validate(input)
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={handleSubmit} className="space-y-2" role="search">
        <label htmlFor="iban-input" className="sr-only">
          IBAN eingeben
        </label>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Input
              ref={inputRef}
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
    </div>
  )
}
