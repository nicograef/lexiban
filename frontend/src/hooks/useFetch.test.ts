import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { useFetch } from './useFetch'

describe('useFetch', () => {
  it('starts in loading state', () => {
    const fetcher = vi.fn(
      () =>
        new Promise<string>(() => {
          /* never resolves */
        }),
    )
    const { result } = renderHook(() => useFetch(fetcher))

    expect(result.current.loading).toBe(true)
    expect(result.current.data).toBeNull()
    expect(result.current.error).toBeNull()
  })

  it('sets data on successful fetch', async () => {
    const fetcher = vi.fn(() => Promise.resolve('test-data'))
    const { result } = renderHook(() => useFetch(fetcher))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.data).toBe('test-data')
    expect(result.current.error).toBeNull()
  })

  it('sets error on failed fetch', async () => {
    const fetcher = vi.fn(() => Promise.reject(new Error('fetch failed')))
    const { result } = renderHook(() => useFetch(fetcher))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.data).toBeNull()
    expect(result.current.error).toBe('fetch failed')
  })

  it('re-fetches when deps change', async () => {
    let callCount = 0
    const fetcher = vi.fn(() =>
      Promise.resolve(`call-${(++callCount).toString()}`),
    )

    const { result, rerender } = renderHook(
      ({ dep }: { dep: number }) => useFetch(fetcher, [dep]),
      { initialProps: { dep: 1 } },
    )

    await waitFor(() => {
      expect(result.current.data).toBe('call-1')
    })

    rerender({ dep: 2 })

    await waitFor(() => {
      expect(result.current.data).toBe('call-2')
    })

    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('re-fetches on reload()', async () => {
    let callCount = 0
    const fetcher = vi.fn(() =>
      Promise.resolve(`call-${(++callCount).toString()}`),
    )
    const { result } = renderHook(() => useFetch(fetcher))

    await waitFor(() => {
      expect(result.current.data).toBe('call-1')
    })

    result.current.reload()

    await waitFor(() => {
      expect(result.current.data).toBe('call-2')
    })
  })
})
