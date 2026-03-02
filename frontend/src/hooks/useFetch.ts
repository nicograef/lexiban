import { useCallback, useEffect, useState } from 'react'

interface UseFetchResult<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => void
}

/**
 * Generic data-fetching hook.
 * Calls `fetcher` on mount and whenever `deps` change.
 * Provides `reload()` for manual re-fetching.
 */
export function useFetch<T>(
  fetcher: () => Promise<T>,
  deps: readonly unknown[] = [],
): UseFetchResult<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = useCallback(() => {
    setReloadKey((k) => k + 1)
  }, [])

  useEffect(() => {
    let cancelled = false

    const doFetch = async () => {
      setLoading(true)
      setError(null)
      try {
        const result = await fetcher()
        if (!cancelled) {
          setData(result)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Unbekannter Fehler')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void doFetch()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadKey, ...deps])

  return { data, loading, error, reload }
}
