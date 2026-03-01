# IBAN Validator — Technische Evaluation

> Evaluation des gesamten Projekts auf Basis der fachlichen Spezifikation in [iban.md](iban.md).
> Fokus: Korrektheit der Validierung, Code-Dokumentation, Frontend-UX und internationale Unterstützung.
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

### 1.2 Probleme bei internationalen IBANs — Status

| #      | Problem                                    | Schwere     | Status         | Details                                                                                               |
| ------ | ------------------------------------------ | ----------- | -------------- | ----------------------------------------------------------------------------------------------------- |
| **P1** | `COUNTRY_LENGTHS` enthielt nur `"DE"` → 22 | **Hoch**    | ✅ Behoben     | Erweitert um 18 SEPA-Länder (AT, BE, CH, DE, DK, ES, FI, FR, GB, IE, IT, LU, MT, NL, NO, PL, PT, SE). |
| **P2** | Keine allgemeine Mindestlänge              | **Mittel**  | ✅ Behoben     | `IBAN_STRUCTURE` Regex erzwingt min. 15 Zeichen. Edge Case `"DE68"` (4 Zeichen) wird abgewiesen.      |
| **P3** | Keine Maximallänge (34 Zeichen)            | **Niedrig** | ✅ Behoben     | `IBAN_STRUCTURE` Regex erzwingt max. 34 Zeichen.                                                      |
| **P4** | Keine Prüfung, ob Pos. 3–4 Ziffern sind    | **Niedrig** | ✅ Behoben     | `IBAN_STRUCTURE` Regex: `\d{2}` an Pos. 3–4.                                                          |
| **P5** | Keine Prüfung, ob Pos. 1–2 Buchstaben sind | **Niedrig** | ✅ Behoben     | `IBAN_STRUCTURE` Regex: `[A-Z]{2}` an Pos. 1–2.                                                       |
| **P6** | BLZ-Extraktion nur für DE                  | **OK**      | Kein Fix nötig | BLZ ist ein deutsches Konzept. Die externe API übernimmt Bankauflösung für andere Länder.             |

### 1.3 Implementierte Fixes

Alle drei Prioritäten wurden umgesetzt:

- **Priorität 1**: `COUNTRY_LENGTHS` auf 18 SEPA-Länder erweitert via `Map.ofEntries()`.
- **Priorität 2**: Neuer `IBAN_STRUCTURE` Regex (`^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$`) als kompiliertes `Pattern` prüft Ländercode, Prüfziffern-Format, BBAN-Zeichen und Gesamtlänge 15–34 in einem Schritt.
- **Priorität 3**: Nicht mehr nötig — der Regex garantiert min. 15 Zeichen, daher kann `substring(4)` nie einen Leerstring erzeugen.

### 1.4 Normalisierung — verbessert ✓

`normalize()` entfernt **alle nicht-alphanumerischen Zeichen** (`[^A-Za-z0-9]`) und konvertiert zu Uppercase. Das ist sicherer als die vorherige Whitelist (`[\\s\\-.]`), da auch Schrägstriche, Unterstriche und andere unerwartete Trennzeichen entfernt werden.

### 1.5 Externe API als Fallback — korrekt ✓

Die Architektur ist sinnvoll: Erst lokal validieren (Mod-97), dann bei gültiger IBAN ohne Bankname die externe API befragen. Das funktioniert international — openiban.com kennt Banken aus vielen Ländern.

### 1.6 Testabdeckung — international erweitert ✓

Tests in `IbanValidationServiceTest` umfassen jetzt 23 Tests (vorher 12), darunter:

| Test                                                 | Status         |
| ---------------------------------------------------- | -------------- |
| Gültige AT-IBAN (20 Zeichen)                         | ✅ Hinzugefügt |
| Gültige GB-IBAN (22 Zeichen, mit Buchstaben im BBAN) | ✅ Hinzugefügt |
| Gültige NO-IBAN (15 Zeichen, kürzeste IBAN)          | ✅ Hinzugefügt |
| Gültige FR-IBAN (27 Zeichen)                         | ✅ Hinzugefügt |
| Gültige MT-IBAN (31 Zeichen, längste IBAN)           | ✅ Hinzugefügt |
| Falsche Länge für bekanntes Land (AT mit 19 Zeichen) | ✅ Hinzugefügt |
| IBAN mit 4 Zeichen (Edge Case)                       | ✅ Hinzugefügt |
| IBAN mit 35+ Zeichen (über ISO-Maximum)              | ✅ Hinzugefügt |
| Nicht-Buchstaben als Ländercode                      | ✅ Hinzugefügt |
| Nicht-Ziffern als Prüfziffern                        | ✅ Hinzugefügt |

---

## 2. Code-Kommentare und JavaDoc

### 2.1 Gesamteindruck: Gut — Alle Vorschläge umgesetzt ✓

Die Kommentare enthalten jetzt durchgehend fachlichen Kontext und TS-Analogien.

### 2.2 IbanValidationService — Umgesetzt ✓

| Stelle            | Status                                                                          |
| ----------------- | ------------------------------------------------------------------------------- |
| `KNOWN_BANKS`     | ✅ JavaDoc mit Erklärung zu Demo-Subset, BLZ vs. Sort Code/BC-Nummer            |
| `COUNTRY_LENGTHS` | ✅ JavaDoc mit ISO-13616-Referenz, kürzeste/längste IBAN                        |
| `IBAN_STRUCTURE`  | ✅ Neues Pattern mit detaillierter JavaDoc + TS-Äquivalent                      |
| `isValidMod97()`  | ✅ Erweiterte JavaDoc mit `Character.getNumericValue()` Erklärung + TS-Analogie |
| `validate()`      | ✅ JavaDoc erklärt null-Returns für bankName/bankIdentifier                     |
| BLZ-Extraktion    | ✅ Kommentar mit 1-based vs. 0-based Erklärung + iban.md §4 Referenz            |

### 2.3 ExternalIbanApiService — Umgesetzt ✓

| Stelle                | Status                                                   |
| --------------------- | -------------------------------------------------------- |
| Query-Parameter       | ✅ getBIC/validateBankCode erklärt                       |
| `catch (Exception e)` | ✅ Bewusster breiter Catch dokumentiert + TS-Analogie    |
| Null-Rückgabe         | ✅ "Service nicht verfügbar" vs. "IBAN ungültig" erklärt |

### 2.4 IbanController — Umgesetzt ✓

| Stelle                  | Status                                                              |
| ----------------------- | ------------------------------------------------------------------- |
| `validateAndSaveIban()` | ✅ JavaDoc: Auch ungültige IBANs werden gespeichert (Audit/History) |
| `validateIban()`        | ✅ Kommentar: Externe API wird für non-DE-IBANs immer aufgerufen    |

### 2.5 Tests — Sehr gute Kommentare ✓

Die Vitest/Jest-Analogien in `IbanValidationServiceTest` und `IbanControllerTest` sind exzellent für das Entwicklerprofil.

---

## 3. Frontend — UX-Unterstützung (keine vollständige Validierung)

> **Projektanforderung**: Das Frontend führt **keine eigenständige IBAN-Validierung** durch.
> Die Validierung erfolgt ausschließlich im Backend. Das Frontend bietet nur UX-Unterstützung.
> Es gibt nur einen POST /api/ibans Endpunkt, der immer validiert und speichert.

### 3.1 Aktueller Stand

- **Formatierung** beim Tippen: `formatIban()` bereinigt und gruppiert in 4er-Blöcke ✓
- **Bereinigung** vor API-Call: `cleanIban()` entfernt Nicht-Alphanumerisches ✓
- **Leereingabe-Schutz**: `!input.trim()` deaktiviert Button ✓
- **`maxLength={42}`**: Begrenzt Eingabe auf 34 Zeichen + 8 Leerzeichen ✅
- **Enter-Taste**: Löst Validierung aus ✅
- **Auto-Focus**: Input wird automatisch fokussiert ✅
- **Clear-Button**: ✕-Icon im Input zum Löschen ✅
- **Listen-Refresh**: IBAN-Liste aktualisiert sich nach jedem Prüfen ✅
- **Dynamisches Label**: "BLZ" für DE, "Bank Identifier" für andere Länder ✅
- **Internationaler Hinweis**: CardDescription erwähnt explizit alle IBAN-Länder ✅

### 3.2 Frontend-Tests — Umfassend erweitert ✓

29 Frontend-Tests verteilt auf zwei Dateien:

- **`utils.test.ts`** (19 Tests): `formatIban()` (7 Tests), `cleanIban()` (5 Tests), `getExpectedLength()` (6 Tests), `COUNTRY_LENGTHS` (1 Test)
- **`IbanInput.test.tsx`** (10 Tests): Render, autoFocus, maxLength, Formatierung beim Tippen, Zeichenzähler, Clear-Button, Button-Zustand

---

## 4. UX-Bewertung

### 4.1 Stärken

| Aspekt                  | Bewertung  | Begründung                                                                 |
| ----------------------- | ---------- | -------------------------------------------------------------------------- |
| Auto-Formatierung       | ✓ Sehr gut | 4er-Gruppen nach DIN 5008 beim Tippen — exakt wie iban.md §8 es beschreibt |
| Placeholder             | ✓ Gut      | Zeigt eine realistische Beispiel-IBAN `DE89 3704 0044 0532 0130 00`        |
| Mono-Font + Tracking    | ✓ Gut      | Macht die IBAN deutlich lesbarer, wichtig für 15–34 Zeichen                |
| Farbcodiertes Ergebnis  | ✓ Gut      | Grün/Rot-Card + Badge — sofort verständlich                                |
| Ein klarer Endpunkt     | ✓ Gut      | Ein "Prüfen"-Button, der immer validiert und speichert                     |
| Loading-States          | ✓ Gut      | `Prüfe...` auf Button                                                      |
| State-Reset bei Eingabe | ✓ Gut      | Vorherige Ergebnisse werden bei neuer Eingabe gelöscht                     |

### 4.2 UX-Probleme — Status

| #      | Problem                                           | Status                                                |
| ------ | ------------------------------------------------- | ----------------------------------------------------- |
| **U1** | Keine Enter-Taste                                 | ✅ Behoben — `onKeyDown` Handler                      |
| **U2** | Kein sofortiges Feedback (Zeichenzähler)          | ✅ Behoben — Zeichenzähler mit erwarteter Länge       |
| **U3** | IBAN-Liste aktualisiert sich nicht nach Speichern | ✅ Behoben — `refreshKey` Prop + Callback             |
| **U4** | Keine differenzierten Fehlermeldungen             | ✅ Behoben — Backend `reason`-Feld + Frontend-Anzeige |
| **U5** | Kein Auto-Focus                                   | ✅ Behoben — `autoFocus`                              |
| **U6** | Speichern-Button separat                          | ✅ Entfällt — nur noch ein Endpunkt                   |
| **U7** | Kein Leeren-Button                                | ✅ Behoben — ✕-Icon im Input                          |
| **U8** | Label "BLZ" bei internationalen IBANs             | ✅ Behoben — dynamisch: "BLZ" / "Bank Identifier"     |
| **U9** | Kein `maxLength` auf dem Input                    | ✅ Behoben — `maxLength={42}`                         |

### 4.3 Internationalisierungs-UX — Status

| Problem                           | Status                                                |
| --------------------------------- | ----------------------------------------------------- |
| Placeholder ist DE-only           | ✅ Behoben — CardDescription erwähnt alle IBAN-Länder |
| Ergebnis-Card zeigt nur DE-Felder | ✅ Behoben — dynamisches Label                        |
| Zeichenzähler fehlt               | ✅ Behoben — Zähler mit Soll/Ist pro Land             |

---

## 5. Zusammenfassung — Status

### Muss (Korrektheit für internationale IBANs) — ✅ Abgeschlossen

1. ✅ **`COUNTRY_LENGTHS` erweitert** auf 18 SEPA-Länder
2. ✅ **Strukturelle Vorvalidierung** via `IBAN_STRUCTURE` Regex (Mindest-/Maximallänge, Ländercode, Prüfziffern)
3. ✅ **Normalisierung verbessert** — entfernt alle nicht-alphanumerischen Zeichen
4. ✅ **Internationale Test-Cases** hinzugefügt (AT, GB, NO, FR, MT + Edge Cases) — 23 Tests gesamt
5. ✅ **JavaDoc-Kommentare** mit fachlichem Kontext und TS-Analogien

### Sollte (Frontend UX) — ✅ Abgeschlossen

6. ✅ **Enter-Taste** zum Validieren
7. ✅ **IBAN-Liste** aktualisiert sich nach Prüfen
8. ✅ **`maxLength={42}`** auf dem Input-Feld
9. ✅ **Label "BLZ"** → "Bank Identifier" für Nicht-DE-IBANs
10. ✅ **Clear-Button** (✕-Icon)
11. ✅ **Auto-Focus** auf Input
12. ✅ **Internationaler Hinweis** in CardDescription

### Kann (Nice-to-have) — ✅ Abgeschlossen

13. ✅ **Zeichenzähler** mit erwarteter Länge basierend auf Ländercode (z. B. „4 / 22 Zeichen (DE)")
14. ✅ **Differenzierte Fehlermeldungen** — Backend liefert `reason`-Feld mit spezifischem Grund (Struktur, Länge, Prüfziffern)
15. ✅ **Frontend-Tests** — 19 Utils-Tests (`formatIban`, `cleanIban`, `getExpectedLength`, `COUNTRY_LENGTHS`) + 10 Interaktions-Tests (Typing, Enter, Clear, Counter, autoFocus, maxLength)
