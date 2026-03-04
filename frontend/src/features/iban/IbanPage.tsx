import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

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
      <Card>
        <CardHeader>
          <CardTitle>
            IBAN Validierung für Selbstständige und kleine Unternehmen.
          </CardTitle>
          <CardDescription>
            Unterstützt alle IBAN-Länder und Formate. Leerzeichen und
            Trennzeichen werden automatisch formatiert. Gib einfach deine IBAN
            ein.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <IbanInput
            onSaved={() => {
              reload()
            }}
          />
        </CardContent>
      </Card>
      <IbanList ibans={ibans} loading={loading} error={error} />
    </>
  )
}
