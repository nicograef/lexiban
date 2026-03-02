# Anemic Domain Model, OOP und DDD — Architektur-Analyse

> Warum trennt Spring Boot Daten und Verhalten in separate Klassen — und widerspricht das nicht dem OOP-Paradigma?
> Analyse am konkreten Beispiel dieses Lexiban-Projekts mit Vorher/Nachher-Vergleich.

---

## Inhaltsverzeichnis

1. [Die Beobachtung: Daten und Verhalten sind getrennt](#1-die-beobachtung-daten-und-verhalten-sind-getrennt)
2. [Warum ist das so? — Historische Gründe](#2-warum-ist-das-so--historische-gründe)
3. [Was genau ist das Problem? — Anemic Domain Model](#3-was-genau-ist-das-problem--anemic-domain-model)
4. [Der alternative Ansatz: Rich Domain Model](#4-der-alternative-ansatz-rich-domain-model)
   - 4.9 [Exkurs: Auswirkungen des Entity-Umbaus auf die DDD-Bewertung](#49-exkurs-auswirkungen-des-entity-umbaus-auf-die-ddd-bewertung)
5. [Vollständige DDD-/Hexagonal-Architektur (Referenz)](#5-vollständige-ddd-hexagonal-architektur-referenz)
6. [Pragmatische Bewertung: Was davon wirklich umsetzen?](#6-pragmatische-bewertung-was-davon-wirklich-umsetzen)
7. [Zusammenfassung](#7-zusammenfassung)

---

## 1. Die Beobachtung: Daten und Verhalten sind getrennt

In der OOP lernt man: **Objekte kapseln Daten und Verhalten gemeinsam.** Ein `Iban`-Objekt sollte also selbst wissen, wie es sich normalisiert, validiert und seine BLZ extrahiert.

In diesem Projekt (und in den meisten Spring-Boot-Projekten) ist es aber genau andersherum aufgebaut:

### Aktuelle Architektur

```
de.nicograef.iban/
├── controller/    ← REST-Endpunkte + DTOs + Orchestrierungslogik
├── service/       ← Validierung + externe API (alles in einem Layer)
├── model/         ← JPA Entity (= reiner Datenbehälter ohne Verhalten)
├── repository/    ← Spring Data JPA
└── config/        ← CORS, Error Handling
```

### Konkretes Beispiel: `Iban.java` — nur Daten, kein Verhalten

```java
// model/Iban.java — JPA Entity
@Entity
@Table(name = "ibans")
public class Iban {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 34)
    private String iban;             // ← Nur ein String, keine Methoden

    @Column(length = 255)
    private String bankName;

    @Column(length = 20)
    private String bankIdentifier;

    private boolean valid;
    private String validationMethod;
    private Instant createdAt;

    // Getter, Constructor, kein Verhalten
}
```

Das Objekt weiß nichts über sich selbst — es weiß nicht, welches Land zur IBAN gehört, was die BLZ ist, ob die Prüfziffer stimmt, oder wie man sie formatiert.

### Konkretes Beispiel: `IbanValidationService.java` — alles Verhalten, keine eigenen Daten

```java
// service/IbanValidationService.java — enthält die gesamte IBAN-Fachlogik
@Service
public class IbanValidationService {

    private static final Map<String, String> KNOWN_BANKS = Map.of(
            "50070010", "Deutsche Bank",
            "50040000", "Commerzbank",
            "10050000", "Berliner Sparkasse");

    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.of("DE", 22);

    public ValidationResult validate(String rawIban) {
        String iban = normalize(rawIban);                  // Normalisierung
        String country = iban.substring(0, 2);             // Ländercode extrahieren
        if (!isValidMod97(iban)) { ... }                   // Prüfziffer validieren
        String bankIdentifier = iban.substring(4, 12);     // BLZ extrahieren
        String bankName = KNOWN_BANKS.get(bankIdentifier); // Bank nachschlagen
        ...
    }

    private String normalize(String input) { ... }
    private boolean isValidMod97(String iban) { ... }
}
```

**Die IBAN wird als `String` herumgereicht.** Der Service macht alles: normalisieren, parsen, validieren, BLZ extrahieren, Bank nachschlagen. Das `Iban`-Entity ist nur ein Datencontainer zum Speichern in die Datenbank.

### Dazu kommt: Controller enthält Geschäftslogik

```java
// controller/IbanController.java — validateIban() enthält Fach-Entscheidungslogik
private IbanResponse validateIban(String rawIban) {
    ValidationResult result = validationService.validate(rawIban);
    String bankName = result.bankName();
    String validationMethod = result.validationMethod();

    // Fach-Entscheidung: Lokal validieren → bei Erfolg ohne Bankname → extern nachschlagen
    if (result.valid() && bankName == null) {
        var external = externalApiService.validate(result.iban());
        if (external != null && external.bankName() != null) {
            bankName = external.bankName();
            validationMethod = "external";
        }
    }
    return new IbanResponse(result.valid(), result.iban(), bankName, ...);
}
```

Der Controller trifft hier eine **fachliche Entscheidung** ("erst lokal, dann extern als Fallback") — das gehört nicht in einen HTTP-Handler.

---

## 2. Warum ist das so? — Historische Gründe

Diese Trennung von Daten und Verhalten ist **kein Spring-Boot-Zwang**, sondern ein historisch gewachsenes Muster. Spring selbst ist agnostisch — man _kann_ Rich Domain Models bauen. Aber es gibt Gründe, warum sich das "dünne Entity + fetter Service"-Pattern durchgesetzt hat:

### 2.1 J2EE-Erbe (2000er Jahre)

In Java EE (damals J2EE) waren Entities sogenannte **EJB Entity Beans** — extrem schwergewichtig, vom Application Server verwaltet, kaum testbar. Geschäftslogik wurde deshalb bewusst in **Session Beans** (≈ Services) ausgelagert. Spring trat als leichtgewichtige Alternative an, übernahm aber das Pattern.

### 2.2 JPA/Hibernate-Einschränkungen

JPA-Entities wie `Iban.java` haben technische Anforderungen, die schlecht zu echten OOP-Objekten passen:

| Anforderung                                | Warum problematisch für OOP                                                                                |
| ------------------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| **No-Args-Constructor** (protected/public) | Hibernate instanziiert Objekte via Reflection — umgeht den normalen Konstruktor und damit jede Validierung |
| **Mutable Felder**                         | Hibernate setzt Felder per Reflection — immutable Value Objects funktionieren nicht direkt                 |
| **Proxy-Magie** (Lazy Loading)             | Hibernate erstellt Subklassen für Lazy Loading — das bricht mit `final`-Klassen oder Records               |
| **Identität über DB-ID**                   | JPA-Entities sind per Definition Datenbank-Zeilen, keine fachlichen Konzepte                               |

**Konkret in diesem Projekt**: `Iban.java` _muss_ einen `protected Iban() {}`-Konstruktor haben, damit Hibernate das Objekt per Reflection erzeugen kann. Ein Value Object, das sich im Konstruktor selbst validiert, wäre damit umgangen.

### 2.3 Transaction Script Pattern

Für einfache CRUD-Anwendungen ist das "Controller → Service → Repository"-Pattern mit dünnen Entities **pragmatisch und schnell**. Es heißt **Transaction Script** (Martin Fowler) und funktioniert so:

```
HTTP Request → Controller → Service führt Logik-Schritte prozedural aus → Repository speichert
```

Das reicht für viele Anwendungen. Es wird erst zum Problem, wenn fachliche Komplexität wächst.

### 2.4 Tutorial-Kultur

~95 % der Spring-Boot-Tutorials im Internet lehren genau dieses Pattern. Viele Java-Entwickler halten es daher für "den Spring-Boot-Weg" — obwohl es nur **ein** möglicher Weg ist.

### Analogie zu TypeScript/Node.js

Das gleiche Anti-Pattern gibt es auch in der TypeScript-Welt — es fällt nur weniger auf:

```typescript
// TypeScript-Äquivalent des Anemic Domain Model:
interface Iban {           // ← Nur Datenstruktur
  iban: string
  bankName: string | null
  valid: boolean
}

function validateIban(raw: string): Iban { ... }   // ← Logik in Funktion statt im Objekt
function formatIban(iban: string): string { ... }   // ← Noch eine Funktion
function cleanIban(iban: string): string { ... }    // ← Und noch eine
```

In Go ist das normal (Structs + Funktionen). In OOP-Sprachen wie Java wirft es die Frage auf: **Warum haben wir Klassen, wenn wir nur structs + Prozeduren schreiben?**

---

## 3. Was genau ist das Problem? — Anemic Domain Model

Martin Fowler hat dieses Muster 2003 als **Anti-Pattern** benannt: das **Anemic Domain Model**.

### 3.1 Definition

> Ein Anemic Domain Model hat Objekte, die wie echte Domain-Objekte aussehen (gleiche Namen, gleiche Felder), aber **kein Verhalten** besitzen. Die Logik lebt in separaten Service-Klassen — das ist prozeduraler Code in OOP-Verkleidung.

### 3.2 Konkret in diesem Projekt

| Was sollte die IBAN über sich selbst wissen?                  | Wo lebt es aktuell?                          | Problem                                |
| ------------------------------------------------------------- | -------------------------------------------- | -------------------------------------- |
| Wie normalisiere ich mich? (`DE89 3704...` → `DE89370400...`) | `IbanValidationService.normalize()`          | Private Methode im Service             |
| Was ist mein Ländercode?                                      | `iban.substring(0, 2)` im Service            | Hardcoded String-Slicing, wiederholbar |
| Was ist meine BLZ?                                            | `iban.substring(4, 12)` im Service           | Magische Zahlen, kein fachlicher Name  |
| Ist meine Prüfziffer korrekt?                                 | `IbanValidationService.isValidMod97()`       | Private Methode im Service             |
| Welche Bank gehört zu mir?                                    | `KNOWN_BANKS.get(bankIdentifier)` im Service | Domänenwissen in technischer Klasse    |
| Wie wird man mich formatiert dar?                             | `formatIban()` in `utils.ts` (Frontend)      | Duplizierte Logik im Frontend          |

Nichts davon steht in `Iban.java`. Nichts hindert jemanden daran, einen nicht-normalisierten `String` als IBAN durch das System zu reichen.

### 3.3 Widerspricht es OOP?

**Ja.** Die Grundidee von OOP ist, dass ein Objekt seine Daten und sein Verhalten kapselt. Wenn `Iban.java` nur Getter hat und die gesamte Logik in `IbanValidationService` lebt, haben wir:

- **Datenklasse** (Iban.java) ≈ C-Struct
- **Funktionssammlung** (IbanValidationService) ≈ C-Modul mit Funktionen

Das ist prozedurale Programmierung mit Java-Syntax.

### 3.4 Widerspricht es DDD?

**Ja.** DDD sagt klar: Domänenlogik gehört in Domänenobjekte.

| DDD-Konzept             | Was es bedeutet                                                       | Aktueller Zustand                                                    |
| ----------------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **Value Object**        | Objekt, definiert durch seine Werte, immutable, mit Verhalten         | `Iban.java` ist ein mutable JPA-Entity ohne Verhalten                |
| **Ubiquitous Language** | Fachbegriffe im Code abbilden                                         | "Ländercode", "BLZ", "BBAN", "Prüfziffer" existieren nicht als Typen |
| **Primitive Obsession** | Fachliche Konzepte als String/int durchreichen statt als eigene Typen | IBAN ist überall ein `String`                                        |

> **Hinweis**: Mit dem geplanten Entity-Umbau (IBAN als natürlicher Primary Key, siehe [iban-as-entity-refactoring.md](iban-as-entity-refactoring.md)) verschärft sich die Primitive Obsession: ein nicht-normalisierter `String` als PK würde zu Duplikaten führen (`DE89 3704...` vs. `DE89370400...`). Das `IbanNumber` Value Object löst dieses Problem direkt.

---

## 4. Der alternative Ansatz: Rich Domain Model

### 4.1 Kernidee

Statt Daten und Verhalten auf zwei Klassen zu verteilen, baut man ein **Value Object**, das beides vereint. Die IBAN _weiß_ etwas über sich selbst.

### 4.2 Vorher vs. Nachher im direkten Vergleich

**Vorher — IBAN als String im Service:**

```java
// Service: alles prozedural
String iban = rawIban.replaceAll("[\\s\\-.]", "").toUpperCase();  // Normalisierung
String country = iban.substring(0, 2);                           // Ländercode
String bankIdentifier = iban.substring(4, 12);                   // BLZ
String bankName = KNOWN_BANKS.get(bankIdentifier);               // Bank
```

**Nachher — IBAN als Value Object:**

```java
// Value Object: Fachlichkeit im Typ
IbanNumber iban = new IbanNumber("DE89 3704 0044 0532 0130 00");  // Normalisiert sich selbst
iban.countryCode()      // → CountryCode("DE")
iban.bankIdentifier()   // → Optional<BankIdentifier("37040044")>
iban.bban()             // → "370400440532013000"
iban.formatted()        // → "DE89 3704 0044 0532 0130 00"
```

### 4.3 Value Object: `IbanNumber` (Java Record)

```java
// model/IbanNumber.java — selbst-normalisierendes Value Object
public record IbanNumber(String value) {

    /**
     * Compact Constructor (Java Records):
     * Wird bei jedem `new IbanNumber(...)` automatisch ausgeführt.
     * Normalisiert den Input und prüft die Grundstruktur.
     */
    public IbanNumber {
        value = value.replaceAll("[\\s\\-.]", "").toUpperCase();
        if (value.length() < 5 || !value.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid IBAN format: " + value);
        }
    }

    /** Ländercode nach ISO 3166-1 (z.B. "DE", "AT", "CH") — siehe iban.md Abschnitt 3 */
    public CountryCode countryCode() {
        return new CountryCode(value.substring(0, 2));
    }

    /** Prüfziffern: Stellen 3–4 */
    public String checkDigits() {
        return value.substring(2, 4);
    }

    /** BBAN = Basic Bank Account Number — nationaler Teil ohne Ländercode + Prüfziffern */
    public String bban() {
        return value.substring(4);
    }

    /**
     * BLZ (Bankleitzahl) für deutsche IBANs: Stellen 5–12.
     * Gibt Optional.empty() für nicht-deutsche IBANs zurück.
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

**Entscheidende Eigenschaft**: Wer ein `IbanNumber`-Objekt hat, **weiß**, dass es normalisiert ist und die richtige Grundstruktur hat. Ein nicht-normalisierter String kann nicht als `IbanNumber` existieren.

**TypeScript-Analogie** — das gleiche Konzept:

```typescript
class IbanNumber {
  readonly value: string;

  constructor(raw: string) {
    this.value = raw.replace(/[\s\-.]/g, "").toUpperCase();
    if (!/^[A-Z]{2}[0-9]{2}[A-Z0-9]+$/.test(this.value)) {
      throw new Error(`Invalid IBAN format: ${this.value}`);
    }
  }

  get countryCode(): string {
    return this.value.slice(0, 2);
  }
  get bban(): string {
    return this.value.slice(4);
  }
  get formatted(): string {
    return this.value.replace(/(.{4})/g, "$1 ").trim();
  }
  get blz(): string | null {
    return this.countryCode === "DE" && this.value.length >= 12
      ? this.value.slice(4, 12)
      : null;
  }
}
```

Vergleiche das mit dem aktuellen Frontend-Code, wo `formatIban()` und `cleanIban()` als lose Funktionen in `utils.ts` liegen — genau das gleiche Anemic-Pattern.

### 4.4 Weitere Value Objects

```java
// Eigene Typen statt Primitive
public record CountryCode(String value) {
    public CountryCode {
        if (!value.matches("[A-Z]{2}"))
            throw new IllegalArgumentException("Invalid country code: " + value);
    }
}

public record BankIdentifier(String value) {
    public BankIdentifier {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("BankIdentifier must not be blank");
    }
}

public record BankInfo(String name, String bic) {}
```

**Warum eigene Typen?** Ohne sie kann man versehentlich eine BLZ dort übergeben, wo ein Ländercode erwartet wird — beides ist ein `String`. Mit eigenen Typen fängt der Compiler das ab.

### 4.5 Domain Service: `Mod97Validator` als eigene Klasse

**Vorher**: Private Methode `isValidMod97()` versteckt in `IbanValidationService`.

**Nachher**: Eigenständige Klasse — der Algorithmus ist ein klar abgegrenztes Fachkonzept.

```java
// service/Mod97Validator.java
@Service
public class Mod97Validator {

    private static final Map<String, Integer> COUNTRY_LENGTHS = Map.of(
        "DE", 22, "AT", 20, "CH", 21, "GB", 22
    );

    /**
     * Prüft IBAN nach ISO 7064 Modulo-97-10 Algorithmus.
     * Siehe iban.md Abschnitt 6.
     *
     * 1. Länderspezifische Länge prüfen
     * 2. Erste 4 Zeichen ans Ende verschieben
     * 3. Buchstaben → Zahlen (A=10, B=11, ..., Z=35)
     * 4. Modulo 97 berechnen (BigInteger, da 30+ Ziffern)
     * 5. Ergebnis == 1 → gültig
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
                ? Character.getNumericValue(c) : c);
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue();
    }
}
```

**Vorteil**: Isoliert testbar — man braucht keinen Spring-Kontext, um `Mod97Validator` zu testen. Als eigene Klasse dokumentiert sich der Algorithmus selbst.

### 4.6 Orchestrierung in den Service statt in den Controller

**Vorher**: `IbanController.validateIban()` enthält die Fach-Entscheidung "erst lokal validieren, dann bei Bedarf extern nachschlagen".

**Nachher**: Der Service orchestriert. Der Controller wird zum Einzeiler.

```java
// service/IbanValidationService.java — orchestriert den gesamten Use Case
@Service
public class IbanValidationService {

    private final Mod97Validator mod97Validator;
    private final ExternalIbanApiService externalApiService;

    // Constructor Injection
    public IbanValidationService(Mod97Validator mod97Validator,
                                  ExternalIbanApiService externalApiService) {
        this.mod97Validator = mod97Validator;
        this.externalApiService = externalApiService;
    }

    public ValidationResult validateWithFallback(String rawIban) {
        IbanNumber iban = new IbanNumber(rawIban);

        if (!mod97Validator.isValid(iban)) {
            return new ValidationResult(false, iban.value(), null, null, "local");
        }

        // BLZ lokal auflösen
        String bankIdentifier = iban.bankIdentifier()
            .map(BankIdentifier::value).orElse(null);
        String bankName = bankIdentifier != null
            ? KNOWN_BANKS.get(bankIdentifier) : null;
        String method = "local";

        // Fallback: externe API, wenn Bank lokal nicht bekannt
        if (bankName == null) {
            var external = externalApiService.validate(iban.value());
            if (external != null && external.bankName() != null) {
                bankName = external.bankName();
                method = "external";
            }
        }

        return new ValidationResult(true, iban.value(), bankName, bankIdentifier, method);
    }
}
```

```java
// controller/IbanController.java — dünn: nur HTTP ↔ Service
@PostMapping("/validate")
public ResponseEntity<IbanResponse> validateIban(@Valid @RequestBody IbanRequest request) {
    var result = validationService.validateWithFallback(request.iban());
    return ResponseEntity.ok(new IbanResponse(
        result.valid(), result.iban(), result.bankName(),
        result.bankIdentifier(), result.validationMethod()));
}
```

### 4.7 Vorher/Nachher-Vergleich: Was gewinnt man konkret?

| Aspekt                   | Vorher (Anemic)                                                   | Nachher (Rich Domain)                                  |
| ------------------------ | ----------------------------------------------------------------- | ------------------------------------------------------ |
| **IBAN als Konzept**     | `String` überall — Normalisierung vergessen = Bug                 | `IbanNumber` garantiert normalisierten Zustand         |
| **Fachbegriffe im Code** | `iban.substring(0, 2)`, `iban.substring(4, 12)` — magische Zahlen | `iban.countryCode()`, `iban.bankIdentifier()` — lesbar |
| **Wo lebt die Logik?**   | Verteilt: Service + Controller + Frontend-Utils                   | Value Object + dedizierter Domain Service              |
| **Controller-Aufgabe**   | HTTP + Orchestrierung + Fallback-Logik                            | Nur HTTP ↔ Service Mapping                             |
| **Testbarkeit Mod-97**   | Nur indirekt über den gesamten `IbanValidationService`            | Isoliert über `Mod97Validator`                         |
| **Primitive Obsession**  | BLZ, Ländercode, IBAN — alles `String`                            | Eigene Typen verhindern Verwechslungen                 |
| **Frontend**             | `formatIban()` + `cleanIban()` in `utils.ts`                      | Kann gleichermaßen Value Object nutzen                 |

### 4.8 Was sind die Nachteile des Rich Domain Model?

| Nachteil                      | Erklärung                                                                                                                                                                                                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Mehr Klassen**              | `IbanNumber`, `CountryCode`, `BankIdentifier`, `Mod97Validator` — statt einer Service-Klasse                                                                                                                                                                             |
| **JPA-Entity bleibt separat** | Das JPA-Entity (`Iban.java`) bleibt ein Datenbehälter — Hibernate braucht das. Man hat also Domain-Objekt + Persistenz-Objekt. Wird mit dem Entity-Umbau (IBAN als PK) aber **weniger problematisch**, da beide Objekte um denselben natürlichen Schlüssel konvergieren. |
| **Mapping nötig**             | Zwischen `IbanNumber` (Domain) und `Iban` (JPA Entity) muss hin- und hergemappt werden. Bei natürlichem PK wird das Mapping einfacher, da `IbanNumber.value()` direkt dem PK entspricht.                                                                                 |
| **Lernkurve**                 | Für Teams, die das Transaction-Script-Pattern gewohnt sind, ist der Umstieg ein Paradigmenwechsel                                                                                                                                                                        |
| **Over-Engineering-Risiko**   | Für kleine CRUD-Apps kann ein Rich Domain Model überdimensioniert sein                                                                                                                                                                                                   |

### 4.9 Exkurs: Auswirkungen des Entity-Umbaus auf die DDD-Bewertung

> Der geplante Umbau ([iban-as-entity-refactoring.md](iban-as-entity-refactoring.md)) ersetzt den synthetischen `BIGINT id` durch die IBAN selbst als Primary Key. Das hat direkte Auswirkungen auf die DDD-Analyse.

#### Die Entity/Value Object-Grenze verschiebt sich

Im aktuellen Modell gibt es eine klare Trennung:

- **Value Object** `IbanNumber` (geplant): definiert durch seinen Wert, immutable, mit Verhalten
- **Entity** `Iban.java`: identifiziert durch eine DB-generierte `Long id`, mutation durch Hibernate

Nach dem Umbau hat die gespeicherte IBAN **keine synthetische ID mehr** — sie ist durch ihren Wert (den IBAN-String) identifiziert. Streng nach DDD ist das kein klassisches Entity mehr, sondern eher ein **persistiertes Value Object**. Das `IbanNumber` Value Object und das JPA `Iban` Entity konvergieren um denselben natürlichen Schlüssel:

```java
// Domain: IbanNumber.value()  →  "DE89370400440532013000"
// JPA:    Iban.getIban()      →  "DE89370400440532013000"  (= @Id)
```

Das stärkt paradoxerweise das Argument, die JPA-Entity-Trennung **nicht** zu machen: wenn Domain-Objekt und Persistenz-Objekt fast identisch sind, ist Mapping dazwischen reine Zeremonie.

#### `IbanNumber` wird zur Normalisierungs-Garantie für den PK

Wenn die IBAN der Primary Key ist, muss **jeder Zugriff** — ob INSERT oder SELECT — den exakt gleichen normalisierten String verwenden. Ohne `IbanNumber` müsste die Normalisierung an _jeder_ Stelle passieren, die einen DB-Zugriff macht:

```java
// OHNE Value Object — Normalisierung manuell an jeder Stelle:
String normalized = rawIban.replaceAll("[^A-Za-z0-9]", "").toUpperCase();  // hier
ibanRepository.findById(normalized);                                       // richtig?
// ... und woanders:
String iban = request.iban().trim().toUpperCase();                         // anders normalisiert!
ibanRepository.save(new Iban(iban, ...));                                  // Bug: anderer Key

// MIT Value Object — Normalisierung genau einmal:
IbanNumber iban = new IbanNumber(rawIban);                                 // immer konsistent
ibanRepository.findById(iban.value());                                     // garantiert gleicher Key
```

Das ist kein theoretischer Vorteil — es ist ein **Bug-Prävention-Mechanismus**, der durch den natürlichen PK zwingend wird.

#### Neues JPA-Problem: `Persistable<String>`

Der Entity-Umbau erzeugt eine neue JPA-Eigenheit: Bei String-PKs kann Hibernate nicht automatisch unterscheiden, ob eine Entity „neu" ist (INSERT) oder „existierend" (UPDATE). Bei auto-generierten `Long id` war das trivial (`id == null` → neu). Mit String-PK muss die Entity `Persistable<String>` implementieren — ein weiteres Beispiel für die in [Abschnitt 2.2](#22-jpahibernate-einschränkungen) beschriebenen JPA-Einschränkungen.

#### Controller wird noch fetter — Orchestrierung noch dringlicher

Der Entity-Umbau fügt Lookup-Logik hinzu: vor jeder Validierung prüft der Controller, ob die IBAN schon in der DB existiert. Zusammen mit der bestehenden Validate+Fallback-Logik ergäbe das:

```
Controller heute:   HTTP + Validate-Orchestrierung
Controller nachher: HTTP + Lookup + Validate-Orchestrierung + Save
```

Das verstärkt das Argument aus [Abschnitt 4.6](#46-orchestrierung-in-den-service-statt-in-den-controller): der Service sollte eine Methode wie `validateOrLookup(IbanNumber iban)` anbieten, die den gesamten Use Case kapselt. Der Controller bleibt ein Einzeiler:

```java
@PostMapping
public ResponseEntity<IbanResponse> validateAndSave(@Valid @RequestBody IbanRequest req) {
    return ResponseEntity.ok(ibanService.validateOrLookup(new IbanNumber(req.iban())));
}
```

---

## 5. Vollständige DDD-/Hexagonal-Architektur (Referenz)

> Dieser Abschnitt beschreibt die **theoretisch vollständige** DDD-Architektur. Nicht alles davon wird umgesetzt (siehe [Abschnitt 6](#6-pragmatische-bewertung-was-davon-wirklich-umsetzen)), aber es dient als Referenz und Lernmaterial.

### 5.1 DDD-Kernkonzepte und ihre Bedeutung für dieses Projekt

| Konzept                 | Beschreibung                                                 | Relevanz für dieses Projekt                                                                                                                                                                      |
| ----------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Ubiquitous Language** | Fachbegriffe aus der Domäne im Code verwenden                | IBAN, BLZ, Prüfziffer, BBAN, Ländercode → als Typen und Methoden                                                                                                                                 |
| **Value Object**        | Definiert durch seine Werte (nicht durch eine ID), immutable | `IbanNumber`, `CountryCode`, `BankIdentifier`                                                                                                                                                    |
| **Entity**              | Objekt mit eigener Identität über die Zeit                   | Gespeicherte IBAN (wird nach dem geplanten Umbau durch ihren natürlichen Wert statt einer DB-ID identifiziert — siehe [Exkurs](#49-exkurs-auswirkungen-des-entity-umbaus-auf-die-ddd-bewertung)) |
| **Domain Service**      | Fachlogik, die keinem einzelnen Objekt gehört                | `Mod97Validator` — operiert auf IbanNumber                                                                                                                                                       |
| **Repository**          | Abstraktion für Persistenz aus Sicht der Domäne              | Port-Interface, implementiert durch JPA-Adapter                                                                                                                                                  |
| **Port**                | Interface an der Domänengrenze                               | `ExternalIbanLookupPort` — "ich brauche externe Bankinfos"                                                                                                                                       |
| **Adapter**             | Implementierung eines Ports für ein Framework/System         | `OpenIbanApiAdapter` — openiban.com REST-Aufruf                                                                                                                                                  |

### 5.2 Clean Architecture — Dependency Rule

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

**Dependency Rule**: Abhängigkeiten zeigen immer **nach innen**. Die Domain kennt weder Spring, noch JPA, noch HTTP.

### 5.3 Analogie zu TypeScript/Node.js

| Java / Spring Boot                     | TypeScript / Node.js Äquivalent                               |
| -------------------------------------- | ------------------------------------------------------------- |
| Value Object (`record IbanNumber`)     | `class IbanNumber` mit `readonly` Properties                  |
| Domain Service (kein `@Service`)       | Reines Modul/Klasse ohne Framework-Abhängigkeiten             |
| Port (Interface)                       | TypeScript `interface`                                        |
| Adapter (`@Component implements Port`) | Klasse, die Interface implementiert + per DI registriert wird |
| `@RestController` (dünn)               | Express-Route-Handler, der nur HTTP ↔ Use Case mapped         |

### 5.4 Vollständige Ziel-Paketstruktur (Backend)

```
de.nicograef.iban/
│
├── domain/                                  ← Innerster Ring: framework-frei
│   ├── model/
│   │   ├── IbanNumber.java                  # Value Object mit Verhalten
│   │   ├── CountryCode.java                 # Value Object: ISO 3166-1
│   │   ├── BankIdentifier.java              # Value Object: BLZ / Sort Code
│   │   ├── BankInfo.java                    # Value Object: Bankname + BIC
│   │   └── ValidationResult.java            # Immutable Ergebnis
│   │
│   ├── service/
│   │   ├── Mod97Validator.java              # Domain Service: Prüfziffernlogik
│   │   └── BankDirectory.java               # Interface: BLZ → BankInfo
│   │
│   └── port/
│       ├── in/
│       │   └── ValidateIbanUseCase.java     # Input-Port: "Validiere eine IBAN"
│       └── out/
│           ├── IbanPersistencePort.java     # Output-Port: Speichern/Laden
│           └── ExternalIbanLookupPort.java  # Output-Port: externe API
│
├── application/                             ← Use Cases, kennt nur domain/
│   └── ValidateIbanService.java             # Implementiert ValidateIbanUseCase
│
├── infrastructure/                          ← Adapter für externe Systeme
│   ├── persistence/
│   │   ├── IbanJpaEntity.java               # JPA Entity (≠ Domain Model!)
│   │   ├── IbanJpaRepository.java           # Spring Data Interface
│   │   └── IbanPersistenceAdapter.java      # Implementiert IbanPersistencePort
│   ├── external/
│   │   └── OpenIbanApiAdapter.java          # Implementiert ExternalIbanLookupPort
│   └── bank/
│       └── InMemoryBankDirectory.java       # Implementiert BankDirectory
│
├── adapter/web/
│   ├── IbanController.java                  # Dünn: HTTP ↔ Use Case
│   └── dto/
│       ├── IbanRequest.java                 # API-Request Record
│       └── IbanResponse.java               # API-Response Record
│
└── config/
    ├── CorsConfig.java
    └── GlobalExceptionHandler.java
```

### 5.5 Ports: Schnittstellen an der Domänengrenze

```java
// domain/port/out/ExternalIbanLookupPort.java
public interface ExternalIbanLookupPort {
    Optional<BankInfo> lookup(IbanNumber iban);
}

// domain/port/out/IbanPersistencePort.java
public interface IbanPersistencePort {
    void save(ValidationResult result);
    List<ValidationResult> findAll();
}

// domain/service/BankDirectory.java
public interface BankDirectory {
    Optional<BankInfo> resolve(BankIdentifier identifier);
}
```

**Warum Ports?** Die Domäne definiert **was** sie braucht (Interface), nicht **wie** es implementiert wird. `ExternalIbanLookupPort` könnte openiban.com oder ein Mock sein — die Domäne weiß es nicht.

**TypeScript-Analogie**: Ein `interface IbanLookup { lookup(iban: string): Promise<BankInfo | null> }` das von einem `FetchIbanLookup` oder `MockIbanLookup` implementiert wird.

> **Nach dem Entity-Umbau**: Das `IbanPersistencePort` würde um Lookup-Semantik erweitert (`Optional<ValidationResult> findByIban(IbanNumber iban)`). Das stärkt das Port-Pattern _theoretisch_, weil der Persistence-Port jetzt sowohl schreiben als auch nachschlagen kann. Praktisch bleibt es aber beim YAGNI-Urteil (siehe [Abschnitt 6.3](#63-was-nicht-umgesetzt-wird--und-warum)).

---

## 6. Pragmatische Bewertung: Was davon wirklich umsetzen?

### 6.1 Das Projekt in Zahlen

- ~130 Zeilen Backend-Logik (Service + Controller ohne Imports/Boilerplate)
- 1 Domänenregel (Mod-97-Prüfziffer)
- 1 externer API-Aufruf (openiban.com)
- 1 Datenbank-Tabelle mit 7 Spalten
- 3 API-Endpunkte
- 0 komplexe Geschäftsprozesse, Aggregates, Domain Events

Der vollständige Hexagonal-Plan aus Abschnitt 5 würde aus ~130 Zeilen **~20+ Dateien in 10+ Packages** machen. Das wäre Indirektion als Selbstzweck.

### 6.2 Was UMGESETZT wird — drei gezielte Verbesserungen

#### ✅ Value Object `IbanNumber`

**Löst**: Primitive Obsession, Normalisierung an mehreren Stellen, Fachlichkeit nicht im Typ abgebildet.

Ein `IbanNumber`-Record mit Normalisierung im Konstruktor und Methoden wie `countryCode()`, `bban()`, `bankIdentifier()`, `formatted()`. Nichts verhindert heute, dass ein nicht-normalisierter `String` als IBAN durchläuft. Das Value Object macht invalide Zustände unmöglich.

**Durch den geplanten Entity-Umbau** (IBAN als natürlicher Primary Key) wird `IbanNumber` nicht nur empfehlenswert, sondern **quasi notwendig**: Wenn die IBAN der PK ist, muss die Normalisierung _vor_ dem DB-Zugriff garantiert sein — sonst entstehen Duplikate (`DE89 3704...` vs. `DE89370400...`). Das Value Object ist die einzige Stelle, die diese Konsistenz zuverlässig sicherstellt.

Lebt als `record` im bestehenden `model`-Package — keine Umstrukturierung nötig.

#### ✅ Orchestrierung aus Controller in Service verschieben

**Löst**: Controller enthält fachliche Entscheidungslogik.

`IbanController.validateIban()` enthält die Fach-Entscheidung "lokal validieren → bei Erfolg ohne Bankname → extern nachschlagen". Diese Logik gehört in `IbanValidationService`. Der Controller wird zum Einzeiler pro Endpunkt.

**Durch den geplanten Entity-Umbau verstärkt**: Der Refactoring-Plan sieht eine zusätzliche Lookup-Logik vor (DB-Abfrage vor Validierung). Wenn beides im Controller bleibt — Lookup **und** Validate-Fallback — wird der Controller noch fetter. Stattdessen sollte der Service eine Methode `validateOrLookup(IbanNumber iban)` anbieten, die sowohl den Cache-Check als auch die Validate+Fallback-Logik kapselt. Die beiden Refactorings (DDD-Verbesserungen + Entity-Umbau) sollten daher **als ein Sprint** geplant werden.

#### ✅ `Mod97Validator` als eigene Klasse extrahieren

**Löst**: Eigenständig testbarer Algorithmus, bessere Lesbarkeit.

Der Modulo-97-Algorithmus ist ein klar abgegrenztes Fachkonzept (siehe [iban.md Abschnitt 6](iban.md)). Als eigene Klasse isoliert testbar und selbstdokumentierend.

### 6.3 Was NICHT umgesetzt wird — und warum

| Pattern                                 | Entscheidung | Begründung                                                                                                                                                                                                                                          |
| --------------------------------------- | ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Port-Interfaces** (Output-Ports)      | ❌ NEIN      | **YAGNI** — genau eine Implementierung pro Port. Interface + Implementierung verdoppelt Dateien ohne Nutzen. Falls doch nötig: Extract Interface = 15 Minuten.                                                                                      |
| **Input-Port-Interfaces** (Use Cases)   | ❌ NEIN      | Reine Zeremonie — `ValidateIbanUseCase`-Interface mit genau einer Implementierung. Der Controller kann den Service direkt injizieren.                                                                                                               |
| **JPA Entity vom Domain Model trennen** | ❌ NEIN      | Mapping-Hölle für 7 Felder. Wird durch den Entity-Umbau (natürlicher PK) sogar **noch weniger sinnvoll**: `IbanNumber` (Value Object) und `Iban` (JPA Entity) identifizieren sich beide über denselben Wert — die Kluft wird kleiner, nicht größer. |
| **10+ Package-Struktur**                | ❌ NEIN      | `domain/model/`, `domain/service/`, `domain/port/in/`, `domain/port/out/`, `application/`, `infrastructure/...` — für 5 fachliche Klassen. Erhöht Cognitive Load, nicht Verständnis.                                                                |
| **"Framework-freie Domain"**            | ❌ NEIN      | `@Service` ist eine Marker-Annotation ohne Verhalten. Spring wird nie gegen ein anderes DI-Framework getauscht.                                                                                                                                     |
| **Frontend DDD-Schichten**              | ❌ NEIN      | Vier Architekturschichten für eine Eingabe, einen API-Call, eine Liste. `formatIban()` und `cleanIban()` als reine Funktionen sind die richtige Abstraktion.                                                                                        |

### 6.4 Entscheidungsprinzipien

| Prinzip                              | Anwendung                                                                                  |
| ------------------------------------ | ------------------------------------------------------------------------------------------ |
| **YAGNI** (You Aren't Gonna Need It) | Interfaces erst bei zweiter Implementierung oder klarem Test-Vorteil                       |
| **Kosten der Abstraktion**           | Jede Indirektion hat Kosten: Navigation, Cognitive Load, Maintenance                       |
| **Proportionale Architektur**        | Architektur-Komplexität proportional zur Domänen-Komplexität                               |
| **Reversible Entscheidungen**        | "Kein Interface jetzt" = 2 Minuten Refactoring. "Alles aufteilen jetzt" = teuer rückgängig |
| **Concrete First, Abstract Later**   | Mit konkreten Implementierungen starten. Abstrahieren bei realem Bedarf                    |

### 6.5 Wann wäre das vollständige Hexagonal-Refactoring gerechtfertigt?

Die Architektur aus Abschnitt 5 wird sinnvoll, wenn mindestens zwei dieser Bedingungen eintreten:

- **Zweite Validierungs-API**: Neben openiban.com z.B. ein SWIFT-Service → Interface lohnt sich
- **Komplexere Domänenlogik**: SEPA-Überweisungen, Sanktionslisten, BIC-Validierung → Domain Layer wächst
- **Mehrere Persistenz-Ziele**: Neben PostgreSQL z.B. Event Store → Persistence Port lohnt sich
- **Team wächst**: Mehrere Entwickler an verschiedenen Bounded Contexts → Package-Grenzen werden wichtig
- **Testlaufzeit**: Spring-Context-Tests zu langsam → Framework-freie Domain-Tests werden wertvoll

> **Faustregel**: Wenn `service/` mehr als ~3–4 Services mit unterschiedlichen Verantwortlichkeiten enthält, lohnt sich die Umstrukturierung.

---

## 7. Zusammenfassung

### Die Kernfrage

> _Warum sind in Spring Boot die Daten in der Entity und das Verhalten im Service — widerspricht das nicht OOP?_

**Ja, es widerspricht OOP und DDD.** Es ist ein historisch gewachsenes Pattern (Anemic Domain Model) aus der J2EE-Ära, das durch JPA-Einschränkungen und Tutorial-Kultur zum De-facto-Standard wurde — aber kein Framework-Zwang ist.

### Was wir daraus machen

**Drei gezielte Verbesserungen** statt einer vollständigen Architektur-Transformation:

1. **`IbanNumber` Value Object** — Daten + Verhalten zusammen, invalide Zustände unmöglich. Wird durch den Entity-Umbau (IBAN als PK) sogar **notwendig** für konsistente Normalisierung.
2. **Orchestrierung in den Service** — Controller wird dünn, fachliche Logik im Service. Durch die hinzukommende Lookup-Logik des Entity-Umbaus **noch dringlicher**.
3. **`Mod97Validator` als eigene Klasse** — isoliert testbar, selbstdokumentierend

**Nicht umgesetzt** werden Ports, Input-Port-Interfaces, JPA/Domain-Trennung, Hexagonal-Packages — weil die Domäne eines IBAN-Validators mit ~130 Zeilen Logik sie nicht rechtfertigt. Der Entity-Umbau ändert an dieser Bewertung nichts; er macht die JPA/Domain-Trennung sogar _noch weniger_ sinnvoll, da Value Object und JPA Entity um denselben natürlichen Schlüssel konvergieren.

### Zusammenspiel mit dem Entity-Umbau

Die DDD-Verbesserungen und der Entity-Umbau ([iban-as-entity-refactoring.md](iban-as-entity-refactoring.md)) sind **komplementär** und sollten als ein Sprint geplant werden:

| DDD-Verbesserung             | Auswirkung durch den Entity-Umbau                                            |
| ---------------------------- | ---------------------------------------------------------------------------- |
| `IbanNumber` Value Object    | **Verstärkt** — wird quasi notwendig für konsistente PKs                     |
| Orchestrierung in Service    | **Verstärkt** — Lookup-Logik kommt hinzu, Controller würde sonst noch fetter |
| `Mod97Validator` extrahieren | **Unverändert**                                                              |
| ❌ Port-Interfaces           | **Unverändert** — weiterhin YAGNI                                            |
| ❌ JPA/Domain-Trennung       | **Noch weniger sinnvoll** — Value Object und Entity konvergieren             |
| ❌ Hexagonal-Packages        | **Unverändert**                                                              |

Insbesondere sollte `IbanNumber` **vor oder gleichzeitig** mit dem PK-Umbau implementiert werden, da es die Normalisierungs-Konsistenz sicherstellt, die der natürliche Key zwingend braucht.

### Das eigentliche Wissen

Die Kompetenz zeigt sich nicht darin, DDD-Patterns blind anzuwenden — sondern darin, zu erkennen **wann** man sie braucht und wann nicht.

> _"The goal of software architecture is to minimize the human resources required to build and maintain the required system."_ — Robert C. Martin
>
> Für dieses Projekt: drei gezielte Verbesserungen statt eine vollständige Architektur-Transformation.
