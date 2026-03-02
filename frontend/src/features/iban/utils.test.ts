import { describe, expect, it } from 'vitest'

import { COUNTRY_LENGTHS, getExpectedLength } from './constants'
import { cleanIban, formatIban } from './utils'

describe('formatIban', () => {
  it('groups a clean IBAN into 4-char blocks', () => {
    expect(formatIban('DE89370400440532013000')).toBe(
      'DE89 3704 0044 0532 0130 00',
    )
  })

  it('removes spaces and reformats', () => {
    expect(formatIban('DE89 3704 0044')).toBe('DE89 3704 0044')
  })

  it('converts lowercase to uppercase', () => {
    expect(formatIban('de89370400440532013000')).toBe(
      'DE89 3704 0044 0532 0130 00',
    )
  })

  it('strips hyphens, dots, and special characters', () => {
    expect(formatIban('DE89-3704.0044/0532')).toBe('DE89 3704 0044 0532')
  })

  it('returns empty string for empty input', () => {
    expect(formatIban('')).toBe('')
  })

  it('handles input shorter than 4 chars', () => {
    expect(formatIban('DE8')).toBe('DE8')
  })

  it('handles exactly 4 chars without trailing space', () => {
    expect(formatIban('DE89')).toBe('DE89')
  })
})

describe('cleanIban', () => {
  it('removes spaces', () => {
    expect(cleanIban('DE89 3704 0044')).toBe('DE8937040044')
  })

  it('removes hyphens and dots', () => {
    expect(cleanIban('DE89-3704.0044')).toBe('DE8937040044')
  })

  it('converts to uppercase', () => {
    expect(cleanIban('de89gb')).toBe('DE89GB')
  })

  it('strips all non-alphanumeric characters', () => {
    expect(cleanIban('DE89!@#$%^&*()')).toBe('DE89')
  })

  it('returns empty string for empty input', () => {
    expect(cleanIban('')).toBe('')
  })
})

describe('getExpectedLength', () => {
  it('returns 22 for DE', () => {
    expect(getExpectedLength('DE')).toBe(22)
  })

  it('returns 20 for AT', () => {
    expect(getExpectedLength('AT')).toBe(20)
  })

  it('returns 15 for NO (shortest in Europe)', () => {
    expect(getExpectedLength('NO')).toBe(15)
  })

  it('returns 31 for MT (longest in Europe)', () => {
    expect(getExpectedLength('MT')).toBe(31)
  })

  it('is case-insensitive', () => {
    expect(getExpectedLength('de')).toBe(22)
  })

  it('returns null for unknown country', () => {
    expect(getExpectedLength('XY')).toBeNull()
  })
})

describe('COUNTRY_LENGTHS', () => {
  it('contains 18 SEPA countries', () => {
    expect(Object.keys(COUNTRY_LENGTHS)).toHaveLength(18)
  })
})
