# DDD, Clean Architecture & Clean Code — Refactoring-Plan

> Analyse des Ist-Zustands und konkreter Umbau-Plan, um die Fachlichkeit aus [iban.md](iban.md) besser im Code abzubilden.
> Grundlage: Domain-Driven Design (DDD), Hexagonale Architektur (Ports & Adapters), Clean Architecture.

---

## 1. Ist-Zustand: Klassische Schichtenarchitektur

Das Projekt folgt dem typischen Spring-Boot-Pattern **Controller → Service → Repository** mit technischer Paketierung:

```
com.iban/
├── controller/    ← REST-Endpunkte + DTOs + Orchestrierungslogik
├── service/       ← Validierung + externe API (alles in einem Layer)
├── model/         ← JPA Entity (= Persistenz-Objekt, nicht Domäne)
├── repository/    ← Spring Data JPA
└── config/        ← CORS, Error Handling
```

### Probleme aus DDD-/Clean-Architecture-Sicht

| #   | Problem                                         | Wo im Code                                                              | DDD-Prinzip verletzt                                                                                               |
| --- | ----------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| 1   | **Anemic Domain Model**                         | `Iban.java` ist ein reiner Datenbehälter ohne Verhalten                 | IBAN sollte ein **Value Object** sein, das sich selbst normalisieren, parsen und validieren kann                   |
| 2   | **Fachlogik im Service statt im Domänenobjekt** | `IbanValidationService` enthält Mod-97, Normalisierung, BLZ-Extraktion  | Domänenlogik gehört **in die Domäne**, nicht in einen technischen Service                                          |
| 3   | **Controller enthält Use-Case-Logik**           | `IbanController.buildResponse()` orchestriert lokal + extern + Fallback | Controller sollte nur HTTP → Use Case → HTTP mappen                                                                |
| 4   | **Keine Ports & Adapters**                      | `ExternalIbanApiService` ist direkt eine Spring-Implementierung         | Domäne sollte ein **Interface** (Port) definieren, Implementierung ist Infrastruktur                               |
| 5   | **DTOs überall verstreut**                      | `ValidationResult` im Service, Request/Response im Controller           | Klare Trennung: Domain-Typen vs. Application-DTOs vs. API-DTOs                                                     |
| 6   | **Technische statt fachliche Pakete**           | `model`, `service`, `controller`                                        | DDD nutzt `domain`, `application`, `infrastructure`, `adapter`                                                     |
| 7   | **Domänenwissen hardcoded im Service**          | `KNOWN_BANKS`, `COUNTRY_LENGTHS` in `IbanValidationService`             | Das ist **Domänenwissen** (→ Value Object / Domain Service)                                                        |
| 8   | **Frontend: keine Domänenlogik**                | `cleanIban()` und `formatIban()` in `utils.ts`                          | IBAN ist ein fachliches Konzept — auch im Frontend sinnvoll als Value Object modelliert                            |
| 9   | **String-Typing**                               | IBAN wird überall als `String` durchgereicht                            | **Primitive Obsession** — ein fachlicher Typ (`IbanNumber`) wäre ausdrucksstärker und verhindert invalide Zustände |
| 10  | **Kein klarer Use-Case-Layer**                  | Controller ruft direkt Services + Repository auf                        | Application Layer fehlt — Use Cases orchestrieren den Ablauf, Controller mapped nur HTTP                           |

---

## 2. Theoretischer Hintergrund

### 2.1 Domain-Driven Design (DDD) — Kernkonzepte

| Konzept                 | Beschreibung                                                        | Relevanz für dieses Projekt                                              |
| ----------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| **Ubiquitous Language** | Fachbegriffe aus der Domäne im Code verwenden                       | IBAN, BLZ, Prüfziffer, BBAN, Ländercode → im Code als Typen und Methoden |
| **Value Object**        | Objekt definiert durch seine Werte (nicht durch eine ID), immutable | `IbanNumber`, `CountryCode`, `BankIdentifier`                            |
| **Entity**              | Objekt mit eigener Identität über die Zeit                          | Gespeicherte IBAN-Validierung (hat eine DB-ID)                           |
| **Domain Service**      | Fachlogik, die keinem einzelnen Objekt gehört                       | `Mod97Validator` — operiert auf IbanNumber                               |
| **Repository**          | Abstraktion für Persistenz (aus Sicht der Domäne)                   | Port-Interface, implementiert durch JPA-Adapter                          |
| **Port**                | Interface an der Domänengrenze                                      | `ExternalIbanLookupPort`, `IbanPersistencePort`                          |
| **Adapter**             | Implementierung eines Ports für ein bestimmtes Framework/System     | `OpenIbanApiAdapter`, `IbanPersistenceAdapter`                           |

### 2.2 Clean Architecture — Dependency Rule

```
                    ┌─────────────────────┐
                    │   adapter/web/      │  ← Kennt: application
                    │   (Controller)      │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼───────────┐
                    │   application/      │  ← Kennt: domain
                    │   (Use Cases)       │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼───────────┐
                    │   domain/           │  ← Kennt: NICHTS (framework-frei!)
                    │   (Value Objects,   │
                    │    Domain Services, │
                    │    Ports)           │
                    └─────────────────────┘
                              ▲
                    ┌─────────┴───────────┐
                    │   infrastructure/   │  ← Implementiert: domain/port
                    │   (JPA, REST Client,│
                    │    In-Memory Bank)  │
                    └─────────────────────┘
```

**Dependency Rule**: Abhängigkeiten zeigen immer **nach innen**. Die Domain kennt weder Spring, noch JPA, noch HTTP. Infrastruktur implementiert die Ports, die die Domain definiert.

### 2.3 Analogie zu TypeScript/Node.js

| Java / Spring Boot                     | TypeScript / Node.js Äquivalent                                       |
| -------------------------------------- | --------------------------------------------------------------------- |
| Value Object (`record IbanNumber`)     | `class IbanNumber` mit `readonly` Properties                          |
| Domain Service (kein `@Service`)       | Reines Modul/Klasse ohne Framework-Abhängigkeiten                     |
| Port (Interface)                       | TypeScript `interface` oder abstrakte Klasse                          |
| Adapter (`@Component implements Port`) | Klasse, die ein Interface implementiert + in DI-Container registriert |
| Application Service / Use Case         | Service-Klasse, die mehrere Dependencies orchestriert                 |
| `@RestController` (dünn)               | Express-Route-Handler, der nur HTTP ↔ Use Case mapped                 |

---

## 3. Ziel-Architektur

### 3.1 Backend — Paketstruktur

```
com.iban/
│
├── domain/                                  ← Innerster Ring: framework-frei, rein Java
│   ├── model/
│   │   ├── IbanNumber.java                  # Value Object: normalized IBAN mit Verhalten
│   │   ├── CountryCode.java                 # Value Object: 2-Buchstaben ISO 3166-1
│   │   ├── BankIdentifier.java              # Value Object: BLZ / Sort Code
│   │   ├── BankInfo.java                    # Value Object: Bankname + BIC (Ergebnis Lookup)
│   │   └── ValidationResult.java            # Immutable Ergebnis: valid + iban + bankInfo + method
│   │
│   ├── service/
│   │   ├── Mod97Validator.java              # Domain Service: Prüfziffernlogik
│   │   └── BankDirectory.java               # Domain Service Interface: BLZ → BankInfo
│   │
│   └── port/
│       ├── in/
│       │   ├── ValidateIbanUseCase.java     # Input-Port: "Validiere eine IBAN"
│       │   ├── SaveIbanUseCase.java         # Input-Port: "Validiere und speichere"
│       │   └── ListSavedIbansUseCase.java   # Input-Port: "Zeige gespeicherte IBANs"
│       └── out/
│           ├── IbanPersistencePort.java     # Output-Port: Speichern/Laden
│           └── ExternalIbanLookupPort.java  # Output-Port: externe API
│
├── application/                             ← Use Cases: Orchestrierung, kennt nur domain/
│   ├── ValidateIbanService.java             # Implementiert ValidateIbanUseCase
│   ├── SaveIbanService.java                 # Implementiert SaveIbanUseCase
│   └── ListSavedIbansService.java           # Implementiert ListSavedIbansUseCase
│
├── infrastructure/                          ← Adapter für externe Systeme
│   ├── persistence/
│   │   ├── IbanJpaEntity.java               # JPA Entity (≠ Domain Model!)
│   │   ├── IbanJpaRepository.java           # Spring Data Interface
│   │   └── IbanPersistenceAdapter.java      # Implementiert IbanPersistencePort
│   ├── external/
│   │   └── OpenIbanApiAdapter.java          # Implementiert ExternalIbanLookupPort
│   └── bank/
│       └── InMemoryBankDirectory.java       # Implementiert BankDirectory (KNOWN_BANKS)
│
├── adapter/
│   └── web/
│       ├── IbanController.java              # Dünn: HTTP ↔ Use Case, kein Fachlogik
│       └── dto/
│           ├── IbanRequest.java             # API-Request Record
│           └── IbanResponse.java            # API-Response Record
│
└── config/
    ├── CorsConfig.java
    └── GlobalExceptionHandler.java
```

### 3.2 Frontend — Paketstruktur

```
src/
├── domain/                                  ← Framework-frei, reine Fachlogik
│   ├── iban.ts                              # Value Object: IbanNumber mit parse/format/validate
│   └── types.ts                             # ValidationResult, BankInfo
│
├── application/                             ← Use Cases / Hooks
│   └── use-iban-validation.ts               # Custom Hook: Zustandslogik aus IbanInput extrahiert
│
├── infrastructure/                          ← HTTP-Anbindung
│   └── iban-api.ts                          # fetch-Aufrufe (reines I/O)
│
├── presentation/                            ← UI-Komponenten
│   ├── IbanInput.tsx                        # Nur Eingabe + Formatierung
│   ├── IbanValidationResult.tsx             # Ergebnis-Anzeige
│   ├── IbanList.tsx                         # Liste gespeicherter IBANs
│   └── ui/                                  # shadcn/ui Primitives
│
└── lib/
    └── utils.ts                             # Nur cn() — kein IBAN-Code mehr hier
```

---

## 4. Kerntransformationen im Detail

### 4.1 Value Object: `IbanNumber` (Backend)

**Vorher**: IBAN ist ein `String`, Normalisierung und Parsing in `IbanValidationService`.

**Nachher**: Self-validating Value Object — ein ungültiges `IbanNumber` kann nicht existieren.

```java
// domain/model/IbanNumber.java — Framework-frei, selbst-normalisierend
public record IbanNumber(String value) {

    /** Normalizes on construction: removes separators, uppercases. */
    public IbanNumber {
        value = value.replaceAll("[\\s\\-.]", "").toUpperCase();
        if (value.length() < 5 || !value.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid IBAN format: " + value);
        }
    }

    public CountryCode countryCode() {
        return new CountryCode(value.substring(0, 2));
    }

    public String checkDigits() {
        return value.substring(2, 4);
    }

    /** BBAN = Basic Bank Account Number — der nationale Teil ohne Ländercode + Prüfziffern */
    public String bban() {
        return value.substring(4);
    }

    /**
     * BLZ (Bankleitzahl) für deutsche IBANs: Stellen 5–12.
     * Gibt Optional.empty() zurück für nicht-deutsche IBANs.
     * Siehe iban.md Abschnitt 4.
     */
    public Optional<BankIdentifier> bankIdentifier() {
        if ("DE".equals(countryCode().value()) && value.length() >= 12) {
            return Optional.of(new BankIdentifier(value.substring(4, 12)));
        }
        return Optional.empty();
    }

    /** Vierergruppen-Formatierung nach DIN 5008 (siehe iban.md Abschnitt 8) */
    public String formatted() {
        return value.replaceAll("(.{4})", "$1 ").trim();
    }
}
```

**Warum?**

- Die Normalisierung (Leerzeichen entfernen, Uppercase) passiert **ein einziges Mal** im Konstruktor — statt an drei verschiedenen Stellen im Code.
- `countryCode()`, `bban()`, `bankIdentifier()` bilden die Fachlichkeit aus [iban.md Abschnitt 3–4](iban.md) direkt im Typ ab.
- Wer ein `IbanNumber`-Objekt hat, weiß: es ist normalisiert und hat mindestens die richtige Grundstruktur.

### 4.2 Weitere Value Objects

```java
// domain/model/CountryCode.java
public record CountryCode(String value) {
    public CountryCode {
        if (!value.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Invalid country code: " + value);
        }
    }
}

// domain/model/BankIdentifier.java — BLZ / Sort Code / BC-Nummer
public record BankIdentifier(String value) {
    public BankIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BankIdentifier must not be blank");
        }
    }
}

// domain/model/BankInfo.java — Ergebnis eines Bank-Lookups
public record BankInfo(String name, String bic) {}

// domain/model/ValidationResult.java — Fachliches Ergebnis
public record ValidationResult(
    boolean valid,
    IbanNumber iban,
    BankInfo bankInfo,         // nullable — Bank nicht immer auflösbar
    String validationMethod    // "local" | "external"
) {}
```

### 4.3 Domain Service: `Mod97Validator`

**Vorher**: Private Methode `isValidMod97()` in `IbanValidationService` (Spring `@Service`).

**Nachher**: Eigenständiger Domain Service — framework-frei, rein unit-testbar.

```java
// domain/service/Mod97Validator.java — KEIN @Service, KEIN Spring-Import
public class Mod97Validator {

    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.of(
        "DE", 22,
        "AT", 20,
        "CH", 21,
        "GB", 22
    );

    /**
     * Prüft IBAN nach ISO 7064 Modulo-97-10 Algorithmus.
     * Siehe iban.md Abschnitte 6.1 und 6.3.
     *
     * Schritt 1: Länderspezifische Länge prüfen
     * Schritt 2: Erste 4 Zeichen ans Ende verschieben
     * Schritt 3: Buchstaben → Zahlen (A=10, B=11, ..., Z=35)
     * Schritt 4: Modulo 97 berechnen (BigInteger, da 30+ Ziffern)
     * Schritt 5: Ergebnis == 1 → gültig
     */
    public boolean isValid(IbanNumber iban) {
        Integer expectedLength = COUNTRY_LENGTHS.get(iban.countryCode().value());
        if (expectedLength != null && iban.value().length() != expectedLength) {
            return false;
        }
        return mod97(iban.value()) == 1;
    }

    private int mod97(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            numeric.append(Character.isLetter(c)
                ? Character.getNumericValue(c)
                : c);
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue();
    }
}
```

**Warum kein `@Service`?** Die Domäne soll framework-frei sein. Spring registriert den Validator über eine `@Bean`-Methode in einer Config-Klasse oder der Application Layer instantiiert ihn direkt.

### 4.4 Ports: Schnittstellen an der Domänengrenze

```java
// domain/port/out/ExternalIbanLookupPort.java
public interface ExternalIbanLookupPort {
    /** Versuche Bankinformationen extern aufzulösen. Gibt Optional.empty() bei Fehler. */
    Optional<BankInfo> lookup(IbanNumber iban);
}

// domain/port/out/IbanPersistencePort.java
public interface IbanPersistencePort {
    void save(ValidationResult result);
    List<ValidationResult> findAll();
}

// domain/service/BankDirectory.java — Interface für BLZ-Lookup
public interface BankDirectory {
    Optional<BankInfo> resolve(BankIdentifier identifier);
}
```

**Warum Ports?**

- Die Domäne definiert **was** sie braucht (Interface), nicht **wie** es implementiert wird.
- `ExternalIbanLookupPort` könnte openiban.com, eine andere API oder ein Mock sein — die Domäne weiß es nicht und braucht es nicht zu wissen.
- **Analog in TypeScript**: Ein Interface `IbanLookup` das von einem `FetchIbanLookup` oder einem `MockIbanLookup` implementiert wird.

### 4.5 Application Layer: Use Cases

**Vorher**: Controller orchestriert lokal → extern → Persistenz in `buildResponse()`.

**Nachher**: Eigenständige Use-Case-Klasse.

```java
// application/ValidateIbanService.java
@Service
public class ValidateIbanService implements ValidateIbanUseCase {

    private final Mod97Validator mod97Validator;
    private final BankDirectory bankDirectory;
    private final ExternalIbanLookupPort externalLookup;

    // Constructor Injection — wie gehabt
    public ValidateIbanService(
            Mod97Validator mod97Validator,
            BankDirectory bankDirectory,
            ExternalIbanLookupPort externalLookup) {
        this.mod97Validator = mod97Validator;
        this.bankDirectory = bankDirectory;
        this.externalLookup = externalLookup;
    }

    @Override
    public ValidationResult execute(String rawIban) {
        IbanNumber iban = new IbanNumber(rawIban);  // Value Object normalisiert

        if (!mod97Validator.isValid(iban)) {
            return new ValidationResult(false, iban, null, "local");
        }

        // BLZ lokal auflösen
        BankInfo bankInfo = iban.bankIdentifier()
            .flatMap(bankDirectory::resolve)
            .orElse(null);

        String method = "local";

        // Fallback: externe API, wenn Bank lokal nicht bekannt
        if (bankInfo == null) {
            bankInfo = externalLookup.lookup(iban).orElse(null);
            if (bankInfo != null) {
                method = "external";
            }
        }

        return new ValidationResult(true, iban, bankInfo, method);
    }
}
```

### 4.6 Infrastruktur-Adapter

```java
// infrastructure/external/OpenIbanApiAdapter.java
@Component
public class OpenIbanApiAdapter implements ExternalIbanLookupPort {

    private final RestClient restClient;

    public OpenIbanApiAdapter() {
        this.restClient = RestClient.builder()
            .baseUrl("https://openiban.com/validate/")
            .build();
    }

    @Override
    public Optional<BankInfo> lookup(IbanNumber iban) {
        try {
            var response = restClient.get()
                .uri("{iban}?getBIC=true&validateBankCode=true", iban.value())
                .retrieve()
                .body(OpenIbanResponse.class);
            if (response != null && response.bankData() != null) {
                return Optional.of(new BankInfo(
                    response.bankData().name(),
                    response.bankData().bic()));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private record OpenIbanResponse(boolean valid, BankData bankData) {}
    private record BankData(String name, String bic) {}
}

// infrastructure/bank/InMemoryBankDirectory.java
@Component
public class InMemoryBankDirectory implements BankDirectory {

    private static final Map<String, String> KNOWN_BANKS = Map.of(
        "50070010", "Deutsche Bank",
        "50040000", "Commerzbank",
        "10050000", "Berliner Sparkasse"
    );

    @Override
    public Optional<BankInfo> resolve(BankIdentifier identifier) {
        String name = KNOWN_BANKS.get(identifier.value());
        return name != null
            ? Optional.of(new BankInfo(name, null))
            : Optional.empty();
    }
}

// infrastructure/persistence/IbanPersistenceAdapter.java
@Component
public class IbanPersistenceAdapter implements IbanPersistencePort {

    private final IbanJpaRepository jpaRepository;

    // ... mappt zwischen IbanJpaEntity ↔ ValidationResult
}
```

### 4.7 Dünner Controller

**Vorher**: Controller enthält `buildResponse()` mit Fallback-Logik, Entity-Mapping, DTOs.

**Nachher**: Nur HTTP-Mapping.

```java
// adapter/web/IbanController.java
@RestController
@RequestMapping("/api/ibans")
public class IbanController {

    private final ValidateIbanUseCase validateUseCase;
    private final SaveIbanUseCase saveUseCase;
    private final ListSavedIbansUseCase listUseCase;

    // Constructor Injection ...

    @PostMapping("/validate")
    public ResponseEntity<IbanResponse> validate(@Valid @RequestBody IbanRequest request) {
        ValidationResult result = validateUseCase.execute(request.iban());
        return ResponseEntity.ok(IbanResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<IbanResponse> validateAndSave(@Valid @RequestBody IbanRequest request) {
        ValidationResult result = saveUseCase.execute(request.iban());
        return ResponseEntity.ok(IbanResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<IbanResponse>> list() {
        return ResponseEntity.ok(
            listUseCase.execute().stream()
                .map(IbanResponse::from)
                .toList());
    }
}
```

### 4.8 Frontend: IBAN als Value Object

**Vorher**: `cleanIban()` und `formatIban()` als Utility-Funktionen in `utils.ts`.

**Nachher**: Fachliches Value Object.

```typescript
// domain/iban.ts
export class IbanNumber {
  readonly value: string;

  constructor(raw: string) {
    this.value = raw.replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
  }

  /** Ländercode nach ISO 3166-1 (z.B. "DE") — siehe iban.md Abschnitt 3 */
  get countryCode(): string {
    return this.value.slice(0, 2);
  }

  get checkDigits(): string {
    return this.value.slice(2, 4);
  }

  /** BBAN = Basic Bank Account Number — nationaler Teil */
  get bban(): string {
    return this.value.slice(4);
  }

  /** Vierergruppen nach DIN 5008 (siehe iban.md Abschnitt 8) */
  get formatted(): string {
    return this.value.replace(/(.{4})/g, "$1 ").trim();
  }

  get isGerman(): boolean {
    return this.countryCode === "DE";
  }

  /** BLZ für deutsche IBANs: Stellen 5–12 (siehe iban.md Abschnitt 4) */
  get blz(): string | null {
    return this.isGerman && this.value.length >= 12
      ? this.value.slice(4, 12)
      : null;
  }

  get isEmpty(): boolean {
    return this.value.length === 0;
  }
}
```

**Nutzung in Komponenten**:

```typescript
// Statt: formatIban(e.target.value)
const iban = new IbanNumber(e.target.value);
setInput(iban.formatted);

// Statt: cleanIban(rawIban) in api.ts
body: JSON.stringify({ iban: new IbanNumber(rawIban).value });
```

---

## 5. Zusammenfassung der Gewinne

| Aspekt                | Vorher (Ist)                                | Nachher (DDD)                                                          |
| --------------------- | ------------------------------------------- | ---------------------------------------------------------------------- |
| **IBAN als Konzept**  | `String` überall                            | `IbanNumber` Value Object mit `countryCode()`, `bban()`, `formatted()` |
| **Validierungslogik** | In Spring-`@Service`                        | `Mod97Validator` — framework-frei, rein unit-testbar                   |
| **Externe API**       | Direkte Kopplung an `RestClient`            | Port-Interface + Adapter (austauschbar, mockbar)                       |
| **Controller**        | Enthält Orchestrierungs- und Fallback-Logik | Dünn: nur HTTP ↔ Use Case                                              |
| **Testbarkeit**       | Braucht Spring für viele Tests              | Domain komplett ohne Framework testbar                                 |
| **Fachlichkeit**      | Versteckt in technischen Schichten          | Im Code lesbar: `IbanNumber.countryCode()`, `Mod97Validator.isValid()` |
| **Persistenz**        | JPA Entity = Domain Model (vermischt)       | `IbanJpaEntity` ≠ `IbanNumber` — getrennte Verantwortung               |
| **Frontend**          | Logik in Utils + riesige Komponenten        | Domain-Layer + Presentation-Layer getrennt                             |
| **Erweiterbarkeit**   | Neue Länder → Service anfassen              | Neue Länder → `COUNTRY_LENGTHS` erweitern, ggf. neues `BankDirectory`  |

---

## 6. Umsetzungs-Reihenfolge (empfohlen)

Die Transformation kann inkrementell erfolgen, ohne den laufenden Betrieb zu brechen:

| Phase | Schritt                                                     | Aufwand | Risiko |
| ----- | ----------------------------------------------------------- | ------- | ------ |
| 1     | Value Objects erstellen (`IbanNumber`, `CountryCode`, etc.) | Klein   | Gering |
| 2     | `Mod97Validator` als Domain Service extrahieren             | Klein   | Gering |
| 3     | Port-Interfaces definieren                                  | Klein   | Gering |
| 4     | Application Use Cases erstellen + Controller verschlanken   | Mittel  | Mittel |
| 5     | `IbanJpaEntity` von Domain Model trennen + Mapper           | Mittel  | Mittel |
| 6     | Pakete umstrukturieren (Rename-Refactoring)                 | Mittel  | Gering |
| 7     | Frontend: `IbanNumber` Value Object + Hooks extrahieren     | Klein   | Gering |
| 8     | Tests anpassen (Domain-Tests ohne Spring-Kontext)           | Mittel  | Gering |

> **Empfehlung**: Phase 1–3 zuerst — sie bringen den größten Erkenntnisgewinn mit dem geringsten Risiko und können im Vorstellungsgespräch als bewusste Architekturentscheidung erklärt werden.

---

## 7. Bezug zu den Fachdokumenten

| Fachliches Konzept aus [iban.md](iban.md)                  | Abbildung im DDD-Modell                                       |
| ---------------------------------------------------------- | ------------------------------------------------------------- |
| IBAN-Aufbau: Ländercode + Prüfziffern + BBAN (Abschnitt 3) | `IbanNumber.countryCode()`, `.checkDigits()`, `.bban()`       |
| Deutsche IBAN: BLZ + Kontonummer (Abschnitt 4)             | `IbanNumber.bankIdentifier()` → `BankIdentifier` Value Object |
| Modulo-97-Algorithmus (Abschnitt 6)                        | `Mod97Validator.isValid()` — Domain Service                   |
| BLZ → Bankname (Abschnitt 4)                               | `BankDirectory` Interface + `InMemoryBankDirectory`           |
| Schreibweise / Formatierung (Abschnitt 8)                  | `IbanNumber.formatted()` — DIN-5008-Darstellung               |
| Externe API openiban.com (Abschnitt 11)                    | `ExternalIbanLookupPort` + `OpenIbanApiAdapter`               |
| BBAN / nationale Struktur (Abschnitt 3, 5)                 | `IbanNumber.bban()` — erweiterbar für andere Länder           |
| Fehlererkennungsfähigkeit (Abschnitt 7)                    | Wird durch `Mod97Validator`-Tests dokumentiert                |

---

## 8. Kritische Bewertung — Was davon wirklich umsetzen?

> Die Abschnitte 1–7 beschreiben die **theoretisch saubere** DDD-/Hexagonal-Architektur.
> Dieser Abschnitt bewertet, welche Teile für dieses konkrete Projekt **tatsächlich sinnvoll** sind — und welche Over-Engineering wären.

### 8.1 Das Projekt in Zahlen

- ~130 Zeilen Backend-Logik (Service + Controller ohne Imports/Boilerplate)
- 1 Domänenregel (Mod-97-Prüfziffer)
- 1 externer API-Aufruf (openiban.com)
- 1 Datenbank-Tabelle mit 7 Spalten
- 3 API-Endpunkte
- 0 komplexe Geschäftsprozesse, Aggregates, Domain Events, Eventual Consistency

Der vollständige DDD-Plan (Abschnitt 3) würde aus diesen ~130 Zeilen **~20+ Dateien in 10+ Packages** machen. Das ist kein Architekturgewinn — das ist Indirektion als Selbstzweck.

### 8.2 Was NICHT umgesetzt wird — und warum

| Vorgeschlagenes Pattern                        | Entscheidung | Begründung                                                                                                                                                                                                                                                                           |
| ---------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Port-Interfaces** (Output-Ports)             | NEIN         | **YAGNI** — genau eine Implementierung, kein Austausch geplant. Interface + Implementierung für `ExternalIbanApiService` verdoppelt Dateien ohne Nutzen. Falls doch nötig: Refactoring dauert 15 Minuten.                                                                            |
| **Input-Port-Interfaces** (Use Cases)          | NEIN         | Reine Zeremonie — `ValidateIbanUseCase`-Interface mit genau einer `ValidateIbanService`-Implementierung. Der Controller kann den Service direkt injizieren. Spring mockt beides gleich einfach für Tests.                                                                            |
| **JPA Entity vom Domain Model trennen**        | NEIN         | Mapping-Hölle für 7 Felder. Hin- und Rück-Mapper sind fehleranfällig, müssen bei Schemaänderungen an zwei Stellen angepasst werden und lösen kein reales Problem im aktuellen Projekt.                                                                                               |
| **10+ Package-Struktur**                       | NEIN         | `domain/model/`, `domain/service/`, `domain/port/in/`, `domain/port/out/`, `application/`, `infrastructure/persistence/`, `infrastructure/external/`, `infrastructure/bank/`, `adapter/web/dto/` — für 5 fachliche Klassen. Erhöht Navigation und Cognitive Load, nicht Verständnis. |
| **"Framework-freie Domain"** (kein `@Service`) | NEIN         | `@Service` ist eine Marker-Annotation ohne Verhalten. Den `Mod97Validator` über `@Bean` in einer Config zu registrieren ist kein Entkopplung — es ist dieselbe Framework-Kopplung an einer anderen Stelle. Spring wird nie gegen ein anderes DI-Framework getauscht.                 |
| **Frontend domain/application/infra**          | NEIN         | Vier Architekturschichten für: eine Eingabe, einen API-Call, eine Liste. `formatIban()` und `cleanIban()` als reine Funktionen sind die richtige Abstraktion für einen ~80-Zeilen-API-Layer.                                                                                         |

### 8.3 Was UMGESETZT wird — gezielte Verbesserungen

Drei Änderungen, die reale Code-Probleme lösen:

#### Umsetzen: Value Object `IbanNumber`

**Löst**: Primitive Obsession (Problem #9), Normalisierung an mehreren Stellen (Problem #2 teilweise), Fachlichkeit nicht im Typ abgebildet.

Ein `IbanNumber`-Record mit Normalisierung im Konstruktor und Methoden wie `countryCode()`, `bban()`, `bankIdentifier()`, `formatted()` ist die **einzige** Transformation, die ein echtes Problem im aktuellen Code löst. Nichts verhindert heute, dass ein nicht-normalisierter `String` als IBAN durchläuft. Das Value Object macht invalide Zustände unmöglich.

Das Value Object kann als `record` im bestehenden `model`-Package leben — keine Package-Umstrukturierung nötig.

#### Umsetzen: Orchestrierung aus Controller in Service verschieben

**Löst**: Controller enthält Use-Case-Logik (Problem #3).

`IbanController.buildResponse()` enthält die Fach-Entscheidung "lokal validieren → bei Erfolg ohne Bankname → extern nachschlagen". Diese Logik gehört in `IbanValidationService`, nicht in den Controller. Der Service bekommt `ExternalIbanApiService` injiziert und enthält die `validateWithFallback()`-Methode. Der Controller wird zu einem Einzeiler pro Endpunkt.

Kein neues Package nötig. Kein Interface nötig. Nur Logik verschieben.

#### Umsetzen: `Mod97Validator` als eigene Klasse extrahieren

**Löst**: Eigenständig testbarer Algorithmus, bessere Lesbarkeit (Problem #2 teilweise).

Der Modulo-97-Algorithmus ist ein klar abgegrenztes Fachkonzept (siehe [iban.md Abschnitt 6](iban.md)). Als eigene Klasse ist er isoliert testbar und dokumentiert sich selbst. Er kann trotzdem `@Service` oder `@Component` bleiben — die Spring-Annotation schadet nicht und die "framework-freie Domain" ist Dogma ohne Praxis-Nutzen für dieses Projekt.

### 8.4 Was NICHT umgesetzt, aber korrekt beschrieben ist

Die Abschnitte 3–4 (Ziel-Architektur, Kerntransformationen) bleiben als **Referenz** im Dokument. Sie beschreiben die korrekte DDD-Architektur und sind wertvoll für:

- **Lernzweck**: Verständnis von Hexagonaler Architektur, Ports & Adapters, Dependency Rule
- **Skalierungs-Szenario**: Falls die Domäne wächst (SEPA-Überweisungen, Sanktionsprüfung, Multi-Währung), ist der Umbau-Plan fertig dokumentiert
- **Vorstellungsgespräch**: Das Wissen zeigt sich darin, zu erkennen wann man es anwendet — und wann nicht

### 8.5 Entscheidungsprinzipien

Die Entscheidungen folgen diesen Prinzipien:

| Prinzip                              | Anwendung                                                                                                                              |
| ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------- |
| **YAGNI** (You Aren't Gonna Need It) | Interfaces erst einführen, wenn es eine zweite Implementierung oder einen klaren Test-Vorteil gibt                                     |
| **Kosten der Abstraktion**           | Jede Indirektion (Interface, Mapper, Package) hat Kosten: Navigation, Cognitive Load, Maintenance. Diese müssen den Nutzen überwiegen. |
| **Proportionale Architektur**        | Die Architektur-Komplexität sollte proportional zur Domänen-Komplexität sein. 1 Validierungsregel ≠ Hexagonale Architektur.            |
| **Reversible Entscheidungen**        | "Kein Interface jetzt" ist trivial umkehrbar (Extract Interface = 2 Minuten). "Alles aufteilen jetzt" ist teuer rückgängig zu machen.  |
| **Concrete First, Abstract Later**   | Mit konkreten Implementierungen starten. Abstrahieren, wenn ein reales Duplikat oder Austausch-Bedarf entsteht.                        |

### 8.6 Wann wäre das vollständige Hexagonal-Refactoring gerechtfertigt?

Die Architektur aus Abschnitt 3 wird sinnvoll, wenn mindestens zwei dieser Bedingungen eintreten:

- **Zweite Validierungs-API**: Neben openiban.com kommt z.B. ein SWIFT-Service → Interface lohnt sich
- **Komplexere Domänenlogik**: SEPA-Überweisungen, Sanktionslisten-Prüfung, BIC-Validierung → Domain Layer wächst
- **Mehrere Persistenz-Ziele**: Neben PostgreSQL z.B. Event Store oder Audit Log → Persistence Port lohnt sich
- **Team wächst**: Mehrere Entwickler arbeiten an verschiedenen Bounded Contexts → Package-Grenzen werden wichtig
- **Testlaufzeit problematisch**: Spring-Context-Tests dauern zu lange → Framework-freie Domain-Tests werden wertvoll

> **Faustregel**: Wenn der `service/`-Ordner mehr als ~3–4 Services mit unterschiedlichen Verantwortlichkeiten enthält, lohnt sich die Umstrukturierung.

---

## 9. Zusammenfassung

Dieses Dokument hat zwei Teile:

1. **Abschnitte 1–7**: Vollständige DDD-/Hexagonal-Architektur als Referenz und Lernmaterial
2. **Abschnitt 8**: Kritische Bewertung und pragmatische Entscheidung, was davon umgesetzt wird

**Umgesetzt werden drei gezielte Verbesserungen:**

- `IbanNumber` Value Object (löst Primitive Obsession)
- Orchestrierung vom Controller in den Service (löst falsche Verantwortlichkeit)
- `Mod97Validator` als eigene Klasse (löst fehlende Separation of Concerns)

**Nicht umgesetzt werden** Ports, Input-Port-Interfaces, JPA/Domain-Trennung, 10-Package-Struktur und Frontend-DDD-Schichten — weil die Domänen-Komplexität sie nicht rechtfertigt.

> _"The goal of software architecture is to minimize the human resources required to build and maintain the required system."_ — Robert C. Martin
>
> Für dieses Projekt bedeutet das: drei gezielte Verbesserungen statt eine vollständige Architektur-Transformation.
