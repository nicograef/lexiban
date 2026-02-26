import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { IbanInput } from '@/components/IbanInput'

describe('IbanInput', () => {
  it('renders the input field', () => {
    render(<IbanInput />)
    expect(
      screen.getByPlaceholderText('DE89 3704 0044 0532 0130 00'),
    ).toBeInTheDocument()
  })

  it('renders validate button', () => {
    render(<IbanInput />)
    expect(screen.getByText('Validieren')).toBeInTheDocument()
  })

  it('renders save button', () => {
    render(<IbanInput />)
    expect(screen.getByText('Validieren & Speichern')).toBeInTheDocument()
  })
})
