# IBAN — Fachliches Wissensdokument

> **Quellen**: [Wikipedia DE – Internationale Bankkontonummer](https://de.wikipedia.org/wiki/Internationale_Bankkontonummer), [Wikipedia EN – International Bank Account Number](https://en.wikipedia.org/wiki/International_Bank_Account_Number)

---

## 1. Definition

Die **Internationale Bankkontonummer** (englisch _International Bank Account Number_, **IBAN**) ist eine international standardisierte Notation für Bankkontonummern. Sie wird durch die ISO-Norm **ISO 13616-1:2020** beschrieben und dient dazu, Bankverbindungen weltweit eindeutig zu identifizieren und Fehler bei der Übermittlung zu minimieren.

Die IBAN wurde entwickelt, um die Zahlungsverkehrssysteme der einzelnen Länder einheitlicher zu gestalten. Die standardisierte Struktur aus Prüf- und Kontodaten (Bankidentifikation + Kontoidentifikation) erschließt Integrations- und Automatisierungspotentiale für den Datenaustausch zwischen Banken verschiedener Länder.

Seit Dezember 2024 nutzen **89 Länder** das IBAN-System.

---

## 2. Geschichtlicher Hintergrund

| Jahr | Ereignis                                                                                     |
| ---- | -------------------------------------------------------------------------------------------- |
| 1997 | Erste Veröffentlichung als ISO 13616:1997                                                    |
| 2003 | Überarbeitung zu ISO 13616:2003 — feste Länge pro Land, nur Großbuchstaben                   |
| 2006 | Schweiz stellt auf IBAN um                                                                   |
| 2007 | ISO teilt Standard in zwei Teile; SWIFT wird offizielle Registrierungsstelle                 |
| 2009 | SEPA-Überweisungen verpflichtend, SEPA-Lastschrift ab November                               |
| 2012 | EU beschließt IBAN-Pflicht (Verordnung (EU) Nr. 260/2012)                                    |
| 2014 | 1. Februar — IBAN ersetzt nationale Kontonummern für Überweisungen & Lastschriften in der EU |
| 2016 | 1. Februar — BIC-Angabe auch bei grenzüberschreitenden EU-Überweisungen nicht mehr nötig     |

**Vor der IBAN** variierten die Notationen für Bankverbindungen (Bankleitzahl, Sort Code, Routing Number, etc.) erheblich zwischen den Ländern. Das führte häufig zu fehlenden Routing-Informationen, verzögerten Zahlungen und Zusatzkosten.

---

## 3. Aufbau / Struktur

Die IBAN besteht aus **maximal 34 alphanumerischen Zeichen**:

```
┌──────────┬──────────────┬──────────────────────────────────────┐
│ Pos 1–2  │   Pos 3–4    │         Pos 5–34 (max.)              │
│ Länder-  │  Prüfziffern │  BBAN (Basic Bank Account Number)    │
│  code    │  (Check       │  — länderspezifisch                  │
│ (ISO     │   Digits)    │  — enthält Bankleitzahl, Filiale,    │
│  3166-1) │              │    Kontonummer etc.                   │
└──────────┴──────────────┴──────────────────────────────────────┘
```

- **Ländercode**: 2 Großbuchstaben gemäß ISO 3166-1 (z.B. `DE` für Deutschland)
- **Prüfziffern**: 2 Ziffern, berechnet nach ISO 7064 (Modulo 97-10)
- **BBAN**: Max. 30 Zeichen (A–Z, 0–9), länderspezifisch aufgebaut

### Erlaubte Zeichen

- Ziffern 0–9
- Lateinische Großbuchstaben A–Z
- Auch in Ländern, deren Schrift nicht das lateinische Alphabet nutzt (z.B. Griechenland)

---

## 4. IBAN-Struktur für Deutschland

Die deutsche IBAN hat **immer 22 Stellen**:

```
D E 6 8 2 1 0 5 0 1 7 0 0 0 1 2 3 4 5 6 7 8
├─┤ ├─┤ ├───────────────┤ ├─────────────────────┤
 │   │    Bankleitzahl       Kontonummer
 │   │    (8 Stellen)        (10 Stellen)
 │   Prüfziffern
 Ländercode "DE"
```

| Feld               | Stellen | Beschreibung                                     |
| ------------------ | ------- | ------------------------------------------------ |
| Ländercode         | 1–2     | Immer `DE`                                       |
| Prüfziffern        | 3–4     | Berechnet nach Modulo 97                         |
| Bankleitzahl (BLZ) | 5–12    | 8-stellig, identifiziert die Bank/Filiale        |
| Kontonummer        | 13–22   | 10-stellig, ggf. mit führenden Nullen aufgefüllt |

### BLZ-Beispiele aus diesem Projekt

| BLZ      | Bank               |
| -------- | ------------------ |
| 50070010 | Deutsche Bank      |
| 50040000 | Commerzbank        |
| 10050000 | Berliner Sparkasse |

---

## 5. IBAN-Struktur anderer Länder (Auswahl)

| Land                            | Länge | Format                                   | Besonderheiten                              |
| ------------------------------- | ----- | ---------------------------------------- | ------------------------------------------- |
| **AT** (Österreich)             | 20    | `ATpp bbbb bkkk kkkk kkkk`               | 5-stellige BLZ                              |
| **CH** (Schweiz)                | 21    | `CHpp bbbb bkkk kkkk kkkk k`             | 5-stellige BC-Nummer                        |
| **FR** (Frankreich)             | 27    | `FRpp bbbb bsss sskk kkkk kkkk kKK`      | Inkl. _code guichet_ + nationale Prüfziffer |
| **GB** (Vereinigtes Königreich) | 22    | `GBpp bbbb ssss sskk kkkk kk`            | Sort Code + Account Number                  |
| **ES** (Spanien)                | 24    | `ESpp bbbb ssss KKkk kkkk kkkk`          | Eigene Prüfziffern (K)                      |
| **IT** (Italien)                | 27    | `ITpp Kbbb bbss sssk kkkk kkkk kkk`      | Prüfbuchstabe (CIN) an Pos. 5               |
| **NL** (Niederlande)            | 18    | `NLpp bbbb kkkk kkkk kk`                 | 4-Buchstaben BIC als Bankcode               |
| **PL** (Polen)                  | 28    | `PLpp bbbs sssK kkkk kkkk kkkk kkkk`     | Nationale Prüfziffer (K)                    |
| **NO** (Norwegen)               | 15    | `NOpp bbbb kkkk kkK`                     | Kürzeste IBAN in Europa                     |
| **MT** (Malta)                  | 31    | `MTpp bbbb ssss skkk kkkk kkkk kkkk kkk` | Längste IBAN in Europa                      |

> **Legende**: `p` = Prüfziffer, `b` = Bankleitzahl, `s` = Filialnummer, `k` = Kontonummer, `K` = nationale Prüfziffer

---

## 6. Prüfziffer-Berechnung (Modulo 97)

### 6.1 Validierung einer IBAN

Der Algorithmus prüft, ob eine IBAN mathematisch korrekt ist:

**Schritt 1** — Länge prüfen (länderspezifisch, z.B. DE = 22 Zeichen). Falls falsch → ungültig.

**Schritt 2** — Die ersten 4 Zeichen (Ländercode + Prüfziffern) ans **Ende** verschieben.

```
Vorher:  DE68 2105 0170 0012 3456 78
Nachher: 2105 0170 0012 3456 78 DE68
```

**Schritt 3** — Jeden Buchstaben durch eine Zahl ersetzen: `A = 10, B = 11, …, Z = 35`

```
D = 13, E = 14
→ 210501700012345678 131468
```

**Schritt 4** — Die entstandene (sehr große) Zahl **modulo 97** berechnen.

```
210501700012345678131468 mod 97 = 1
```

**Schritt 5** — Wenn das Ergebnis **= 1** ist, ist die IBAN gültig.

### 6.2 Berechnung der Prüfziffern (IBAN-Generierung)

**Schritt 1** — Die Prüfziffern auf `00` setzen (z.B. `DE00...`).

**Schritt 2** — Die ersten 4 Zeichen ans Ende verschieben.

**Schritt 3** — Buchstaben durch Zahlen ersetzen (A = 10, …, Z = 35).

**Schritt 4** — Modulo 97 der resultierenden Zahl berechnen.

**Schritt 5** — **98 minus Rest** = Prüfziffern (einstelliges Ergebnis mit führender Null auffüllen).

```
Beispiel:
DE00 2105 0170 0012 3456 78
→ 210501700012345678131400
→ mod 97 = 30
→ 98 − 30 = 68
→ Prüfziffern = 68
→ IBAN: DE68 2105 0170 0012 3456 78
```

### 6.3 Stückweise Berechnung (Piece-wise Modulo)

Da die Zahl bis zu **60+ Stellen** haben kann, unterstützen viele Programmiersprachen keine nativen Integer dieser Größe. Der Modulo kann **stückweise** berechnet werden:

```
Zahl: 3214282912345698765432161182

Schritt 1: N = 321428291           → 321428291 mod 97 = 70
Schritt 2: N = 702345698           → 702345698 mod 97 = 29
Schritt 3: N = 297654321           → 297654321 mod 97 = 24
Schritt 4: N = 2461182             → 2461182   mod 97 = 1 ✓
```

**Prinzip**: Nimm die ersten 9 Ziffern, berechne `mod 97`, hänge die nächsten 7 Ziffern an den Rest, wiederhole bis alle Ziffern verarbeitet sind.

> **In Java** kann stattdessen `BigInteger` verwendet werden, das beliebig große Zahlen nativ unterstützt — analog zu JavaScripts `BigInt`.

---

## 7. Fehlererkennungsfähigkeit

Das Modulo-97-Verfahren (ISO 7064) erkennt:

| Fehlertyp                                     | Erkennungsrate                              |
| --------------------------------------------- | ------------------------------------------- |
| **Einzelner Tippfehler** (eine Ziffer falsch) | **100%**                                    |
| **Zahlendreher** zweier benachbarter Ziffern  | **100%** (fast immer bei fast-benachbarten) |
| **Verschiebung** der gesamten Zeichenkette    | **100%**                                    |
| **Auslassung** einer Ziffer                   | Erkannt durch feste Länge                   |
| **Verdopplung** einer Ziffer                  | Erkannt durch feste Länge                   |
| **Doppelter Substitutionsfehler**             | Sehr hohe Rate                              |

Für die deutsche IBAN kommt erschwerend hinzu:

- Nur ca. 3.600 verschiedene BLZ existieren (von 10.000.000 möglichen)
- Kontonummern haben institutsspezifische eigene Prüfzifferverfahren

**Fazit**: Die Wahrscheinlichkeit, dass ein normaler Tippfehler zu einer gültigen, existierenden IBAN führt, ist extrem gering.

---

## 8. Schreibweise

### Elektronisch (maschinell)

Ohne Trennzeichen, als durchgehende Zeichenkette:

```
DE68210501700012345678
```

### Papierform (DIN 5008)

In Vierergruppen, getrennt durch Leerzeichen (von links beginnend):

```
DE68 2105 0170 0012 3456 78
```

### Eingabe in Formulare

- Viele Systeme fügen automatisch Leerzeichen ein und entfernen sie vor der Verarbeitung
- Andere Systeme können eine mit Trennzeichen angereicherte IBAN nicht verarbeiten
- **Best Practice**: Alle Nicht-Alphanumerischen Zeichen vor der Verarbeitung entfernen

> **Projektrelevanz**: Im Frontend werden Leerzeichen und Trennzeichen (Bindestriche, Punkte) entfernt, bevor die IBAN ans Backend gesendet wird. Die Anzeige erfolgt in Vierergruppen.

---

## 9. SEPA-Kontext

### Was ist SEPA?

Der **Single Euro Payments Area** (Europäischer Zahlungsraum) ist ein einheitliches Zahlungssystem, das nationale Zahlungssysteme in der EU ersetzt hat. Die IBAN ist das zentrale Identifikationsmerkmal.

### Wichtige Regelungen

| Regelung                     | Datum      | Inhalt                               |
| ---------------------------- | ---------- | ------------------------------------ |
| Verordnung (EU) Nr. 260/2012 | 14.02.2012 | IBAN-Pflicht beschlossen             |
| SEPA-Stichtag                | 01.02.2014 | Nationale Systeme durch IBAN ersetzt |
| BIC-Pflicht Inland entfällt  | 01.02.2014 | IBAN reicht für Inlandsüberweisungen |
| BIC-Pflicht EU entfällt      | 01.02.2016 | IBAN reicht auch grenzüberschreitend |

### SEPA-Teilnehmerländer

Die 36 SEPA-Teilnehmerländer umfassen:

- Alle 27 EU-Mitgliedstaaten
- Island, Liechtenstein, Norwegen (EWR)
- Schweiz, Monaco, San Marino, Vatikanstadt, Vereinigtes Königreich, Andorra

---

## 10. Abgrenzung zu anderen Systemen

| System                          | Beschreibung                                | Verhältnis zur IBAN                                                 |
| ------------------------------- | ------------------------------------------- | ------------------------------------------------------------------- |
| **BIC / SWIFT-Code** (ISO 9362) | Identifiziert eine Bank (8 oder 11 Zeichen) | War früher zusätzlich zur IBAN nötig, seit 2016 in der EU entfallen |
| **BLZ** (Bankleitzahl)          | Deutsche 8-stellige Bankidentifikation      | In der deutschen IBAN enthalten (Stellen 5–12)                      |
| **Sort Code**                   | Britische 6-stellige Bankidentifikation     | In der britischen IBAN enthalten                                    |
| **Routing Number** (ABA)        | US-Bankidentifikation                       | USA nutzt kein IBAN-System                                          |
| **BBAN**                        | IBAN ohne Ländercode und Prüfziffern        | Nationaler Teil der IBAN                                            |

### Länder ohne IBAN

- **USA** — nutzt ABA Routing Transit Numbers
- **Kanada** — nutzt Routing Numbers von Payments Canada
- **Australien / Neuseeland** — nutzt BSB-Codes (Bank State Branch)

---

## 11. Externe Validierungs-APIs

### openiban.com (wird in diesem Projekt verwendet)

```
GET https://openiban.com/validate/{iban}?getBIC=true&validateBankCode=true
```

Liefert:

- Ob die IBAN gültig ist
- BIC-Code der Bank
- Bankname und Sitz
- Validierung des Bankcodes

### Weitere Dienste

- **UN CEFACT TBG5** — Kostenloser Validierungsservice in 32 Sprachen
- **SWIFT** — Offizielle Registrierungsstelle für IBAN-Formate

---

## 12. Relevanz für dieses Projekt

Dieses Wissensdokument bildet die fachliche Grundlage für Lexiban:

| Fachliches Konzept                    | Umsetzung im Projekt                                             |
| ------------------------------------- | ---------------------------------------------------------------- |
| IBAN-Aufbau (Abschnitt 3–4)           | Parsing der Eingabe, Extraktion von BLZ/Kontonummer              |
| Modulo-97-Algorithmus (Abschnitt 6)   | `IbanValidationService.java` — eigene Validierungslogik          |
| Stückweise Berechnung (Abschnitt 6.3) | Java `BigInteger` für die Modulo-Berechnung                      |
| Schreibweise (Abschnitt 8)            | Frontend-Formatierung in 4er-Gruppen, Bereinigung vor API-Aufruf |
| BLZ-Extraktion (Abschnitt 4)          | Stellen 5–12 extrahieren, gegen bekannte Banken matchen          |
| Externe API (Abschnitt 11)            | Fallback-Validierung über openiban.com                           |
| Fehlertoleranz (Abschnitt 7)          | Verständnis, warum Modulo 97 ausreicht                           |

---

## 13. Glossar

| Begriff        | Erklärung                                                                                       |
| -------------- | ----------------------------------------------------------------------------------------------- |
| **IBAN**       | International Bank Account Number — internationale Kontonummer                                  |
| **BBAN**       | Basic Bank Account Number — nationaler Teil der IBAN (ohne Ländercode + Prüfziffern)            |
| **BIC**        | Business Identifier Code (SWIFT-Code) — identifiziert eine Bank weltweit                        |
| **BLZ**        | Bankleitzahl — 8-stellige deutsche Bankidentifikation                                           |
| **SEPA**       | Single Euro Payments Area — einheitlicher Euro-Zahlungsraum                                     |
| **Modulo 97**  | Prüfziffernverfahren nach ISO 7064 — Rest bei Division durch 97                                 |
| **ISO 13616**  | Internationaler Standard für IBAN-Format und Prüfziffern                                        |
| **ISO 3166-1** | Standard für Ländercodes (Alpha-2: DE, AT, CH, …)                                               |
| **ISO 7064**   | Standard für Prüfziffernsysteme (MOD-97-10)                                                     |
| **SWIFT**      | Society for Worldwide Interbank Financial Telecommunication — Registrierungsstelle und Netzwerk |
| **ECBS**       | European Committee for Banking Standards (bis 2006) — hat die IBAN mitentwickelt                |
| **DIN 5008**   | Deutsche Norm für Schreibregeln — schreibt Vierergruppierung der IBAN vor                       |
