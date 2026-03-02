import { z } from 'zod'

/**
 * Zod schemas for IBAN API responses.
 * These mirror the Java Records (DTOs) defined in IbanController.java
 * and provide runtime validation of API responses.
 *
 * IbanValidationResponseSchema ↔ IbanController.IbanResponse record
 * IbanListEntrySchema          ↔ IbanController.IbanListEntry record
 */

export const IbanValidationResponseSchema = z.object({
  valid: z.boolean(),
  iban: z.string(),
  bankName: z.string().nullable(),
  reason: z.string().nullable(),
})

export type IbanValidationResponse = z.infer<
  typeof IbanValidationResponseSchema
>

export const IbanListEntrySchema = z.object({
  iban: z.string(),
  bankName: z.string().nullable(),
  valid: z.boolean(),
  reason: z.string().nullable(),
})

export type IbanListEntry = z.infer<typeof IbanListEntrySchema>
