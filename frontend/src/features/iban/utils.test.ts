import { describe, expect, it } from 'vitest'

import { getExpectedLength } from './constants'
import { cleanIban, formatIban } from './utils'

describe('formatIban', () => {
  it('groups into 4-char blocks and uppercases', () => {
    expect(formatIban('de89370400440532013000')).toBe(
      'DE89 3704 0044 0532 0130 00',
    )
  })

  it('strips special characters before formatting', () => {
    expect(formatIban('DE89-3704.0044/0532')).toBe('DE89 3704 0044 0532')
  })

  it('handles empty and short input', () => {
    expect(formatIban('')).toBe('')
    expect(formatIban('DE8')).toBe('DE8')
    expect(formatIban('DE89')).toBe('DE89')
  })
})

describe('cleanIban', () => {
  it('strips non-alphanumeric chars and uppercases', () => {
    expect(cleanIban('de89 3704-0044.!@#')).toBe('DE8937040044')
    expect(cleanIban('')).toBe('')
  })
})

describe('getExpectedLength', () => {
  it('returns length for known country (case-insensitive)', () => {
    expect(getExpectedLength('DE')).toBe(22)
    expect(getExpectedLength('de')).toBe(22)
  })

  it('returns null for unknown country', () => {
    expect(getExpectedLength('XY')).toBeNull()
  })
})
