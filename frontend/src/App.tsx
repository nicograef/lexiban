import './index.css'

import { IbanInput, IbanList } from '@/components/IbanInput'

export default function App() {
  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-2xl px-4 py-8 space-y-6">
        <h1 className="text-3xl font-bold text-foreground">IBAN Validator</h1>
        <IbanInput />
        <IbanList />
      </div>
    </div>
  )
}
