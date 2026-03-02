import './index.css'

import { ThemeToggle } from '@/components/ThemeToggle'

import { IbanPage } from './features/iban/IbanPage'

export default function App() {
  return (
    <div className="min-h-screen bg-background">
      <ThemeToggle />
      <div className="mx-auto max-w-2xl px-4 py-8 space-y-6">
        <IbanPage />
      </div>
    </div>
  )
}
