import './index.css'

import { ThemeToggle } from '@/components/ThemeToggle'

import { IbanPage } from './features/iban/IbanPage'

export default function App() {
  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-end p-4">
        <ThemeToggle />
      </header>
      <main className="mx-auto max-w-2xl px-4 pb-12 space-y-6">
        <IbanPage />
      </main>
    </div>
  )
}
