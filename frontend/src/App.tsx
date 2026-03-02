import './index.css'

import { useState } from 'react'

import { IbanInput, IbanList } from '@/components/IbanInput'
import { ThemeToggle } from '@/components/ThemeToggle'

export default function App() {
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-2xl px-4 py-8 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-foreground">IBAN Validator</h1>
          <ThemeToggle />
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
