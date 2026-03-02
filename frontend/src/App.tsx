import './index.css'

import { useState } from 'react'

import { IbanInput, IbanList } from '@/components/IbanInput'
import { ThemeToggle } from '@/components/ThemeToggle'

export default function App() {
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="min-h-screen bg-background">
      <ThemeToggle />
      <div className="mx-auto max-w-2xl px-4 py-8 space-y-6">
        <div className="flex flex-col items-center justify-between">
          <img src="/lexiban.svg" alt="Lexiban Logo" />
          <h1 className="text-2xl font-bold text-foreground">
            IBAN Validierung für Selbstständige und kleine Unternehmen.
          </h1>
        </div>
        <IbanInput
          onSaved={() => {
            setRefreshKey((k) => k + 1)
          }}
        />
        <IbanList refreshKey={refreshKey} />
      </div>
    </div>
  )
}
