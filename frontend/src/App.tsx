import './index.css'

import { useState } from 'react'

import { IbanInput } from '@/components/IbanInput'
import { IbanList } from '@/components/IbanList'
import { ThemeToggle } from '@/components/ThemeToggle'

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
