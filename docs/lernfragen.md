# Java & Spring Boot — Lernguide

## Teil 1 — Die Java-Plattform

### Was sind JDK, JRE und JVM?

Drei Schichten, von außen nach innen:

| Schicht                            | Enthält                          | Analogie                              |
| ---------------------------------- | -------------------------------- | ------------------------------------- |
| **JDK** (Java Development Kit)     | Compiler (`javac`) + Tools + JRE | ≈ Node.js + npm + TypeScript-Compiler |
| **JRE** (Java Runtime Environment) | JVM + Standardbibliothek         | ≈ Node.js Runtime (ohne npm)          |
| **JVM** (Java Virtual Machine)     | Führt Bytecode aus               | ≈ V8-Engine in Node.js                |

Die JVM ist der Kern: Sie nimmt kompilierten **Bytecode** (`.class`-Dateien) und führt ihn aus — plattformunabhängig. Ob Linux, Mac oder Windows — derselbe Bytecode läuft überall, weil die JVM die Brücke zum Betriebssystem bildet.

In diesem Projekt nutzen wir **JDK 21** (konfiguriert in der `pom.xml`):

```xml
<properties>
    <java.version>21</java.version>
</properties>
```

### Wie funktioniert der Build-Prozess?

Zwei Schritte — ähnlich wie TypeScript → JavaScript Kompilierung:

```
  .java Dateien                    .class Dateien (Bytecode)
       │                                  │
       ▼                                  ▼
    javac (Compiler)               java (JVM startet)
  ≈ tsc (TypeScript)              ≈ node dist/index.js
```

1. **`javac`** kompiliert `.java` → `.class` (Bytecode). ≈ `tsc` kompiliert `.ts` → `.js`.
2. **`java`** startet die JVM und führt `.class`-Dateien aus. ≈ `node index.js`.

In der Praxis übernimmt **Maven** beides automatisch. `mvn package` ruft intern `javac` auf, sammelt alles in eine `.jar`-Datei (≈ ein ZIP-Archiv mit Bytecode + Dependencies), und du startest mit:

```bash
java -jar target/iban-validator-0.0.1-SNAPSHOT.jar
```

Das ist vergleichbar mit Go: `go build` erzeugt ein Binary, `./binary` startet es. Der Unterschied: Go erzeugt echten Maschinencode, Java erzeugt Bytecode, der von der JVM interpretiert/JIT-kompiliert wird.

### Compile-Zeit vs. Laufzeit: Wann passiert was?

Eine häufige Frage: Wenn `IbanApplication.java` nur **eine Zeile** hat (`SpringApplication.run(...)`) — wie entstehen daraus drei HTTP-Endpunkte? Die Antwort: Annotations wie `@RestController` oder `@GetMapping` werden bei `mvn compile` **nur als Metadaten** in den Bytecode eingebettet. Sie lösen noch nichts aus. Erst zur **Laufzeit** — wenn `SpringApplication.run()` aufgerufen wird — liest Spring diese Metadaten per **Reflection** (siehe unten) und baut daraus die gesamte Applikation zusammen.

| Phase                            | Was passiert                                                                                                                        | Analogie                                                                                           |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Compile-Zeit** (`mvn compile`) | `javac` prüft Syntax + Typen, erzeugt `.class`-Bytecode. Annotations werden als Metadaten eingebettet — **nichts wird ausgeführt**. | `tsc` — TypeScript prüft Typen, erzeugt `.js`. Aber `express.Router()` wird noch nicht aufgerufen. |
| **Laufzeit** (`java -jar ...`)   | Spring liest Annotations per Reflection, erstellt Beans, injiziert Dependencies, registriert Routes, startet Tomcat.                | `node server.js` — erst jetzt werden `app.use()`, `router.get()` etc. aufgerufen.                  |

**Trade-off:** Weil alles zur Laufzeit passiert, können Fehler (z.B. fehlende Bean, falscher Typ) erst beim **Start** auftreten — nicht schon beim Build. In Go kompiliert der Code nicht, wenn eine Dependency fehlt. In Spring kann der Build erfolgreich sein, aber die App crasht beim Starten.

**Alternative Frameworks** wie **Quarkus** und **Micronaut** lösen dieses Trade-off anders: Sie verarbeiten Annotations zur **Compile-Zeit** statt zur Laufzeit. Vorteil: schnellerer Start, Fehler früher erkannt. Nachteil: weniger Flexibilität (z.B. keine dynamischen Profile/Conditions). Spring Boot setzt bewusst auf Runtime-Flexibilität.

### Maven — der Build-Manager

Maven ist Javas Build-Tool + Dependency-Manager. Die `pom.xml` (Project Object Model) ist die zentrale Konfigurationsdatei — ≈ `package.json` in Node.js / `go.mod` in Go.

**Aufbau der `pom.xml`:**

```xml
<!-- Inherit von Spring Boot's Parent (setzt Default-Versionen für alle Dependencies) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.3</version>
</parent>

<!-- Projekt-Koordinaten — eindeutige Identifikation. ≈ npm @scope/name -->
<groupId>de.nicograef</groupId>           <!-- ≈ npm-Scope: @nicograef -->
<artifactId>iban-validator</artifactId> <!-- ≈ Paketname: validator -->
<version>0.0.1-SNAPSHOT</version>      <!-- SNAPSHOT = Entwicklungsversion -->

<!-- Dependencies ≈ "dependencies" in package.json -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- Keine Version nötig — kommt vom Parent -->
    </dependency>
</dependencies>
```

**Maven Lifecycle** (≈ npm-Scripts in Reihe):

| Befehl                | Was passiert                          | npm-Äquivalent |
| --------------------- | ------------------------------------- | -------------- |
| `mvn compile`         | `javac` kompiliert `.java` → `.class` | `tsc`          |
| `mvn test`            | Compile + Tests ausführen             | `pnpm test`    |
| `mvn package`         | Compile + Test + `.jar` erzeugen      | `pnpm build`   |
| `mvn clean`           | `target/`-Ordner löschen              | `rm -rf dist/` |
| `mvn spring-boot:run` | App im Dev-Modus starten              | `pnpm dev`     |

Der `target/`-Ordner ist das Ergebnis des Builds (≈ `dist/` oder `build/`). Die `.jar`-Datei darin ist ein self-contained Archiv mit eingebettetem Tomcat — eine einzelne Datei, die du deployen und starten kannst.

### Wie funktioniert die JVM zur Laufzeit?

Die JVM ist kein einfacher Interpreter — sie optimiert aktiv:

1. **Bytecode laden** — ClassLoader lädt `.class`-Dateien
2. **Interpretieren** — Bytecode wird zunächst interpretiert (schneller Start)
3. **JIT-Kompilierung** — Häufig ausgeführter Code ("Hot Code") wird zur Laufzeit in nativen Maschinencode kompiliert (≈ V8's TurboFan in Node.js)
4. **Garbage Collection** — Automatische Speicherbereinigung (siehe nächster Abschnitt)

**Warum ist das relevant?** Java-Apps starten langsamer als Go-Binaries (JVM muss hochfahren, Klassen laden), sind aber im Dauerbetrieb durch JIT-Optimierung oft genauso schnell. Spring Boot startet in 2–5 Sekunden — merkbar länger als `node server.js`, aber im Produktionsbetrieb dann sehr performant.

### Garbage Collection, Stack und Heap

**Stack** und **Heap** sind zwei Speicherbereiche:

|                     | Stack                                      | Heap                                   |
| ------------------- | ------------------------------------------ | -------------------------------------- |
| **Was**             | Lokale Variablen, Methodenaufrufe          | Objekte, Arrays                        |
| **Lebensdauer**     | Automatisch freigegeben nach Method-Return | Garbage Collector räumt auf            |
| **Geschwindigkeit** | Sehr schnell (LIFO-Struktur)               | Langsamer (komplexe Verwaltung)        |
| **Analogie**        | Wie der Call Stack in JS DevTools          | Wie der Heap in Chrome Memory Profiler |

```java
public ValidationResult validate(String rawIban) {
    String iban = normalize(rawIban);  // "iban" → Referenz auf Stack, String-Objekt auf Heap
    boolean valid = isValidMod97(iban); // "valid" → primitiver Wert auf Stack (kein Heap nötig)
    return new ValidationResult(...);   // neues Objekt auf Heap, Referenz wird zurückgegeben
}
```

Die **Garbage Collection** (GC) räumt nicht mehr referenzierte Objekte vom Heap. Das passiert automatisch — du musst nie `free()` oder `delete` aufrufen. In Node.js funktioniert das identisch (V8 hat auch eine GC). In Go gibt es ebenfalls eine GC. Der Unterschied zu C/C++: Dort musst du Speicher manuell verwalten.

---

## Teil 2 — Die Sprache Java

### Sichtbarkeit: public, private, protected

In Java hat jede Klasse, jedes Feld und jede Methode einen **Access Modifier**:

| Modifier                            | Sichtbar für                                    | Projekt-Beispiel                                                                         |
| ----------------------------------- | ----------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `public`                            | Alle                                            | `public ValidationResult validate(...)` — API der Service-Klasse                         |
| `private`                           | Nur die eigene Klasse                           | `private boolean isValidMod97(...)` — internes Implementierungsdetail                    |
| `protected`                         | Eigene Klasse + Unterklassen + gleiches Package | `protected Iban() {}` — JPA braucht den Konstruktor, aber extern soll ihn niemand nutzen |
| _(kein Modifier)_ "package-private" | Nur im selben Package                           | Kommt im Projekt bewusst nicht vor — entweder `public` oder `private`                    |

**Faustregel** (identisch zu TypeScript-Klassen):

- Alles so restriktiv wie möglich → `private` als Default
- Öffentliche API-Methoden → `public`
- Felder fast immer `private`, Zugriff über Getter

### static vs. non-static

`static` bedeutet: **gehört zur Klasse, nicht zu einer Instanz**. ≈ In TypeScript: eine Funktion auf Modul-Ebene vs. eine Methode auf einer Instanz.

```java
// IbanValidationService.java
private static final Map<String, String> KNOWN_BANKS = Map.of(...);
//      ^^^^^^         ≈ const KNOWN_BANKS = { ... } auf Modul-Ebene in TS
//      Existiert genau 1x, egal wie viele Instanzen der Klasse erstellt werden

public ValidationResult validate(String rawIban) { ... }
//     Nicht-static → wird auf einer Instanz aufgerufen: service.validate(...)
//     ≈ Methode auf einem Objekt in TS/Go
```

|                        | static                                       | non-static                      |
| ---------------------- | -------------------------------------------- | ------------------------------- |
| **Gehört zu**          | Der Klasse selbst                            | Einer Instanz                   |
| **Zugriff**            | `ClassName.method()`                         | `instance.method()`             |
| **Zugriff auf `this`** | ❌ Nein                                      | ✅ Ja                           |
| **Typischer Einsatz**  | Konstanten, Utility-Methoden, `main()`       | Business-Logik, Services        |
| **TS-Analogie**        | Top-level `export const` / `export function` | Methode auf einer Klasse/Objekt |

In Spring Boot sind Services Singletons (es gibt nur eine Instanz), daher ist der Unterschied zwischen `static` und non-static bei Methoden weniger relevant. Konstanten wie `KNOWN_BANKS` werden aber immer `static final` gemacht.

### final, const und Immutabilität

Java hat kein `const` Keyword wie TypeScript. Stattdessen:

| Java             | TypeScript              | Go                | Bedeutung                                           |
| ---------------- | ----------------------- | ----------------- | --------------------------------------------------- |
| `final` Variable | `const` / `readonly`    | —                 | Referenz kann nicht neu zugewiesen werden           |
| `final` Klasse   | —                       | —                 | Klasse kann nicht vererbt werden (≈ `sealed` in TS) |
| `final` Methode  | —                       | —                 | Methode kann nicht überschrieben werden             |
| `static final`   | `const` auf Modul-Ebene | `const`           | Klassen-Konstante                                   |
| Record           | `Readonly<T>`           | Struct (by value) | Immutable Datenstruktur                             |

```java
// IbanController.java
private final IbanValidationService validationService;
//      ^^^^^  ≈ readonly in TS — kann nach Konstruktor nicht neu zugewiesen werden

// IbanValidationService.java
private static final Map<String, String> KNOWN_BANKS = Map.of(...);
//             ^^^^^  ≈ const KNOWN_BANKS = Object.freeze({...}) in TS
```

**Wichtig**: `final` schützt nur die **Referenz**, nicht den **Inhalt**. Eine `final List<String>` kann nicht auf eine andere Liste zeigen, aber die Elemente der Liste können geändert werden. Für echte Immutabilität nutzt man `Map.of()`, `List.of()` (unmodifizierbare Collections) oder Records.

### Was ist Reflection?

Reflection erlaubt es, zur Laufzeit Klassen zu inspizieren und zu manipulieren — Felder lesen, Methoden aufrufen, Objekte erstellen, ohne den konkreten Typ zur Compile-Zeit zu kennen.

```java
// Pseudo-Code — was Hibernate intern bei Entity-Loading tut:
Class<?> clazz = Class.forName("de.nicograef.iban.model.Iban");  // Klasse über Namen laden
Object entity = clazz.getDeclaredConstructor().newInstance();  // new Iban() per Reflection
Field field = clazz.getDeclaredField("iban");
field.setAccessible(true);  // Umgeht "private"!
field.set(entity, "DE89370400440532013000");  // Setzt Wert ohne Setter
```

**Warum ist das wichtig?** Drei Schlüssel-Mechanismen in diesem Projekt nutzen Reflection:

1. **Hibernate/JPA** — lädt Daten aus der DB und setzt die Felder der Entity per Reflection. Deshalb braucht `Iban.java` den leeren `protected Iban() {}` Konstruktor und mutable Felder (keine Records als Entities möglich — dazu mehr in Teil 3).
2. **Spring DI** — findet Klassen mit `@Service`/`@Controller` per Classpath-Scanning, liest deren Konstruktor-Parameter und injiziert die richtigen Beans.
3. **Route-Registrierung** — Spring liest `@RequestMapping`, `@GetMapping`, `@PostMapping` per Reflection und registriert die Methoden als HTTP-Handler im Tomcat.

```java
// Pseudo-Code — was Spring beim Start intern macht, um Routes zu registrieren:
Class<?> clazz = IbanController.class;

// 1. Hat die Klasse @RestController? → als Bean registrieren
if (clazz.isAnnotationPresent(RestController.class)) {
    // 2. Basis-Pfad aus @RequestMapping lesen
    String basePath = clazz.getAnnotation(RequestMapping.class).value(); // "/api/ibans"

    // 3. Alle Methoden durchgehen, HTTP-Annotations suchen
    for (Method m : clazz.getMethods()) {
        if (m.isAnnotationPresent(PostMapping.class)) {
            String subPath = m.getAnnotation(PostMapping.class).value(); // "/validate"
            // → POST /api/ibans/validate → ruft m.invoke(controllerInstance, ...) auf
            tomcat.registerRoute("POST", basePath + subPath, m);
        }
        if (m.isAnnotationPresent(GetMapping.class)) {
            // → GET /api/ibans → ruft getAllIbans() auf
            tomcat.registerRoute("GET", basePath, m);
        }
    }
}
```

**Express-Analogie:** Stell dir vor, Express würde automatisch alle Dateien in `src/` scannen und jede Funktion mit einem `// @get /users`-Kommentar als Route registrieren — ohne dass du `router.get("/users", handler)` schreibst. Genau das macht Spring per Reflection.

In TS/JS gibt es kein echtes Reflection, aber `Reflect.metadata` (TypeScript Decorators) kommt dem nahe. Go hat das `reflect`-Package.

---

## Teil 3 — Datenmodellierung in Java

### Getter und Setter

Java-Convention: Felder sind `private`, Zugriff läuft über `getX()` / `setX()` Methoden. ≈ Property-Accessor in TypeScript, aber explizit.

```java
// Iban.java — Getter (Setter gibt es in unserem Projekt nicht, da Entities nur geschrieben werden)
private String iban;                    // ≈ private iban: string;
public String getIban() { return iban; } // ≈ get iban() { return this._iban; }
```

**Warum nicht einfach `public` Felder?** Drei Gründe:

1. **Kapselung** — du kannst die interne Repräsentation ändern, ohne die API zu brechen
2. **Frameworks erwarten es** — Jackson (JSON) und Hibernate nutzen Getter/Setter, um Felder zu lesen/schreiben
3. **Java-Convention** — die gesamte Toolchain (IDEs, Libraries) baut auf diesem Pattern auf

Es gibt **keine eingebaute Short-Syntax** für Getter/Setter in Java-Klassen. Aber es gibt zwei Alternativen:

### Lombok — das Boilerplate-Killer-Tool (nicht in diesem Projekt)

Lombok ist eine Compile-Time-Library, die per Annotation Getter, Setter, Konstruktoren etc. generiert:

```java
// MIT Lombok (wir nutzen das NICHT):
@Data  // Generiert: Getter, Setter, equals(), hashCode(), toString()
public class Iban {
    private String iban;
    private String bankName;
}

// OHNE Lombok (so machen wir es — explizite Getter):
public class Iban {
    private String iban;
    public String getIban() { return iban; }
    // ... usw.
}
```

Wir verzichten bewusst auf Lombok, weil: es eine zusätzliche Dependency ist, der generierte Code unsichtbar ist, und für Lernzwecke der explizite Code verständlicher. In vielen Produktions-Projekten ist Lombok allerdings Standard.

### Records — Javas Antwort auf TypeScript `type`

Seit Java 16. **Immutable Datenklassen** ohne jeglichen Boilerplate. Der Compiler generiert automatisch: Konstruktor, Getter (ohne `get`-Prefix), `equals()`, `hashCode()`, `toString()`.

```java
// IbanController.java — DTO als Record
record IbanResponse(boolean valid, String iban, String bankName,
                    String bankIdentifier, String validationMethod) {}

// TS-Äquivalent:
// type IbanResponse = { valid: boolean; iban: string; bankName: string; ... }
```

**Zugriff auf Felder**: `response.valid()`, `response.iban()` — ohne `get`-Prefix.

**Records vs. Klassen — wann was?**

|                        | Record                                                  | Klasse                            |
| ---------------------- | ------------------------------------------------------- | --------------------------------- |
| **Mutierbar?**         | ❌ Nein                                                 | ✅ Ja                             |
| **Boilerplate**        | Null                                                    | Getter/Setter/Konstruktor manuell |
| **Einsatz im Projekt** | DTOs: `IbanRequest`, `IbanResponse`, `ValidationResult` | JPA Entity: `Iban.java`           |
| **Vererbung**          | ❌ Kann nicht erweitert werden                          | ✅ Kann erweitert werden          |

**Warum kann die Entity `Iban.java` kein Record sein?** Weil Hibernate Reflection nutzt (siehe Teil 2): leerer Konstruktor, dann Felder einzeln setzen. Records sind immutable — das geht nicht.

### DTOs — Data Transfer Objects

DTOs sind Objekte, die **nur Daten transportieren** — zwischen Schichten (Controller ↔ Service) oder zwischen Client und Server (JSON). Sie enthalten keine Business-Logik.

```java
// IbanController.java — drei DTOs für drei verschiedene Zwecke:
record IbanRequest(@NotBlank String iban) {}          // Eingehend: was der Client schickt
record IbanResponse(boolean valid, String iban, ...) {} // Ausgehend: was der Client bekommt
record IbanListEntry(Long id, String iban, ...) {}      // Ausgehend: für die IBAN-Liste
```

**Warum nicht einfach die Entity direkt als JSON senden?** Weil du kontrollieren willst, welche Felder der Client sieht. Die Entity hat z.B. `createdAt` — das muss nicht in jeder Response sein. In TS/Node ist das das gleiche Prinzip: du schickst selten das Prisma-Model 1:1 als JSON, sondern mappst auf ein Response-Objekt.

### Nested Records — wo und warum?

Records müssen **nicht** in einer eigenen Datei stehen. In Java kann man sie als **Top-Level-Klasse** (eigene `.java`-Datei) oder als **Nested Record** (innerhalb einer Klasse) definieren — beides funktioniert identisch. Die Wahl ist eine **Design-Entscheidung**, kein Sprachzwang.

```typescript
// TS-Analogie: Top-Level vs. Inline-Typ
// Top-Level — eigene Datei, exportiert, wiederverwendbar:
// types/iban.ts
export type IbanResponse = { valid: boolean; iban: string };

// Inline — nur in dieser Route, nicht exportiert:
// routes/iban.ts
type IbanRequest = { iban: string }; // Nur hier gebraucht
```

Im Projekt gibt es beide Varianten:

| Record                                         | Wo definiert?                      | Modifier  | Warum?                                                          |
| ---------------------------------------------- | ---------------------------------- | --------- | --------------------------------------------------------------- |
| `IbanRequest`, `IbanResponse`, `IbanListEntry` | Nested in `IbanController`         | `public`  | Erscheinen in `public` Method-Signaturen → müssen `public` sein |
| `ValidationResult`                             | Nested in `IbanValidationService`  | `public`  | Wird vom Controller importiert (anderes Package!)               |
| `ExternalValidationResult`                     | Nested in `ExternalIbanApiService` | `public`  | Wird vom Controller importiert                                  |
| `OpenIbanResponse`, `BankData`                 | Nested in `ExternalIbanApiService` | `private` | Interne DTOs für die openiban.com-API, nur dort gebraucht       |

**Faustregel:**

- **Nested + `public`** → DTO erscheint in `public` Method-Signaturen oder wird von anderen Packages importiert (z. B. `IbanRequest`/`IbanResponse` im Controller, `ValidationResult` im Service)
- **Nested + `private`** → DTO wird nur innerhalb der eigenen Klasse gebraucht und taucht in keiner öffentlichen Signatur auf (z. B. `OpenIbanResponse`, `BankData` im `ExternalIbanApiService`)
- **Top-Level (eigene Datei)** → DTO wird von vielen Klassen in verschiedenen Kontexten gebraucht, oder ist selbst komplex genug für eine eigene Datei

### Access Modifier bei Nested Records

Ohne expliziten Modifier ist ein nested Record **package-private** — sichtbar für alle Klassen im selben Package. Die richtige Sichtbarkeit hängt davon ab, wo der Record referenziert wird:

```java
// ❌ Package-private (Default) — oft zu breit oder zu schmal
record IbanRequest(@NotBlank String iban) {}

// ✅ public — der Record taucht in einer public Method-Signatur auf
public record IbanRequest(@NotBlank String iban) {}

// ✅ private — rein internes DTO, nicht in öffentlicher Signatur
private record OpenIbanResponse(boolean valid, BankData bankData) {}
```

**Wichtig:** Wenn ein Record als Parameter oder Rückgabetyp einer `public`-Methode auftritt, **muss** er ebenfalls `public` sein — sonst exportiert man einen nicht-öffentlichen Typ über eine öffentliche API (Code-Smell). Jackson/Spring kann zwar `private` Records per Reflection serialisieren, aber die **Code-Sichtbarkeit** sollte konsistent mit der API-Sichtbarkeit sein.

**Warum sind die Controller-Records `public`?** Weil sie in den `public` Methoden-Signaturen vorkommen (`ResponseEntity<IbanResponse>`, `@RequestBody IbanRequest`).

**Warum ist `ValidationResult` public?** Weil der Controller es importiert:

```java
// IbanController.java (Package: controller)
import de.nicograef.iban.service.IbanValidationService.ValidationResult;
//                         ^^^^^^^ anderes Package → braucht public
```

Ohne `public` wäre `ValidationResult` nur innerhalb von `de.nicograef.iban.service` sichtbar — der Import aus dem `controller`-Package würde nicht kompilieren.

### Jackson — JSON-Serialisierung

Jackson ist die Standard-JSON-Library in Spring Boot. Sie konvertiert Java-Objekte ↔ JSON automatisch.

```
Client schickt:          { "iban": "DE89..." }
                              ↓ Jackson deserialisiert
Java-Objekt:             IbanRequest("DE89...")       ← @RequestBody

Server antwortet:        IbanResponse(true, "DE89...", ...)
                              ↓ Jackson serialisiert
JSON an Client:          { "valid": true, "iban": "DE89...", ... }
```

**Wie matcht Jackson Felder?** Per Name: das JSON-Feld `"iban"` wird auf den Record-Parameter `iban` gemappt. Bei Klassen nutzt Jackson die Getter-Methoden: `getIban()` → JSON-Feld `"iban"`, `isValid()` → `"valid"`.

In TypeScript: `JSON.parse()` + `JSON.stringify()` — aber ohne Typsicherheit. Jackson gibt dir beides: Serialisierung + automatisches Mapping auf typisierte Objekte. ≈ `zod.parse()` mit automatischem Serialisieren.

---

## Teil 4 — Vor Spring Boot: Der Java-Web-Stack

### Wie hat man Java-Backends vor Spring Boot gebaut?

Kurze Zeitreise, damit du Spring Boot's Mehrwert verstehst:

**Phase 1: Servlets (seit 1997)** — Javas erste Web-API. Du schreibst Klassen, die HTTP-Requests verarbeiten, deployed auf einen externen **Application Server** (Tomcat, JBoss, WebLogic). Viel XML-Konfiguration, viel Boilerplate.

```java
// So sah ein Servlet aus (ohne Spring):
public class IbanServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String iban = req.getParameter("iban");
        // Manuell: JSON parsen, DB-Connection holen, SQL schreiben, Response bauen...
        resp.getWriter().write("{\"valid\": true}");
    }
}
```

**Phase 2: Spring Framework (seit 2003)** — brachte Dependency Injection und reduzierte den Boilerplate. Aber: immer noch viel XML-Konfiguration, externer Tomcat nötig.

**Phase 3: Spring Boot (seit 2014)** — Convention over Configuration. **Embedded Tomcat**, Auto-Konfiguration, Starter-Dependencies, kein XML. Genau das nutzen wir.

### Was ist Tomcat?

Tomcat ist ein **Java Servlet Container** — ein HTTP-Server, der Java-Code ausführen kann. ≈ Express.js, aber auf einer niedrigeren Ebene.

In Spring Boot ist Tomcat **eingebettet** (embedded): Er steckt in deiner `.jar`-Datei und startet automatisch. Du musst ihn nicht separat installieren oder konfigurieren.

```properties
# application.properties — das ist die gesamte Tomcat-Konfiguration:
server.port=8080
```

Wenn `SpringApplication.run()` aufgerufen wird (`IbanApplication.java`), startet Spring den eingebetteten Tomcat, registriert alle Controller als Request-Handler und lauscht auf Port 8080.

**REST vs. SOAP**: Dieses Projekt nutzt REST (JSON über HTTP-Methoden) — genau wie in Express.js/Go. SOAP ist der ältere, XML-basierte Enterprise-Standard mit formalem Schema (WSDL). In modernen Projekten kaum noch relevant.

### JDBC — der Low-Level-Datenbankzugriff

JDBC (Java Database Connectivity) ist Javas **Standard-API für Datenbankzugriffe**. ≈ `pg`-Package in Node.js — du schreibst SQL von Hand.

```java
// So sähe IBAN-Speichern mit purem JDBC aus (OHNE JPA/Hibernate):
Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/iban");
PreparedStatement stmt = conn.prepareStatement(
    "INSERT INTO ibans (iban, bank_name, valid, validation_method, created_at) VALUES (?, ?, ?, ?, ?)");
stmt.setString(1, "DE89370400440532013000");
stmt.setString(2, "Commerzbank");
stmt.setBoolean(3, true);
stmt.setString(4, "local");
stmt.setTimestamp(5, Timestamp.from(Instant.now()));
stmt.executeUpdate();
conn.close();
```

**Viel Boilerplate**, Error-Handling, Connection-Management. Deshalb nutzt man in der Praxis fast immer ein ORM darüber.

### Jakarta, JPA und Hibernate — der ORM-Stack

Drei Begriffe, die zusammengehören:

1. **Jakarta EE** (früher Java EE) — ein Satz von **Spezifikationen** (≈ Interfaces/Standards) für Enterprise-Java. Jakarta Persistence, Jakarta Validation, Jakarta Servlet usw. ≈ W3C-Standards für das Web — sie beschreiben _was_, nicht _wie_.

2. **JPA** (Jakarta Persistence API) — die Spezifikation für ORMs in Java. Definiert Annotations wie `@Entity`, `@Id`, `@Column`. ≈ Ein Interface, das beschreibt wie ein ORM aussehen soll.

3. **Hibernate** — die populärste **Implementierung** von JPA. ≈ Prisma/TypeORM implementiert die ORM-Konzepte, Hibernate implementiert die JPA-Spezifikation.

```
JPA (Spezifikation) → Hibernate (Implementierung) → JDBC (Low-Level-Zugriff) → PostgreSQL
≈ Fetch-API (Standard) → axios (Library) → HTTP (Protokoll) → Server
```

**Ist das immer ein ORM?** JPA/Hibernate ist der De-facto-Standard in Spring Boot. Aber es gibt Alternativen:

| Ansatz                   | Beispiel                      | Analogie                 |
| ------------------------ | ----------------------------- | ------------------------ |
| **JPA/Hibernate** (ORM)  | `ibanRepository.save(entity)` | Prisma, TypeORM, GORM    |
| **Spring JDBC Template** | SQL + automatisches Mapping   | `pg` mit Hilfsfunktionen |
| **jOOQ**                 | Type-safe SQL DSL             | Drizzle ORM (TS)         |
| **MyBatis**              | SQL in XML/Annotations        | Raw SQL mit Mapping      |

Spring unterstützt auch **NoSQL** (MongoDB, Redis, Elasticsearch) über separate Starter. Das Repository-Pattern bleibt gleich — statt `JpaRepository` nutzt du z.B. `MongoRepository`.

---

## Teil 5 — Spring Boot

### Was ist Spring Boot und warum ist es so beliebt?

Spring Boot ist ein **Framework auf dem Spring Framework**, das die Konfiguration automatisiert. Drei Kernprinzipien:

1. **Auto-Configuration** — füge `spring-boot-starter-web` zur `pom.xml` hinzu und Spring konfiguriert automatisch: Tomcat, Jackson, Routing. ≈ Express + body-parser + router in einem `npm install`.

2. **Embedded Server** — kein externer Tomcat nötig. Die App ist eine einzelne `.jar`-Datei. ≈ `node server.js` statt Deployment auf Apache.

3. **Starter Dependencies** — ein `starter` bringt alles mit, was du für ein Feature brauchst.

```xml
<!-- pom.xml — ein Starter, alles drin: -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Bringt mit: Tomcat + Spring MVC + Jackson. ≈ npm install express -->
</dependency>
```

### Was passiert bei `SpringApplication.run()`?

`IbanApplication.java` hat nur **eine Zeile** in der `main()`-Methode — aber diese eine Zeile löst eine ganze Kette von Automatismen aus:

```
SpringApplication.run(IbanApplication.class, args)
  │
  ├── 1. Component Scan
  │     Spring durchsucht alle Packages ab de.nicograef.iban per Reflection
  │     nach annotierten Klassen (@RestController, @Service, @Repository, @Configuration)
  │     ≈ Express scannt automatisch src/ nach Dateien mit export default router
  │
  ├── 2. Bean-Erstellung + Dependency Injection
  │     Gefundene Klassen werden instanziiert, Constructor-Parameter aufgelöst
  │     ≈ In Go: repo := NewRepo(db); svc := NewService(); ctrl := NewController(svc, repo)
  │     Spring macht das automatisch per Reflection
  │
  ├── 3. Route-Registrierung
  │     @RequestMapping, @GetMapping, @PostMapping werden gelesen und im Tomcat registriert
  │     ≈ app.use("/api/ibans", router); router.post("/validate", handler)
  │
  ├── 4. Flyway Migrations
  │     SQL-Dateien aus db/migration/ werden gegen die DB ausgeführt
  │     ≈ prisma migrate deploy / golang-migrate up
  │
  └── 5. Embedded Tomcat starten
        HTTP-Server lauscht auf Port 8080
        ≈ app.listen(8080)
```

All das passiert zur **Laufzeit**, nicht zur Compile-Zeit (siehe Teil 1 → "Compile-Zeit vs. Laufzeit"). Die Annotations (`@RestController`, `@Service` etc.) sind beim Kompilieren nur passive Metadaten. Erst `SpringApplication.run()` liest sie per Reflection und setzt alles zusammen.

**Das Prinzip heißt „Convention over Configuration":** Du sagst Spring _was_ du willst (über Annotations), und Spring kümmert sich um das _wie_. In Express/Go machst du beides selbst — expliziter, aber mehr Boilerplate.

### Was ist ein Bean?

Ein **Bean** ist ein Objekt, das Spring erstellt und verwaltet. Think of it as ein Eintrag in einem zentralen Container (dem "Application Context"), der Objekte auf Abruf bereitstellt.

In unserem Projekt gibt es diese Beans:

- `IbanController` (wegen `@RestController`)
- `IbanValidationService` (wegen `@Service`)
- `ExternalIbanApiService` (wegen `@Service`)
- `IbanRepository` (wegen `extends JpaRepository` — Spring generiert eine Implementierung)
- `GlobalExceptionHandler` (wegen `@RestControllerAdvice`)
- `CorsConfig` + der `WebMvcConfigurer` darin (wegen `@Configuration` + `@Bean`)

**Analogie**: In Angular sind Services auch DI-verwaltete Singletons. In Go/TS-Projekten erstellst du diese Objekte manuell in `main()` / `index.ts` und reichst sie weiter.

### Dependency Injection und Inversion of Control

**Inversion of Control (IoC)** = du erstellst deine Abhängigkeiten nicht selbst, sondern jemand anderes gibt sie dir. Das "Kontroll"-Prinzip ist umgekehrt.

**Dependency Injection (DI)** = eine konkrete Umsetzung von IoC: der Container (Spring) "injiziert" Abhängigkeiten in deinen Konstruktor.

```java
// OHNE DI (≈ normales Express/Go):
public class IbanController {
    private final IbanValidationService validationService;
    public IbanController() {
        this.validationService = new IbanValidationService();  // ← Du erstellst es selbst
    }
}

// MIT DI (Spring):
public class IbanController {
    private final IbanValidationService validationService;
    public IbanController(IbanValidationService validationService) {
        this.validationService = validationService;  // ← Spring gibt es dir
    }
}
```

**Vorteil**: In Tests kannst du ein Mock übergeben, ohne den Controller-Code zu ändern. Und wenn `IbanValidationService` selbst Abhängigkeiten hat, löst Spring die gesamte Kette auf.

### @Component, @Service, @Repository, @Controller

Vier Annotations, die alle dasselbe tun: **die Klasse als Bean registrieren**. Der Unterschied ist rein semantisch:

| Annotation                        | Bedeutung                                           | Projekt-Beispiel                                  |
| --------------------------------- | --------------------------------------------------- | ------------------------------------------------- |
| `@Component`                      | Generische Bean (Basis für alle anderen)            | —                                                 |
| `@Service`                        | Business-Logik                                      | `IbanValidationService`, `ExternalIbanApiService` |
| `@Repository`                     | Datenzugriff (+ automatische Exception-Übersetzung) | `IbanRepository` (implizit durch `JpaRepository`) |
| `@Controller` / `@RestController` | HTTP-Request-Handler                                | `IbanController`                                  |

Technisch könnte man überall `@Component` schreiben — aber die spezifischen Annotations machen den Code lesbar und ermöglichen Framework-spezifisches Verhalten (z.B. übersetzt `@Repository` SQL-Exceptions automatisch in Spring-Exceptions).

### Injection-Varianten

Drei Wege, eine Dependency zu injizieren:

```java
// 1. Constructor Injection (✅ Best Practice — nutzen wir im Projekt)
public class IbanController {
    private final IbanValidationService validationService;
    public IbanController(IbanValidationService validationService) {
        this.validationService = validationService;
    }
}

// 2. Field Injection (❌ nicht empfohlen)
public class IbanController {
    @Autowired  // Spring setzt das Feld per Reflection
    private IbanValidationService validationService;
}

// 3. Setter Injection (selten genutzt)
public class IbanController {
    private IbanValidationService validationService;
    @Autowired
    public void setValidationService(IbanValidationService validationService) {
        this.validationService = validationService;
    }
}
```

**Warum Constructor Injection?**

- Felder können `final` sein → Immutabilität
- Abhängigkeiten sind explizit sichtbar
- Objekt ist nach Konstruktor-Aufruf vollständig initialisiert
- Testbar ohne Spring (einfach `new Controller(mockService)`)

**@Autowired vs. @Inject vs. @Resource**: Drei Wege, dasselbe zu sagen.

- `@Autowired` — Spring-spezifisch, am häufigsten gesehen
- `@Inject` — Jakarta-Standard (≈ framework-agnostisch)
- `@Resource` — Jakarta-Standard, sucht per Name statt per Typ
- Bei **Constructor Injection** brauchst du **keine davon** — Spring erkennt automatisch, dass der einzige Konstruktor Injection braucht.

### Bean Scopes

Wie lange lebt ein Bean?

| Scope         | Lebensdauer                                | Default?               |
| ------------- | ------------------------------------------ | ---------------------- |
| **Singleton** | Ein Objekt für die gesamte App-Lebensdauer | ✅ Ja                  |
| **Prototype** | Neues Objekt bei jeder Anfrage danach      | Nein                   |
| **Request**   | Neues Objekt pro HTTP-Request              | Nein (nur in Web-Apps) |
| **Session**   | Neues Objekt pro HTTP-Session              | Nein                   |

In diesem Projekt sind alle Beans **Singletons** (Default). Es gibt genau eine Instanz von `IbanController`, eine von `IbanValidationService`, usw. Das ist fast immer richtig für stateless Services.

### Konfiguration: application.properties

`application.properties` ist Spring Boot's zentrale Konfigurationsdatei — ≈ `.env` in Node.js.

```properties
# Datenbank-Verbindung (${VAR:default} liest Env-Variablen mit Fallback)
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/iban}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:admin}
# ≈ process.env.DATABASE_URL ?? 'postgresql://admin@localhost:5432/iban'

# Hibernate validiert Schema, ändert es aber nicht (Flyway übernimmt das)
spring.jpa.hibernate.ddl-auto=validate

# Embedded Tomcat Port
server.port=8080
```

**Profile** ≈ `NODE_ENV`. Spring lädt automatisch `application-{profil}.properties` als Override:

- `application.properties` — Basis (immer geladen)
- `application-dev.properties` — Overrides für Entwicklung
- Aktiviert mit: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

In unserem Projekt definiert `application-dev.properties` die CORS-Origins für den Vite Dev-Server:

```properties
app.cors.allowed-origins=http://localhost,http://localhost:5173
```

Diese Property wird in `CorsConfig.java` per `@Value` gelesen:

```java
@Value("${app.cors.allowed-origins:http://localhost}")
private String allowedOrigins;
// ≈ const allowedOrigins = process.env.APP_CORS_ALLOWED_ORIGINS ?? 'http://localhost'
```

`@Configuration` + `@Profile("dev")` sorgt dafür, dass die CORS-Config nur im Dev-Profil aktiv ist — in Produktion läuft alles über den Nginx-Reverse-Proxy (gleiche Origin, kein CORS nötig).

---

## Teil 6 — Spring Web (HTTP-Schicht)

### Wie funktioniert Spring Web?

`spring-boot-starter-web` gibt dir den kompletten HTTP-Stack:

```
HTTP Request → Embedded Tomcat → DispatcherServlet → @RestController-Methode
                                                            ↓
HTTP Response ← Jackson (→JSON) ← ResponseEntity ← Return-Wert
```

Der **DispatcherServlet** ist Spring's zentraler Request-Router. Er schaut auf `@RequestMapping` / `@GetMapping` / `@PostMapping`, findet die richtige Methode und ruft sie auf. ≈ Express's internem Routing-Mechanismus.

### @RestController vs. @Controller

|                    | `@Controller`                                | `@RestController`               |
| ------------------ | -------------------------------------------- | ------------------------------- |
| **Return-Typ**     | View-Name (HTML-Template)                    | JSON/XML-Daten                  |
| **Entspricht**     | Server-Side Rendering (≈ EJS/Pug in Express) | REST-API (≈ `res.json()`)       |
| **Ist eigentlich** | `@Component` + View-Resolution               | `@Controller` + `@ResponseBody` |

`@RestController` = `@Controller` + `@ResponseBody`. Jeder Rückgabewert wird automatisch von Jackson zu JSON serialisiert. In unserem Projekt nutzen wir ausschließlich `@RestController` — wir bauen eine REST-API, kein Server-Side Rendering.

```java
// IbanController.java — jeder Return-Wert wird automatisch JSON:
@PostMapping("/validate")
public ResponseEntity<IbanResponse> validateIban(@Valid @RequestBody IbanRequest request) {
    return ResponseEntity.ok(buildResponse(request.iban()));
    // ≈ res.status(200).json(buildResponse(req.body.iban))
}
```

### @Valid und @NotBlank — Zwei-Stufen-Validierung

Eine häufige Frage: Warum braucht man `@Valid` auf dem Parameter, wenn `@NotBlank` schon auf dem Record-Feld steht? Das ist ein **Zwei-Stufen-System** — analog zu zod + Middleware in Express:

| Stufe                       | Annotation                         | Was sie tut                                 | Analogie                                |
| --------------------------- | ---------------------------------- | ------------------------------------------- | --------------------------------------- |
| **1. Schema definieren**    | `@NotBlank` auf `IbanRequest.iban` | Legt die Validierungsregel fest             | `z.string().min(1)` — Schema-Definition |
| **2. Validierung auslösen** | `@Valid` auf dem Parameter         | Sagt Spring: „Prüfe jetzt die Constraints!" | Express-Middleware `validate(schema)`   |

```java
// Stufe 1: Schema definieren (≈ const schema = z.object({ iban: z.string().min(1) }))
record IbanRequest(@NotBlank String iban) {}

// Stufe 2: Validierung triggern (≈ validate(schema) Middleware)
public ResponseEntity<IbanResponse> validateIban(@Valid @RequestBody IbanRequest request) { ... }
//                                                ^^^^^
//                                                Ohne @Valid → @NotBlank wird IGNORIERT!
```

**Ohne `@Valid`** deserialisiert Spring den JSON-Body zu einem `IbanRequest`-Objekt, führt aber **keine Validierung** durch — ein leerer String käme ungeprüft in die Controller-Methode. `@Valid` ist der Trigger, der Jakarta Bean Validation aktiviert.

**Bei Validierungsfehler** (z.B. `{"iban": ""}`) wirft Spring automatisch eine `MethodArgumentNotValidException` — die Controller-Methode wird **nie betreten**. Der `GlobalExceptionHandler` fängt diese Exception und gibt HTTP 400 zurück:

```
Client sendet {"iban": ""}
  → Spring deserialisiert JSON (@RequestBody)
  → Spring validiert (@Valid prüft @NotBlank) → FEHLER!
  → MethodArgumentNotValidException → GlobalExceptionHandler → HTTP 400
  → Controller-Methode wird NIE aufgerufen
```

### Error Handling: @RestControllerAdvice

`@RestControllerAdvice` ist Spring's globaler Error-Handler — ≈ Express's Error-Handling-Middleware: `app.use((err, req, res, next) => { ... })`.

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Fängt @Valid-Fehler (z.B. @NotBlank schlägt fehl) → HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(...) {
        return Map.of("error", "Validation failed", "details", ...);
    }
    // ≈ Express: if (err instanceof ZodError) res.status(400).json({...})

    // Catch-all für unbehandelte Exceptions → HTTP 500
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericError(Exception ex) {
        return Map.of("error", "Internal server error");
    }
}
```

Der Ablauf: Controller-Methode wirft Exception → Spring fängt sie ab → sucht passende `@ExceptionHandler`-Methode → serialisiert den Return-Wert zu JSON. Ohne `@RestControllerAdvice` bekäme der Client eine hässliche HTML-Error-Page.

### RestClient: Externe HTTP-Aufrufe

Für den Fallback-Call zu openiban.com nutzt `ExternalIbanApiService` Spring's `RestClient` (seit Spring 6.1) — ein fluent HTTP-Client, ≈ `fetch()` oder `axios`.

```java
// ExternalIbanApiService.java
private final RestClient restClient = RestClient.builder()
        .baseUrl("https://openiban.com/validate/")
        .build();
// ≈ const apiClient = axios.create({ baseURL: 'https://openiban.com/validate/' })

public ExternalValidationResult validate(String iban) {
    try {
        var response = restClient.get()
                .uri("{iban}?getBIC=true&validateBankCode=true", iban)
                .retrieve()
                .body(OpenIbanResponse.class);  // Jackson deserialisiert automatisch
        // ...
    } catch (Exception e) {
        log.warn("External IBAN validation failed: {}", e.getMessage());
        return null;  // Graceful degradation
    }
}
```

```typescript
// TS-Äquivalent:
const res = await fetch(`${BASE_URL}${iban}?getBIC=true&validateBankCode=true`);
const data: OpenIbanResponse = await res.json();
```

Der Unterschied: `RestClient` deserialisiert direkt in ein typisiertes Java-Objekt (`.body(OpenIbanResponse.class)`). Der `try/catch` implementiert **Graceful Degradation** — wenn openiban.com down ist, gibt der Service `null` zurück und der Controller nutzt nur die lokale Validierung.

---

## Teil 7 — Datenbank-Schicht

### JPA Entities und ihre Annotations

Eine **JPA Entity** ist eine Java-Klasse, die auf eine Datenbank-Tabelle gemappt wird. Jede Instanz = eine Zeile.

Die Annotations im Detail (alle aus `Iban.java`):

```java
@Entity                     // "Diese Klasse repräsentiert eine DB-Tabelle"
                            // ≈ model Iban { ... } in Prisma
@Table(name = "ibans")      // Tabellenname explizit angeben (sonst: Klassenname)
                            // ≈ @@map("ibans") in Prisma
public class Iban {

    @Id                     // Primärschlüssel
                            // ≈ @id in Prisma
    @GeneratedValue(strategy = GenerationType.IDENTITY)
                            // DB generiert den Wert (auto-increment)
                            // ≈ @default(autoincrement()) in Prisma
    private Long id;

    @Column(nullable = false, length = 34)
                            // NOT NULL + VARCHAR(34)
                            // ≈ iban String @db.VarChar(34) in Prisma
    private String iban;
}
```

Die Spalten-Namen werden automatisch von camelCase in snake_case konvertiert: `bankName` → `bank_name`, `validationMethod` → `validation_method`. Das ist eine Spring-Boot-Convention (konfigurierbar, aber der Default passt fast immer).

### Spring Data JPA — Repositories

Spring Data JPA generiert die gesamte CRUD-Implementierung aus einem Interface:

```java
// IbanRepository.java — die GESAMTE Datei:
public interface IbanRepository extends JpaRepository<Iban, Long> {
}
```

Das gibt dir: `save()`, `findAll()`, `findById()`, `deleteById()`, `count()`, und vieles mehr.

**Unter der Haube**: Beim App-Start scannt Spring den Classpath, findet Interfaces die `JpaRepository` erweitern, und erzeugt zur Laufzeit per Proxy eine vollständige Implementierung. Der Proxy delegiert an Hibernate, das SQL generiert, das über JDBC an PostgreSQL geht:

```
ibanRepository.save(entity)
       ↓ Spring Data JPA (generierter Proxy)
       ↓ Hibernate (generiert SQL)
       ↓ JDBC (sendet SQL an DB)
       ↓ PostgreSQL (führt INSERT aus)
```

### Schema-Management: Flyway

In Spring Boot wird das DB-Schema **nicht** von Hibernate verwaltet (obwohl es könnte). Stattdessen nutzen wir **Flyway** — ein Migrations-Tool, ≈ Prisma Migrate / golang-migrate.

```
src/main/resources/db/migration/
    V1__initial_schema.sql    ← Wird einmalig beim Start ausgeführt
    V2__add_index.sql         ← Nächste Version (noch nicht vorhanden)
```

**Ablauf bei App-Start:**

1. Flyway prüft die `flyway_schema_history`-Tabelle in PostgreSQL
2. Welche Versionen wurden schon ausgeführt? V1? → Skip. V2 neu? → Ausführen.
3. Hibernate validiert (`ddl-auto=validate`): passt die Entity-Klasse zum Schema?

**Flyway vs. Liquibase**: Beides sind Migrations-Tools. Flyway nutzt **plain SQL**-Dateien (einfacher, db-spezifisch). Liquibase nutzt XML/YAML/JSON (db-agnostisch, komplexer). Flyway ist in Spring Boot der populärere Default.

---

## Teil 8 — Testing

Das Projekt hat zwei Arten von Tests — ≈ Vitest Unit-Tests + Supertest-Integrationstests.

### Unit-Tests: IbanValidationServiceTest

Pure Unit-Tests ohne Spring-Kontext. Die Service-Klasse wird direkt instanziiert — genau wie man eine reine Funktion in Vitest testet.

```java
class IbanValidationServiceTest {

    private IbanValidationService service;

    @BeforeEach  // ≈ beforeEach(() => { ... })
    void setUp() {
        service = new IbanValidationService();  // Kein Spring nötig!
    }

    @Test  // ≈ it('validates a valid German IBAN', () => { ... })
    void validGermanIban() {
        var result = service.validate("DE89370400440532013000");
        assertTrue(result.valid());                              // ≈ expect(result.valid).toBe(true)
        assertEquals("DE89370400440532013000", result.iban());   // Achtung: expected zuerst!
        assertEquals("local", result.validationMethod());
    }

    @Test
    void validGermanIbanWithSpaces() {
        var result = service.validate("DE89 3704 0044 0532 0130 00");
        assertTrue(result.valid());  // Leerzeichen werden intern entfernt
    }

    @Test
    void invalidCheckDigit() {
        assertFalse(service.validate("DE00370400440532013000").valid());
    }
}
```

**JUnit 5 Assertions vs. Vitest:**

| JUnit 5                          | Vitest/Jest                     |
| -------------------------------- | ------------------------------- |
| `assertTrue(x)`                  | `expect(x).toBe(true)`          |
| `assertEquals(expected, actual)` | `expect(actual).toBe(expected)` |
| `assertNull(x)`                  | `expect(x).toBeNull()`          |
| `@BeforeEach`                    | `beforeEach()`                  |
| `@Test`                          | `it()` / `test()`               |

### Integrationstests: IbanControllerTest

Testen den Controller mit echtem HTTP-Routing, aber gemockten Services. Spring startet dafür einen Mini-Kontext — nur die Web-Schicht, keine DB.

```java
@WebMvcTest(IbanController.class)  // Nur Web-Schicht laden (kein DB, kein echter Service)
class IbanControllerTest {

    @Autowired
    private MockMvc mockMvc;       // Fake-HTTP-Client. ≈ supertest(app)

    @MockitoBean                   // Erstellt Mock + registriert als Spring Bean
    private IbanValidationService validationService;  // ≈ vi.mock('./service')

    @MockitoBean
    private ExternalIbanApiService externalApiService;

    @MockitoBean
    private IbanRepository ibanRepository;

    @Test
    void validateValidIban() throws Exception {
        // Mock-Verhalten definieren
        when(validationService.validate("DE89370400440532013000"))
                .thenReturn(new ValidationResult(
                        true, "DE89370400440532013000", "Commerzbank", "37040044", "local"));
        // ≈ vi.mocked(service.validate).mockReturnValue({valid: true, ...})

        mockMvc.perform(post("/api/ibans/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"iban\": \"DE89370400440532013000\"}")
        )
        .andExpect(status().isOk())                              // ≈ expect(res.status).toBe(200)
        .andExpect(jsonPath("$.valid").value(true))              // ≈ expect(res.body.valid).toBe(true)
        .andExpect(jsonPath("$.bankName").value("Commerzbank")); // ≈ expect(res.body.bankName).toBe(...)
    }

    @Test
    void validateEmptyIbanReturnsBadRequest() throws Exception {
        // Kein Mock nötig — @Valid + @NotBlank lehnt ab, BEVOR die Methode aufgerufen wird
        mockMvc.perform(post("/api/ibans/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"iban\": \"\"}")
        )
        .andExpect(status().isBadRequest());  // HTTP 400 via GlobalExceptionHandler
    }
}
```

**Schlüssel-Annotations:**

| Annotation                  | Was es tut                                 | Analogie                             |
| --------------------------- | ------------------------------------------ | ------------------------------------ |
| `@WebMvcTest(X.class)`      | Mini-Spring-Context nur für den Controller | Supertest mit isoliertem Router      |
| `@MockitoBean`              | Registriert einen Mock als Spring Bean     | `vi.mock()` / `jest.mock()`          |
| `MockMvc`                   | Fake-HTTP-Client (kein echtes Netzwerk)    | `supertest(app)`                     |
| `when(...).thenReturn(...)` | Definiert Mock-Verhalten                   | `vi.mocked(fn).mockReturnValue(...)` |

**Tests ausführen:**

```bash
mvn test           # Alle Tests
mvn verify -B      # CI-Modus (batch, keine interaktive Eingabe)
```

---

## Teil 9 — Conventions

### Java-Conventions

| Convention                        | Beispiel im Projekt                                  | Warum                                           |
| --------------------------------- | ---------------------------------------------------- | ----------------------------------------------- |
| Klassen: PascalCase               | `IbanController`, `ValidationResult`                 | Standard seit Java 1.0                          |
| Methoden/Variablen: camelCase     | `validateIban()`, `bankName`                         | Einheitlichkeit                                 |
| Konstanten: UPPER_SNAKE_CASE      | `KNOWN_BANKS`, `COUNTRY_LENGTHS`                     | Sofort als Konstante erkennbar                  |
| Packages: lowercase               | `de.nicograef.iban.service`                          | Convention, alles lowercase                     |
| Eine öffentliche Klasse pro Datei | `IbanController.java` enthält `class IbanController` | Dateiname = Klassenname                         |
| Getter: `getX()` / `isX()`        | `getIban()`, `isValid()`                             | JavaBeans-Standard, Frameworks bauen darauf auf |

### Spring-Boot-Conventions

| Convention               | Beispiel im Projekt                                | Warum                                   |
| ------------------------ | -------------------------------------------------- | --------------------------------------- |
| Starter Dependencies     | `spring-boot-starter-web`                          | Ein Starter = alle Deps für ein Feature |
| Package-Struktur         | `controller/`, `service/`, `model/`, `repository/` | Übliche Schichtentrennung               |
| `application.properties` | `server.port=8080`                                 | Zentrale Konfiguration                  |
| Profile                  | `application-dev.properties`                       | Environment-spezifische Config          |
| Constructor Injection    | Kein `@Autowired` auf Feldern                      | Testbarkeit + Immutabilität             |
| Schema per Flyway        | `V1__initial_schema.sql`                           | Hibernate validiert nur, ändert nicht   |
| `@RestControllerAdvice`  | `GlobalExceptionHandler`                           | Zentrales Error-Handling                |
| Records für DTOs         | `IbanRequest`, `IbanResponse`                      | Immutable, kein Lombok nötig            |

---

## Zusammenfassung — Von unten nach oben

```
┌───────────────────────────────────────────────────────────────┐
│                     Spring Boot App                           │
├───────────────────────────────────────────────────────────────┤
│  @RestController (IbanController)                             │
│    ├── empfängt HTTP Requests (≈ Express Router)              │
│    ├── Jackson deserialisiert JSON → Records (DTOs)           │
│    ├── @Valid prüft Constraints (≈ zod)                       │
│    └── @RestControllerAdvice fängt Fehler global              │
├───────────────────────────────────────────────────────────────┤
│  @Service (IbanValidationService, ExternalIbanApiService)     │
│    ├── Business-Logik (Mod-97, BLZ-Lookup)                   │
│    └── RestClient → openiban.com (≈ fetch/axios)             │
├───────────────────────────────────────────────────────────────┤
│  JpaRepository (IbanRepository — 0 Zeilen eigener Code)      │
│    └── Spring → Hibernate → JDBC → SQL                       │
├───────────────────────────────────────────────────────────────┤
│  @Entity (Iban.java) ↔ Flyway (V1__initial_schema.sql)       │
│    └── Flyway verwaltet Schema, Hibernate validiert nur       │
├───────────────────────────────────────────────────────────────┤
│  Konfiguration: application.properties + Profile (dev/prod)  │
├───────────────────────────────────────────────────────────────┤
│  Tests: JUnit 5 (Unit) + MockMvc (Integration)               │
├───────────────────────────────────────────────────────────────┤
│  Maven (pom.xml) → Build → .jar                              │
├───────────────────────────────────────────────────────────────┤
│  JVM (Java 21) + Embedded Tomcat + PostgreSQL                 │
└───────────────────────────────────────────────────────────────┘
```
