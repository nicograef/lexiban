function getCounterColor(current: number, expected: number | null): string {
  if (expected && current === expected) return 'text-success'
  if (expected && current > expected) return 'text-destructive'
  return 'text-muted-foreground'
}

interface CharCounterProps {
  currentLength: number
  expectedLength: number | null
  countryCode: string
}

/** Displays the current / expected character count with colour feedback. */
export function CharCounter({
  currentLength,
  expectedLength,
  countryCode,
}: CharCounterProps) {
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
