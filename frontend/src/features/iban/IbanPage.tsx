import { useIbanList } from './hooks'
import { IbanInput } from './IbanInput'
import { IbanList } from './IbanList'

export function IbanPage() {
  const { data: ibans, loading, error, reload } = useIbanList()

  return (
    <>
      <img src="/lexiban.svg" alt="Lexiban Logo" />
      <IbanInput
        onSaved={() => {
          reload()
        }}
      />
      <IbanList ibans={ibans} loading={loading} error={error} />
    </>
  )
}
