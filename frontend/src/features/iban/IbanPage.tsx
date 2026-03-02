import { useIbanList } from './hooks'
import { IbanInput } from './IbanInput'
import { IbanList } from './IbanList'

export function IbanPage() {
  const { data: ibans, loading, error, reload } = useIbanList()

  return (
    <>
      <figure>
        <img src="/lexiban.svg" alt="Lexiban" className="max-w-1/2 h-auto" />
      </figure>
      <IbanInput
        onSaved={() => {
          reload()
        }}
      />
      <IbanList ibans={ibans} loading={loading} error={error} />
    </>
  )
}
