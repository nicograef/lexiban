import './index.css'

import { useState } from 'react'

import { ThemeToggle } from '@/components/ThemeToggle'
import { IbanInput } from '@/features/iban/IbanInput'
import { IbanList } from '@/features/iban/IbanList'

export default function App() {
  const [refreshKey, setRefreshKey] = useState(0)

  return (
    <div className="min-h-screen bg-background">
      <ThemeToggle />
      <div className="mx-auto max-w-2xl px-4 py-8 space-y-6">
        <img src="/lexiban.svg" alt="Lexiban Logo" />
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
