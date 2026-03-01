# IBAN Validator — Technische Evaluation

> Evaluation des gesamten Projekts auf Basis der fachlichen Spezifikation in [iban.md](iban.md).
> Fokus: Korrektheit der Validierung, Code-Dokumentation, Frontend-Validierung und UX.
> Besonderer Schwerpunkt: **Internationale IBAN-Unterstützung** (89 Länder, 15–34 Zeichen).

---

## 1. Korrektheit der IBAN-Validierung

### 1.1 Mod-97-Algorithmus — korrekt ✓

Die Implementierung in `IbanValidationService.isValidMod97()` setzt den ISO-13616-Algorithmus korrekt um:

1. **Rearrangement**: Erste 4 Zeichen (Ländercode + Prüfziffern) ans Ende → ✓
2. **Buchstaben-Konvertierung**: `Character.getNumericValue()` liefert A=10, B=11, …, Z=35 → ✓
3. **BigInteger mod 97**: Korrekte Nutzung von `BigInteger` für 60+-stellige Zahlen → ✓
4. **Ergebnis = 1**: Prüfung auf Remainder 1 → ✓

Der Kern-Algorithmus funktioniert **für alle Länder identisch** — das ist laut ISO 13616 das Prinzip. Die Mod-97-Prüfung ist länderunabhängig und damit bereits international korrekt.

### 1.2 Probleme bei internationalen IBANs

| #      | Problem                                    | Schwere     | Details                                                                                                                                                                                                                                                                                                                                                     |
| ------ | ------------------------------------------ | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **P1** | `COUNTRY_LENGTHS` enthält nur `"DE"` → 22  | **Hoch**    | Laut iban.md variiert die Länge je Land erheblich: NO = 15, NL = 18, AT = 20, DE/GB = 22, ES = 24, FR/IT = 27, PL = 28, MT = 31. Aktuell werden **alle nicht-DE-IBANs ohne Längenprüfung** durchgelassen. Eine GB-IBAN mit 23 statt 22 Zeichen würde nur durch Mod-97 geprüft — das erkennt zwar oft Fehler, aber nicht zuverlässig bei Längenabweichungen. |
| **P2** | Keine allgemeine Mindestlänge              | **Mittel**  | Die kürzeste IBAN weltweit hat 15 Zeichen (Norwegen). IBANs < 5 Zeichen werden durch `iban.length() < 2` und Regex abgefangen, aber eine 4-Zeichen-Eingabe wie `"DE68"` würde in `isValidMod97()` zu `iban.substring(4)` → Leerstring → `BigInteger("")` → **NumberFormatException** führen.                                                                |
| **P3** | Keine Maximallänge (34 Zeichen)            | **Niedrig** | Laut ISO 13616 ist die IBAN auf maximal 34 Zeichen begrenzt. Aktuell wird keine Obergrenze geprüft. Ein String mit 50+ Zeichen würde den Mod-97-Check durchlaufen (meist als ungültig erkannt, aber unnötig).                                                                                                                                               |
| **P4** | Keine Prüfung, ob Pos. 3–4 Ziffern sind    | **Niedrig** | Laut Spezifikation müssen Positionen 3–4 **immer Ziffern** sein (Prüfziffern). `"DEAB..."` würde aktuell in den Mod-97-Check gehen. In der Praxis quasi unmöglich, dass so etwas als gültig durchgeht, aber streng nach ISO nicht korrekt.                                                                                                                  |
| **P5** | Keine Prüfung, ob Pos. 1–2 Buchstaben sind | **Niedrig** | Der Ländercode muss laut ISO 3166-1 aus 2 Buchstaben bestehen. `"12345..."` passiert aktuell den Regex-Check `[A-Z0-9]+` und wird nur durch Mod-97 geprüft.                                                                                                                                                                                                 |
| **P6** | BLZ-Extraktion nur für DE                  | **OK**      | Das ist korrekt so — BLZ ist ein deutsches Konzept. Andere Länder haben andere Bankidentifikations-Strukturen (Sort Code in GB, BC-Nummer in CH etc.). Die externe API über openiban.com übernimmt das für andere Länder.                                                                                                                                   |

### 1.3 Empfohlene Fixes (priorisiert)

```
Priorität 1 — COUNTRY_LENGTHS erweitern:
  Mindestens die gängigen SEPA-Länder aus iban.md hinzufügen:
  AT=20, CH=21, FR=27, GB=22, ES=24, IT=27, NL=18, PL=28, NO=15, MT=31

Priorität 2 — Strukturelle Vorbedingungen:
  - iban.length() >= 15 (kürzeste IBAN = Norwegen)
  - iban.length() <= 34 (ISO-Maximum)
  - Pos. 1–2 sind Buchstaben: Character.isLetter(iban.charAt(0))
  - Pos. 3–4 sind Ziffern: Character.isDigit(iban.charAt(2))

Priorität 3 — Defensiver Schutz in isValidMod97():
  - Guard clause: if (iban.length() < 5) return false
```

### 1.4 Normalisierung — korrekt ✓

`normalize()` entfernt Leerzeichen, Bindestriche und Punkte und konvertiert zu Uppercase. Das deckt alle in iban.md genannten Schreibweisen ab (DIN 5008 mit Leerzeichen, Bindestriche aus Copy-Paste, Punkte). Die Regex `[\\s\\-.]` ist korrekt.

### 1.5 Externe API als Fallback — korrekt ✓

Die Architektur ist sinnvoll: Erst lokal validieren (Mod-97), dann bei gültiger IBAN ohne Bankname die externe API befragen. Das funktioniert international — openiban.com kennt Banken aus vielen Ländern.

### 1.6 Testabdeckung — nur DE

Alle Tests in `IbanValidationServiceTest` verwenden ausschließlich deutsche IBANs. Es fehlen:

| Fehlender Test                                       | Warum wichtig                               |
| ---------------------------------------------------- | ------------------------------------------- |
| Gültige AT-IBAN (20 Zeichen)                         | Kürzeres Format, anderer BBAN-Aufbau        |
| Gültige GB-IBAN (22 Zeichen, mit Buchstaben im BBAN) | BBAN enthält Buchstaben (Sort Code aus BIC) |
| Gültige NO-IBAN (15 Zeichen)                         | Kürzeste europäische IBAN                   |
| Gültige FR-IBAN (27 Zeichen)                         | Enthält nationale Prüfziffer am Ende        |
| IBAN mit unbekanntem Ländercode                      | Sicherstellen, dass Mod-97 trotzdem greift  |
| IBAN mit 4 Zeichen                                   | Edge Case: `substring(4)` auf Leerstring    |
| IBAN mit 35+ Zeichen                                 | Über ISO-Maximum                            |

---

## 2. Code-Kommentare und JavaDoc

### 2.1 Gesamteindruck: Gut, aber ausbaufähig für Internationalisierung

Die bestehenden Kommentare sind **überdurchschnittlich gut** — insbesondere die TS/Vitest-Analogien in den Tests. Für ein Projekt, das internationale IBANs unterstützen soll, fehlen aber fachliche Kontextinformationen.

### 2.2 IbanValidationService — Verbesserungsvorschläge

| Stelle                    | Aktuell                                            | Vorschlag                                                                                                                                                                                                                                                                           |
| ------------------------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `KNOWN_BANKS`             | Kein Kommentar                                     | Hinzufügen: _"Demo subset of German BLZ → bank name mappings. In production, this would be loaded from the Bundesbank BLZ file (~3,600 entries). Only relevant for German IBANs — other countries use different bank identifier schemes (Sort Code in GB, BC-Nummer in CH, etc.)."_ |
| `COUNTRY_LENGTHS`         | Kein Kommentar                                     | Hinzufügen: _"Expected IBAN length per country code (ISO 13616). The shortest IBAN is Norway (15), the longest Malta (31), max possible is 34. Countries not listed here skip the length check — only Mod-97 is applied."_                                                          |
| `isValidMod97()`          | Kurzer Kommentar vorhanden                         | Erweitern um: Warum `Character.getNumericValue()` funktioniert (es liefert für A–Z die Werte 10–35, was exakt der ISO-7064-Spezifikation entspricht). Kurzer Hinweis auf die TS-Analogie: `"A".charCodeAt(0) - 55 // = 10`                                                          |
| `validate()`, Rückgabetyp | `bankName`/`bankIdentifier` können null sein       | `@Nullable`-Annotation oder Kommentar: _"bankName and bankIdentifier are null for non-DE IBANs or unknown German banks. The external API may resolve these as fallback."_                                                                                                           |
| BLZ-Extraktion            | `// Extract BLZ for German IBANs (positions 4–11)` | Fachlich korrekt wäre: _"positions 5–12 in human-readable notation (1-based), which is substring(4, 12) in 0-based Java indexing. See iban.md §4."_                                                                                                                                 |

### 2.3 ExternalIbanApiService — Verbesserungsvorschläge

| Stelle                | Vorschlag                                                                                                                                                                                                                      |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Query-Parameter       | Erklären: `getBIC=true` → liefert den BIC/SWIFT-Code der Bank mit. `validateBankCode=true` → prüft zusätzlich, ob die Bankleitzahl/der Bankcode im BBAN tatsächlich existiert (geht über Mod-97 hinaus).                       |
| `catch (Exception e)` | Kommentar, dass der breite Catch **bewusst** ist: Die externe API ist ein Best-Effort-Enrichment. Ein Fehler hier darf niemals die lokale Validierung blockieren. Analog: In Node.js würde man `.catch(() => null)` verwenden. |
| Null-Rückgabe         | Erklären: Null bedeutet "Service nicht verfügbar", nicht "IBAN ungültig". Der Controller prüft das und fällt auf die lokale Validierung zurück.                                                                                |

### 2.4 IbanController — Verbesserungsvorschläge

| Stelle                  | Vorschlag                                                                                                                                               |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `validateAndSaveIban()` | Nicht offensichtlich: **Auch ungültige IBANs werden gespeichert.** Das ist eine bewusste Entscheidung (Audit-Log / Historie). Sollte im JavaDoc stehen. |
| `buildResponse()`       | Der Kommentar ist gut. Ergänzen: _"For non-German IBANs, the external API is always called (since KNOWN_BANKS only contains German BLZ)."_              |

### 2.5 Tests — Sehr gute Kommentare ✓

Die Vitest/Jest-Analogien in `IbanValidationServiceTest` und `IbanControllerTest` sind exzellent für das Entwicklerprofil. Kein Verbesserungsbedarf bei den bestehenden Kommentaren.

---

## 3. Frontend-Validierung

### 3.1 Aktueller Stand: Keine Validierung vorhanden

Das Frontend macht aktuell:

- **Formatierung** beim Tippen: `formatIban()` bereinigt und gruppiert in 4er-Blöcke ✓
- **Bereinigung** vor API-Call: `cleanIban()` entfernt Nicht-Alphanumerisches ✓
- **Leereingabe-Schutz**: `!input.trim()` deaktiviert Buttons ✓
- **Validierung**: ✗ — Es gibt keine

Jede Eingabe, egal wie offensichtlich falsch, wird an den Server geschickt.

### 3.2 Empfohlene Frontend-Validierung (nach Aufwand sortiert)

| #      | Validierung                                             | Aufwand | Nutzen                                                        | Fachliche Grundlage (iban.md)                        |
| ------ | ------------------------------------------------------- | ------- | ------------------------------------------------------------- | ---------------------------------------------------- |
| **F1** | `maxLength={42}` auf Input (34 Zeichen + 8 Leerzeichen) | Trivial | Verhindert absurd lange Eingaben                              | §3: Max. 34 alphanumerische Zeichen                  |
| **F2** | Mindestlänge ≥ 15 Zeichen (bereinigt) vor Submit        | Trivial | Vermeidet unnötige API-Calls                                  | §5: Kürzeste IBAN = Norwegen mit 15                  |
| **F3** | Erste 2 Zeichen müssen Buchstaben sein                  | Trivial | Sofortiges Feedback                                           | §3: Ländercode = 2 Großbuchstaben (ISO 3166-1)       |
| **F4** | Zeichen 3–4 müssen Ziffern sein                         | Trivial | Sofortiges Feedback                                           | §3: Prüfziffern = 2 Ziffern                          |
| **F5** | Nur A–Z und 0–9 erlaubt (nach Bereinigung)              | Trivial | Bereits implizit durch `cleanIban()`, aber kein User-Feedback | §3: Erlaubte Zeichen                                 |
| **F6** | Mod-97-Check im Frontend via `BigInt`                   | Mittel  | Komplette Offline-Validierung, spart Latenz                   | §6: Algorithmus universell, `BigInt` in JS verfügbar |

### 3.3 Beispiel-Implementierung für F1–F5

```typescript
// In lib/utils.ts oder als eigene Datei lib/iban-validation.ts

interface ClientValidationResult {
  valid: boolean;
  error: string | null;
}

export function validateIbanClient(raw: string): ClientValidationResult {
  const clean = cleanIban(raw);

  if (clean.length < 15) {
    return { valid: false, error: "IBAN zu kurz (min. 15 Zeichen)" };
  }
  if (clean.length > 34) {
    return { valid: false, error: "IBAN zu lang (max. 34 Zeichen)" };
  }
  if (!/^[A-Z]{2}/.test(clean)) {
    return {
      valid: false,
      error: "IBAN muss mit 2 Buchstaben (Ländercode) beginnen",
    };
  }
  if (!/^[A-Z]{2}\d{2}/.test(clean)) {
    return {
      valid: false,
      error: "Stelle 3–4 müssen Ziffern sein (Prüfziffern)",
    };
  }

  return { valid: true, error: null };
}
```

### 3.4 Frontend-Tests — Minimal

Die Tests in `IbanInput.test.tsx` prüfen nur, ob die Komponente rendert (Input, Buttons vorhanden). Es fehlen:

- Tests für `formatIban()` und `cleanIban()` in `utils.ts`
- Tests für User-Interaktion (Eingabe, Button-Klick, Ergebnis-Anzeige)
- Tests für Validierungslogik (sobald F1–F6 implementiert sind)

---

## 4. UX-Bewertung

### 4.1 Stärken — Solide Basis

| Aspekt                  | Bewertung  | Begründung                                                                 |
| ----------------------- | ---------- | -------------------------------------------------------------------------- |
| Auto-Formatierung       | ✓ Sehr gut | 4er-Gruppen nach DIN 5008 beim Tippen — exakt wie iban.md §8 es beschreibt |
| Placeholder             | ✓ Gut      | Zeigt eine realistische Beispiel-IBAN `DE89 3704 0044 0532 0130 00`        |
| Mono-Font + Tracking    | ✓ Gut      | Macht die IBAN deutlich lesbarer, wichtig für 15–34 Zeichen                |
| Farbcodiertes Ergebnis  | ✓ Gut      | Grün/Rot-Card + Badge — sofort verständlich                                |
| Zwei getrennte Aktionen | ✓ Gut      | "Validieren" vs. "Validieren & Speichern" klar getrennt                    |
| Loading-States          | ✓ Gut      | `Prüfe...` / `Speichere...` auf Buttons                                    |
| State-Reset bei Eingabe | ✓ Gut      | Vorherige Ergebnisse werden bei neuer Eingabe gelöscht                     |

### 4.2 Schwächen — Verbesserungspotenzial

| #      | Problem                                           | Auswirkung                                                                                      | Empfohlener Fix                                                                                                                   |
| ------ | ------------------------------------------------- | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **U1** | Keine Enter-Taste                                 | User muss immer klicken statt Enter zu drücken                                                  | `onKeyDown` → Enter löst Validierung aus                                                                                          |
| **U2** | Kein sofortiges Feedback                          | User tippt 30 Zeichen, weiß nicht ob Länge stimmt                                               | Inline-Hinweis unter dem Input: „22/22 Zeichen (DE)" oder „zu kurz"                                                               |
| **U3** | IBAN-Liste aktualisiert sich nicht nach Speichern | `IbanList` fetcht nur beim Mount                                                                | Callback/Event nach erfolgreichem Speichern → Liste neu laden                                                                     |
| **U4** | Keine differenzierten Fehlermeldungen             | „IBAN ungültig" — aber warum? Zu kurz? Falsche Prüfziffer? Falsches Land?                       | Backend liefert keinen Fehlergrund (Reason Code). Alternativ: Frontend-Validierung (§3) gibt spezifischere Meldungen              |
| **U5** | Kein Auto-Focus                                   | User muss erst ins Input klicken                                                                | `autoFocus` auf dem Input-Element                                                                                                 |
| **U6** | Speichern-Button speichert auch ungültige IBANs   | Möglicherweise verwirrend                                                                       | Entweder nur bei gültiger IBAN erlauben, oder explizit kommunizieren: „Auch ungültige IBANs werden zur Dokumentation gespeichert" |
| **U7** | Kein Leeren-Button                                | User muss manuell alles löschen                                                                 | X-Icon im Input oder Reset-Button                                                                                                 |
| **U8** | Label „BLZ" bei internationalen IBANs             | BLZ ist ein rein deutsches Konzept. Bei GB-IBANs steht dort der Sort Code, bei CH die BC-Nummer | Label dynamisch: „BLZ" für DE, „Bank Identifier" für andere Länder                                                                |
| **U9** | Kein `maxLength` auf dem Input                    | User kann beliebig viel tippen                                                                  | `maxLength={42}` (34 Zeichen + 8 Leerzeichen durch 4er-Gruppen)                                                                   |

### 4.3 Internationalisierungs-spezifische UX-Probleme

Wenn das Projekt internationale IBANs unterstützen soll (was die Mod-97-Logik bereits tut), gibt es folgende UX-Lücken:

| Problem                           | Detail                                                                                                                                                              |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Placeholder ist DE-only           | `DE89 3704 0044 0532 0130 00` suggeriert, dass nur deutsche IBANs erwartet werden. Besser: Ein Hinweistext „Unterstützt alle IBAN-Länder (DE, AT, CH, GB, FR, ...)" |
| Ergebnis-Card zeigt nur DE-Felder | `bankIdentifier` wird als „BLZ" gelabelt — bei anderen Ländern irreführend                                                                                          |
| Zeichenzähler fehlt               | Von 15 (NO) bis 34 (max.) — der User weiß nicht, wann die Eingabe „komplett" ist                                                                                    |

---

## 5. Zusammenfassung — Prioritäten

### Muss (Korrektheit für internationale IBANs)

1. **`COUNTRY_LENGTHS` erweitern** um mindestens die 10 SEPA-Kernländer aus iban.md §5
2. **Defensiver Guard** in `isValidMod97()`: `if (iban.length() < 5) return false`
3. **Allgemeine Strukturprüfung**: Pos. 1–2 = Buchstaben, Pos. 3–4 = Ziffern, Gesamtlänge 15–34
4. **Internationale Test-Cases** hinzufügen (AT, GB, NO, FR, mindestens eine pro Längenkategorie)

### Sollte (Frontend-Validierung + UX)

5. **Frontend-seitige Basisvalidierung** (Länge, Ländercode-Format — kein API-Call nötig)
6. **Enter-Taste** zum Validieren
7. **IBAN-Liste nach Speichern aktualisieren**
8. **`maxLength={42}`** auf dem Input-Feld
9. **Label „BLZ"** → „Bank Identifier" für Nicht-DE-IBANs

### Kann (Nice-to-have)

10. **Mod-97 im Frontend** via `BigInt` (komplette Offline-Validierung)
11. **Zeichenzähler** mit erwarteter Länge basierend auf Ländercode
12. **Differenzierte Fehlermeldungen** (warum ungültig?)
13. **Bessere JavaDoc-Kommentare** mit fachlichem Kontext aus iban.md
