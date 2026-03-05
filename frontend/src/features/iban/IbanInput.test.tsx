import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { IbanInput } from './IbanInput'

describe('IbanInput', () => {
  it('formats input into 4-char groups while typing', async () => {
    const user = userEvent.setup()
    render(<IbanInput onSaved={() => void 0} />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    await user.type(input, 'DE89370400440532013000')
    expect(input).toHaveValue('DE89 3704 0044 0532 0130 00')
  })

  it('shows character counter with country info', async () => {
    const user = userEvent.setup()
    render(<IbanInput onSaved={() => void 0} />)
    await user.type(
      screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00'),
      'DE89',
    )
    expect(screen.getByTestId('char-counter')).toHaveTextContent(
      '4 / 22 Zeichen (DE)',
    )
  })

  it('shows clear button that resets input', async () => {
    const user = userEvent.setup()
    render(<IbanInput onSaved={() => void 0} />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    expect(screen.queryByLabelText('Eingabe löschen')).not.toBeInTheDocument()
    await user.type(input, 'DE89')
    expect(screen.getByLabelText('Eingabe löschen')).toBeInTheDocument()
    await user.click(screen.getByLabelText('Eingabe löschen'))
    expect(input).toHaveValue('')
  })

  it('disables button when empty, enables with input', async () => {
    const user = userEvent.setup()
    render(<IbanInput onSaved={() => void 0} />)
    expect(screen.getByText('IBAN Prüfen')).toBeDisabled()
    await user.type(
      screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00'),
      'DE89',
    )
    expect(screen.getByText('IBAN Prüfen')).toBeEnabled()
  })
})
