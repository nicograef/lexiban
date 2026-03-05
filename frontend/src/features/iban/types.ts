import { z } from 'zod'

export const ValidationResultSchema = z.object({
  iban: z.string(),
  valid: z.boolean(),
  bankName: z.string().nullable(),
  reason: z.string().nullable(),
})

export type ValidationResult = z.infer<typeof ValidationResultSchema>
