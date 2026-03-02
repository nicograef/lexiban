# DDD-Refactoring — Architektur-Analyse

> **Status: Umgesetzt.** Anemic Domain Model → Rich Domain Model (pragmatisch).
> Siehe [decisions.md](decisions.md) für ADRs.

---

## 1. Problem: Anemic Domain Model

Das Projekt hatte ein klassisches **Anemic Domain Model** (Martin Fowler, 2003): `Iban.java` war ein reiner Datenbehälter, sämtliche Fachlogik lebte in Service-Klassen, der Controller enthielt Orchestrierungslogik.

### Was die IBAN über sich selbst wissen sollte — aber nicht wusste

| Fachliche Frage | Wo lebte die Logik? | Problem |
|---|---|---|
| Wie normalisiere ich mich? (`DE89 3704...` → `DE89370400...`) | `IbanValidationService.normalize()` | Private Methode im Service |
| Was ist mein Ländercode? | `iban.substring(0, 2)` im Service | Hardcoded String-Slicing |
| Was ist meine BLZ? | `iban.substring(4, 12)` im Service | Magische Zahlen, kein fachlicher Name |
| Ist meine Prüfziffer korrekt? | `isValidMod97()` im Service | Versteckte private Methode |
| Wie formatiert man mich? | `formatIban()` in `utils.ts` (Frontend) | Duplizierte Logik |

Nichts davon stand in `Iban.java`. Nichts hinderte jemanden daran, einen nicht-normalisierten `String` als IBAN durch das System zu reichen.

### Warum war das so?

Kein Spring-Boot-Zwang, sondern historisch gewachsen:

- **J2EE-Erbe**: EJB Entity Beans waren schwergewichtig → Logik in Session Beans (≈ Services) ausgelagert. Spring übernahm das Pattern.
- **JPA-Einschränkungen**: No-Args-Constructor, mutable Felder, Proxy-Magie — schlecht kompatibel mit echten OOP-Objekten.
- **Transaction Script**: Für einfache CRUD pragmatisch und schnell. Wird erst zum Problem bei wachsender Fachlogik.
- **Tutorial-Kultur**: ~95 % der Spring-Boot-Tutorials lehren genau dieses Pattern.

---

## 2. Vorher vs. Nachher

**Vorher — IBAN als String im Service:**

```java
String iban = rawIban.replaceAll("[\\s\\-.]", "").toUpperCase();  // Normalisierung
String country = iban.substring(0, 2);                           // Ländercode
String bankIdentifier = iban.substring(4, 12);                   // BLZ
String bankName = KNOWN_BANKS.get(bankIdentifier);               // Bank
```

**Nachher — IBAN als Value Object:**

```java
IbanNumber iban = new IbanNumber("DE89 3704 0044 0532 0130 00");  // Normalisiert sich selbst
iban.countryCode()      // → "DE"
iban.bankIdentifier()   // → Optional("37040044")
iban.bban()             // → "370400440532013000"
iban.formatted()        // → "DE89 3704 0044 0532 0130 00"
```

**Entscheidende Eigenschaft**: Wer ein `IbanNumber`-Objekt hat, weiß, dass es normalisiert ist. Ein nicht-normalisierter String kann nicht als `IbanNumber` existieren.

### Controller: vorher fett, nachher dünn

**Vorher** — Controller enthielt fachliche Entscheidungslogik (erst lokal, dann extern als Fallback):

```java
private IbanResponse validateIban(String rawIban) {
    ValidationResult result = validationService.validate(rawIban);
    if (result.valid() && bankName == null) {
        var external = externalApiService.validate(result.iban());  // Fach-Entscheidung im Controller!
        ...
    }
}
```

**Nachher** — Controller ist ein Einzeiler, Service orchestriert:

```java
@PostMapping
public ResponseEntity<IbanResponse> validateAndSaveIban(@Valid @RequestBody IbanRequest request) {
    var result = ibanService.validateOrLookup(request.iban());
    return ResponseEntity.ok(new IbanResponse(result.valid(), result.iban(), result.bankName(), result.reason()));
}
```

---

## 3. Umgesetzte Verbesserungen

### ✅ Value Object `IbanNumber`

Selbst-normalisierendes Java Record mit `countryCode()`, `bankIdentifier()`, `bban()`, `formatted()`. Invalide Zustände unmöglich. Durch den Entity-Umbau (IBAN als natürlicher Primary Key, siehe [Decision 15](decisions.md)) **quasi notwendig**: ohne `IbanNumber` müsste die Normalisierung an jeder DB-Zugriffsstelle manuell passieren — sonst entstehen Duplikate.

### ✅ Orchestrierung in `IbanService`

Fachliche Logik (Cache-Lookup → lokale Validierung → externer Fallback) im Service statt im Controller. `validateOrLookup()` kapselt den gesamten Use Case. Controller ist ein Einzeiler.

### ✅ `Mod97Validator` extrahiert

ISO 7064 Algorithmus als eigene, isoliert testbare Klasse. Vorher war `isValidMod97()` eine private Methode im monolithischen `IbanValidationService`.

---

## 4. Bewusst nicht umgesetzt

| Pattern | Entscheidung | Begründung |
|---|---|---|
| **Port-Interfaces** | ❌ | YAGNI — genau eine Implementierung pro Port. Extract Interface = 15 Minuten wenn nötig. |
| **JPA/Domain-Trennung** | ❌ | Mapping-Overhead für 5 Felder. Value Object und Entity konvergieren um denselben natürlichen Schlüssel. |
| **Hexagonal-Packages** | ❌ | ~130 Zeilen Logik rechtfertigen keine 20+ Dateien in 10+ Packages. |
| **Input-Port-Interfaces** | ❌ | Reine Zeremonie — `ValidateIbanUseCase` mit genau einer Implementierung. |
| **Framework-freie Domain** | ❌ | `@Service` ist eine Marker-Annotation. Spring wird nie ausgetauscht. |

### Entscheidungsprinzipien

| Prinzip | Anwendung |
|---|---|
| **YAGNI** | Interfaces erst bei zweiter Implementierung |
| **Proportionale Architektur** | Architektur-Komplexität proportional zur Domänen-Komplexität |
| **Reversible Entscheidungen** | "Kein Interface jetzt" = 2 Min Refactoring. "Alles aufteilen jetzt" = teuer rückgängig. |
| **Concrete First, Abstract Later** | Abstrahieren bei realem Bedarf, nicht prophylaktisch |

### Wann wäre Hexagonal gerechtfertigt?

- Zweite Validierungs-API (z.B. SWIFT-Service) → Interface lohnt sich
- Komplexere Domänenlogik (SEPA, Sanktionslisten) → Domain Layer wächst
- Team wächst → Package-Grenzen werden wichtig
- Spring-Context-Tests zu langsam → Framework-freie Domain-Tests

> **Faustregel**: Wenn `service/` mehr als ~3–4 Services mit unterschiedlichen Verantwortlichkeiten enthält, lohnt sich die Umstrukturierung.
