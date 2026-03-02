import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { IbanInput } from './IbanInput'

describe('IbanInput', () => {
  it('renders the input field', () => {
    render(<IbanInput />)
    expect(
      screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00'),
    ).toBeInTheDocument()
  })

  it('renders validate button', () => {
    render(<IbanInput />)
    expect(screen.getByText('IBAN Prüfen')).toBeInTheDocument()
  })

  it('has autoFocus on the input', () => {
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    expect(input).toHaveFocus()
  })

  it('has maxLength of 42', () => {
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    expect(input).toHaveAttribute('maxLength', '42')
  })

  it('formats input into 4-char groups while typing', async () => {
    const user = userEvent.setup()
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    await user.type(input, 'DE89370400440532013000')
    expect(input).toHaveValue('DE89 3704 0044 0532 0130 00')
  })

  it('shows character counter after typing', async () => {
    const user = userEvent.setup()
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    await user.type(input, 'DE89')
    const counter = screen.getByTestId('char-counter')
    expect(counter).toHaveTextContent('4 / 22 Zeichen (DE)')
  })

  it('shows clear button when input has text', async () => {
    const user = userEvent.setup()
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    // Initially no clear button
    expect(screen.queryByLabelText('Eingabe löschen')).not.toBeInTheDocument()
    await user.type(input, 'DE89')
    expect(screen.getByLabelText('Eingabe löschen')).toBeInTheDocument()
  })

  it('clears input when clear button is clicked', async () => {
    const user = userEvent.setup()
    render(<IbanInput />)
    const input = screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00')
    await user.type(input, 'DE89')
    expect(input).toHaveValue('DE89')
    await user.click(screen.getByLabelText('Eingabe löschen'))
    expect(input).toHaveValue('')
  })

  it('disables button when input is empty', () => {
    render(<IbanInput />)
    expect(screen.getByText('IBAN Prüfen')).toBeDisabled()
  })

  it('enables button when input has text', async () => {
    const user = userEvent.setup()
    render(<IbanInput />)
    await user.type(
      screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00'),
      'DE89',
    )
    expect(screen.getByText('IBAN Prüfen')).toBeEnabled()
  })
})
