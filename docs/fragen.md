Liste von Fragen, die in der Prüfung, nach der Präsentation oder beim Bewerbungsgespräch vorkommen können.
Format: Multiple-Choice oder Single-Choice mit jeweils vier Antwortoptionen.
Struktur: Fragennummer (zB 13), Frage (Was bedeutet ... ?), Antwortmöglichkeiten ([A] erste Antwortoption, [B] ...), Auflösung (zB [A] für Single-Chice oder [B,D] für Multiple-Choice), Erklärung/Begründung

---

## Block 1 — IBAN Fachlichkeit

### 1. Wie viele Zeichen hat eine deutsche IBAN?

- [A] 20
- [B] 22
- [C] 24
- [D] 34

**Antwort: [B]**
Die deutsche IBAN hat immer exakt 22 Stellen: 2 Buchstaben Ländercode (DE) + 2 Prüfziffern + 8 Stellen BLZ + 10 Stellen Kontonummer.

---

### 2. Aus welchen Bestandteilen setzt sich eine IBAN zusammen (allgemein)?

- [A] Ländercode, Prüfziffern, BBAN
- [B] BIC, Kontonummer, Bankleitzahl
- [C] SWIFT-Code, Prüfziffern, Kontonummer
- [D] Routing Number, Ländercode, Kontonummer

**Antwort: [A]**
Jede IBAN besteht aus: 2-Buchstaben-Ländercode (ISO 3166-1), 2 Prüfziffern (Modulo 97) und dem BBAN (Basic Bank Account Number), der länderspezifisch aufgebaut ist.

---

### 3. Was ist das BBAN?

- [A] Der BIC-Code einer Bank
- [B] Der nationale Teil der IBAN ohne Ländercode und Prüfziffern
- [C] Eine alternative Bezeichnung für die gesamte IBAN
- [D] Die Bankleitzahl in Deutschland

**Antwort: [B]**
BBAN = Basic Bank Account Number. Es ist der länderspezifische Teil der IBAN (ab Position 5), der Bankidentifikation und Kontonummer enthält.

---

### 4. Welcher Modulo-Wert muss bei einer gültigen IBAN herauskommen, wenn man den ISO-7064-Algorithmus anwendet?

- [A] 0
- [B] 1
- [C] 97
- [D] 98

**Antwort: [B]**
Bei der Modulo-97-Validierung muss der Rest der Division durch 97 exakt 1 ergeben. Das ist der ISO-7064-Standard (MOD-97-10).

---

### 5. Was passiert im ersten Schritt der Modulo-97-Validierung mit den ersten 4 Zeichen der IBAN?

- [A] Sie werden entfernt
- [B] Sie werden verdoppelt
- [C] Sie werden ans Ende der IBAN verschoben
- [D] Sie werden durch Nullen ersetzt

**Antwort: [C]**
Die ersten 4 Zeichen (Ländercode + Prüfziffern) werden ans Ende verschoben. Aus DE68210501… wird 210501…DE68. Danach werden Buchstaben in Zahlen konvertiert (A=10, …, Z=35).

---

### 6. Wie werden Buchstaben bei der Modulo-97-Berechnung in Zahlen umgewandelt?

- [A] A=1, B=2, …, Z=26
- [B] A=10, B=11, …, Z=35
- [C] Über den ASCII-Wert direkt
- [D] A=0, B=1, …, Z=25

**Antwort: [B]**
Laut ISO 13616 wird jeder Buchstabe durch seine Position im Alphabet + 9 ersetzt: A=10, B=11, …, Z=35. So wird z. B. D=13 und E=14.

---

### 7. Warum wird in Java `BigInteger` für die Modulo-97-Berechnung verwendet?

- [A] `BigInteger` ist schneller als `int`
- [B] Die entstehende Zahl kann 30–60+ Stellen haben und passt in keinen nativen Datentyp
- [C] `BigInteger` ist der einzige Typ, der Modulo unterstützt
- [D] Es ist eine Vorgabe von Spring Boot

**Antwort: [B]**
Nach der Buchstaben-Konvertierung entsteht eine Zahl mit 30–60+ Stellen. `int` (max. ~2 Mrd.) und `long` (max. ~9,2 × 10¹⁸) reichen nicht. `BigInteger` unterstützt beliebig große Zahlen — analog zu JavaScripts `BigInt`.

---

### 8. An welchen Positionen steht die Bankleitzahl (BLZ) in einer deutschen IBAN?

- [A] Position 1–8
- [B] Position 3–10
- [C] Position 5–12
- [D] Position 13–22

**Antwort: [C]**
In der deutschen IBAN stehen die 8 Stellen der BLZ an Position 5–12 (nach Ländercode und Prüfziffern). In Java-Code: `iban.substring(4, 12)` (0-basiert).

---

### 9. Welche Fehlerarten erkennt die Modulo-97-Prüfung zu 100 %?

- [A] Nur einzelne Tippfehler
- [B] Einzelne Tippfehler und Zahlendreher zweier benachbarter Ziffern
- [C] Nur Zahlendreher
- [D] Doppelte Substitutionsfehler

**Antwort: [B]**
Das Modulo-97-Verfahren (ISO 7064) erkennt 100 % aller einzelnen Tippfehler und 100 % aller Zahlendreher zweier benachbarter Ziffern. Zusätzlich erkennt die feste Länderlänge Auslassungen und Verdopplungen.

---

### 10. Was ist SEPA und wann wurde die IBAN in der EU verpflichtend?

- [A] Single Euro Payments Area — verpflichtend seit 2016
- [B] Secure European Payment Authorization — verpflichtend seit 2012
- [C] Single Euro Payments Area — verpflichtend seit 1. Februar 2014
- [D] Standard Electronic Payment Account — verpflichtend seit 2010

**Antwort: [C]**
SEPA = Single Euro Payments Area. Die EU-Verordnung Nr. 260/2012 legte den 1. Februar 2014 als Stichtag fest, ab dem IBAN die nationalen Kontonummern für Überweisungen und Lastschriften ersetzte.

---

### 11. Welche maximale Länge hat eine IBAN laut ISO 13616?

- [A] 22 Zeichen
- [B] 28 Zeichen
- [C] 34 Zeichen
- [D] 40 Zeichen

**Antwort: [C]**
Laut ISO 13616 beträgt die maximale IBAN-Länge 34 alphanumerische Zeichen. Die kürzeste IBAN hat Norwegen mit 15 Zeichen, die längste in Europa hat Malta mit 31 Zeichen.

---

### 12. Welches Land nutzt KEIN IBAN-System?

- [A] Schweiz
- [B] Vereinigtes Königreich (UK)
- [C] USA
- [D] Norwegen

**Antwort: [C]**
Die USA nutzen kein IBAN-System, sondern ABA Routing Transit Numbers. Auch Kanada und Australien verwenden eigene Systeme. Die Schweiz, UK und Norwegen nutzen IBANs.

---

## Block 2 — Java & Spring Boot Basics

### 13. Was sind JDK, JRE und JVM — in welcher Beziehung stehen sie?

- [A] JVM enthält JRE, JRE enthält JDK
- [B] JDK enthält JRE, JRE enthält JVM
- [C] Alle drei sind synonyme Begriffe
- [D] JRE enthält JDK und JVM

**Antwort: [B]**
Das JDK (Java Development Kit) enthält das JRE (Java Runtime Environment), das wiederum die JVM (Java Virtual Machine) enthält. Die JVM führt Bytecode aus, das JRE liefert die Standardbibliothek, das JDK bringt den Compiler (`javac`) und Entwickler-Tools mit.

---

### 14. Was erzeugt der Java-Compiler `javac`?

- [A] Nativen Maschinencode (.exe)
- [B] JavaScript-Dateien
- [C] Bytecode (.class-Dateien)
- [D] Direkt ausführbare .jar-Dateien

**Antwort: [C]**
`javac` kompiliert `.java`-Dateien zu `.class`-Dateien (Bytecode). Dieser Bytecode ist plattformunabhängig und wird von der JVM interpretiert/JIT-kompiliert. Analog zu: `tsc` kompiliert `.ts` → `.js`.

---

### 15. Wann werden Spring-Annotations wie `@RestController` oder `@GetMapping` tatsächlich ausgewertet?

- [A] Zur Compile-Zeit durch `javac`
- [B] Zur Laufzeit durch Spring per Reflection
- [C] Beim Maven-Build durch ein Plugin
- [D] Noch vor dem `main()`-Aufruf durch die JVM

**Antwort: [B]**
Annotations sind zur Compile-Zeit nur passive Metadaten im Bytecode. Erst zur Laufzeit — beim Aufruf von `SpringApplication.run()` — liest Spring diese per Reflection aus und baut daraus Beans, Routes und DI-Konfiguration.

---

### 16. Was ist Dependency Injection (DI)?

- [A] Ein Pattern, bei dem Klassen ihre Abhängigkeiten selbst mit `new` erstellen
- [B] Ein Framework-Feature, bei dem Abhängigkeiten von außen in den Konstruktor übergeben werden
- [C] Ein Build-Tool-Mechanismus von Maven
- [D] Ein Verfahren zum automatischen Download von Libraries

**Antwort: [B]**
DI = Inversion of Control: Statt `new Service()` im Controller zu schreiben, übergibt Spring die fertige Instanz über den Konstruktor. Vorteil: Testbarkeit (Mocks injizieren) und lose Kopplung.

---

### 17. Welche Injection-Variante gilt in Spring Boot als Best Practice?

- [A] Field Injection mit `@Autowired`
- [B] Setter Injection
- [C] Constructor Injection
- [D] Static Injection

**Antwort: [C]**
Constructor Injection ist Best Practice: Felder können `final` sein (Immutabilität), Abhängigkeiten sind explizit sichtbar, Objekte sind nach Konstruktion komplett initialisiert, und Tests funktionieren ohne Spring-Kontext (`new Controller(mockService)`).

---

### 18. Was ist ein Bean in Spring?

- [A] Eine Java-Klasse, die mit `new` erstellt wird
- [B] Ein von Spring verwaltetes Objekt im Application Context
- [C] Eine Konfigurationsdatei
- [D] Ein Maven-Dependency-Eintrag

**Antwort: [B]**
Ein Bean ist ein Objekt, das Spring erstellt, verwaltet und per DI bereitstellt. Beans werden durch Annotations wie `@Service`, `@RestController`, `@Repository` oder `@Configuration` + `@Bean` registriert.

---

### 19. Was ist der Standard-Scope eines Spring-Beans?

- [A] Prototype (neues Objekt pro Anfrage)
- [B] Request (pro HTTP-Request)
- [C] Singleton (ein Objekt für die gesamte App-Lebensdauer)
- [D] Session (pro HTTP-Session)

**Antwort: [C]**
Der Default-Scope ist Singleton: Es gibt genau eine Instanz pro Bean-Definition für den gesamten Application Context. Für stateless Services (wie in diesem Projekt) ist das ideal.

---

### 20. Welche dieser Annotations registriert eine Klasse NICHT als Spring Bean?

- [A] `@Service`
- [B] `@RestController`
- [C] `@Entity`
- [D] `@Repository`

**Antwort: [C]**
`@Entity` ist eine JPA-Annotation und registriert die Klasse NICHT als Spring Bean. Sie markiert eine Klasse als Datenbank-Entität. `@Service`, `@RestController` und `@Repository` sind alle Spezialisierungen von `@Component` und registrieren Beans.

---

### 21. Was bedeutet `@SpringBootApplication`?

- [A] Es aktiviert nur den embedded Tomcat
- [B] Es ist eine Kombination aus `@Configuration`, `@EnableAutoConfiguration` und `@ComponentScan`
- [C] Es ersetzt die `main()`-Methode
- [D] Es aktiviert nur Flyway-Migrations

**Antwort: [B]**
`@SpringBootApplication` ist eine Meta-Annotation, die drei Annotations kombiniert: `@Configuration` (Konfigurationsklasse), `@EnableAutoConfiguration` (automatische Konfiguration basierend auf Dependencies) und `@ComponentScan` (sucht nach `@Component`-Klassen im Package-Baum).

---

### 22. Was macht `SpringApplication.run()` beim Start? (Mehrfachauswahl)

- [A] Führt Component Scan durch und findet annotierte Klassen
- [B] Erstellt Beans und löst Dependency Injection auf
- [C] Startet den embedded Tomcat auf dem konfigurierten Port
- [D] Alle genannten Punkte

**Antwort: [D]**
`SpringApplication.run()` löst eine ganze Kette aus: Component Scan per Reflection, Bean-Erstellung + DI, Route-Registrierung aus `@RequestMapping`-Annotations, Flyway-Migrations und Start des embedded Tomcat auf Port 8080.

---

## Block 3 — Backend Architecture (Controller → Service → Repository)

### 23. Welche drei Schichten bilden das Standard-Spring-Boot-Pattern?

- [A] View → Model → Database
- [B] Controller → Service → Repository
- [C] Router → Middleware → Handler
- [D] Frontend → Backend → Cache

**Antwort: [B]**
Das kanonische Spring-Boot-Pattern: Controller (HTTP-Handling), Service (Business-Logik), Repository (Datenzugriff). Jede Schicht hat eine klare Verantwortung und ist einzeln testbar.

---

### 24. Was ist die Aufgabe des `IbanController` in diesem Projekt?

- [A] IBAN-Validierungslogik (Modulo 97) ausführen
- [B] HTTP-Requests empfangen, validieren und Responses zurückgeben
- [C] Datenbank-Queries ausführen
- [D] Die externe API aufrufen

**Antwort: [B]**
Der Controller ist die HTTP-Schicht: Er empfängt Requests, delegiert an Services und gibt JSON-Responses zurück. Die eigentliche Validierungslogik steckt im `IbanValidationService`.

---

### 25. Was bedeutet `@RestController`?

- [A] Es ist eine Kombination aus `@Controller` und `@ResponseBody`
- [B] Es aktiviert nur die REST-Dokumentation
- [C] Es registriert die Klasse als JPA-Repository
- [D] Es ist eine Kombination aus `@Service` und `@RequestMapping`

**Antwort: [A]**
`@RestController` = `@Controller` + `@ResponseBody`. Jeder Rückgabewert einer Methode wird automatisch von Jackson zu JSON serialisiert. Ohne `@ResponseBody` würde Spring einen View-Namen (HTML-Template) erwarten.

---

### 26. Was bewirkt `@RequestMapping("/api/ibans")` auf Klassenebene?

- [A] Es definiert den Basis-Pfad für alle Endpunkte in diesem Controller
- [B] Es erstellt automatisch eine Datenbank-Tabelle
- [C] Es aktiviert CORS für diesen Pfad
- [D] Es registriert einen Fehler-Handler

**Antwort: [A]**
`@RequestMapping` auf Klassenebene setzt den Basis-Pfad. Alle Methoden-Annotations (`@PostMapping("/validate")`, `@GetMapping`) werden relativ dazu aufgelöst: `/api/ibans/validate`, `/api/ibans`.

---

### 27. Welche Annotation sorgt dafür, dass der JSON-Request-Body auf ein Java-Objekt gemappt wird?

- [A] `@RequestParam`
- [B] `@PathVariable`
- [C] `@RequestBody`
- [D] `@ModelAttribute`

**Antwort: [C]**
`@RequestBody` weist Spring an, den HTTP-Body per Jackson in das annotierte Objekt (z. B. `IbanRequest`) zu deserialisieren. Analog zu `req.body` in Express.js (mit body-parser).

---

### 28. Was ist ein Java Record und wofür wird er in diesem Projekt verwendet?

- [A] Ein veränderbares Datenobjekt mit Gettern und Settern
- [B] Eine immutable Datenklasse ohne Boilerplate, verwendet als DTO
- [C] Eine abstrakte Klasse für Vererbung
- [D] Ein Interface für Datenzugriff

**Antwort: [B]**
Records (seit Java 16) sind immutable Datenklassen. Der Compiler generiert Konstruktor, Getter (ohne `get`-Prefix), `equals()`, `hashCode()` und `toString()`. Im Projekt als DTOs: `IbanRequest`, `IbanResponse`, `ValidationResult`.

---

### 29. Warum kann die JPA-Entity `Iban.java` kein Record sein?

- [A] Records sind in Java 21 noch nicht stabil
- [B] Hibernate braucht einen leeren Konstruktor und mutable Felder (Reflection)
- [C] Records dürfen keine Annotations haben
- [D] Records werden nicht von Jackson unterstützt

**Antwort: [B]**
Hibernate/JPA instanziiert Entities per Reflection: leerer Konstruktor → Felder einzeln per `field.set()` setzen. Records sind immutable (alle Felder `final`) und haben keinen leeren Konstruktor — das ist mit Hibernate inkompatibel.

---

### 30. Was macht die `buildResponse()`-Methode im IbanController?

- [A] Sie baut die HTML-Seite auf
- [B] Sie führt die Mod-97-Validierung durch und fällt bei unbekannter Bank auf die externe API zurück
- [C] Sie speichert die IBAN in der Datenbank
- [D] Sie sendet eine E-Mail-Benachrichtigung

**Antwort: [B]**
`buildResponse()` ist die extrahierte Shared-Logik beider Endpunkte: Erst lokale Validierung via `IbanValidationService`, dann Fallback auf `ExternalIbanApiService` falls die Bank unbekannt ist. DRY-Prinzip — vermeidet Code-Duplizierung.

---

### 31. Was ist Jackson im Spring-Boot-Kontext?

- [A] Ein HTTP-Server
- [B] Ein Datenbank-Treiber
- [C] Die Standard-JSON-Serialisierungs/Deserialisierungsbibliothek
- [D] Ein Test-Framework

**Antwort: [C]**
Jackson konvertiert automatisch Java-Objekte ↔ JSON. Bei `@RequestBody` deserialisiert es den JSON-Request in ein Java-Objekt, bei `@RestController`-Returns serialisiert es den Rückgabewert zu JSON. Analog zu `JSON.parse()`/`JSON.stringify()` plus automatischem Typ-Mapping.

---

## Block 4 — JPA, Hibernate & Flyway

### 32. Was ist JPA?

- [A] Eine konkrete ORM-Implementierung
- [B] Eine Spezifikation (Standard) für Object-Relational Mapping in Java
- [C] Ein Datenbank-Treiber für PostgreSQL
- [D] Ein Build-Tool für Java-Projekte

**Antwort: [B]**
JPA (Jakarta Persistence API) ist eine Spezifikation — sie definiert Annotations wie `@Entity`, `@Id`, `@Column` und Interfaces. Hibernate ist die populärste Implementierung dieser Spezifikation. Analog: JPA ist das Interface, Hibernate implementiert es.

---

### 33. Was ist Hibernate im Verhältnis zu JPA?

- [A] Ein Ersatz für JPA
- [B] Die populärste Implementierung der JPA-Spezifikation
- [C] Ein Datenbank-Management-Tool
- [D] Ein Spring-Boot-Plugin

**Antwort: [B]**
Hibernate implementiert die JPA-Spezifikation und bildet die Brücke zwischen Java-Objekten und relationalen Datenbanken. Die Aufruf-Kette: Spring Data JPA → Hibernate → JDBC → PostgreSQL. Analog zu Prisma als ORM über dem `pg`-Treiber.

---

### 34. Was bewirkt die Annotation `@Entity` auf einer Klasse?

- [A] Sie registriert die Klasse als Spring Bean
- [B] Sie markiert die Klasse als Datenbank-Tabellen-Mapping
- [C] Sie macht die Klasse immutable
- [D] Sie aktiviert JSON-Serialisierung

**Antwort: [B]**
`@Entity` sagt Hibernate: „Diese Klasse repräsentiert eine Datenbank-Tabelle." Jede Instanz entspricht einer Zeile. Hibernate nutzt Reflection, um Felder auf Spalten zu mappen. Analog zu `model Iban { ... }` in Prisma.

---

### 35. Was bewirkt `@GeneratedValue(strategy = GenerationType.IDENTITY)` auf dem `id`-Feld?

- [A] Die ID wird von der Anwendung generiert (UUID)
- [B] Die Datenbank generiert die ID automatisch (Auto-Increment)
- [C] Die ID wird aus dem IBAN-Wert berechnet
- [D] Spring generiert eine zufällige Zahl

**Antwort: [B]**
`GenerationType.IDENTITY` delegiert die ID-Generierung an die Datenbank (Auto-Increment / `GENERATED BY DEFAULT AS IDENTITY` in PostgreSQL). Hibernate liest die generierte ID nach dem INSERT zurück. Analog zu `@default(autoincrement())` in Prisma.

---

### 36. Was macht `IbanRepository extends JpaRepository<Iban, Long>`?

- [A] Es definiert manuell alle CRUD-SQL-Queries
- [B] Es ist ein leeres Interface — Spring generiert die komplette CRUD-Implementierung zur Laufzeit
- [C] Es erstellt eine REST-API automatisch
- [D] Es definiert Flyway-Migrationen

**Antwort: [B]**
Spring Data JPA erkennt das Interface beim Start, erzeugt per Proxy eine vollständige Implementierung mit `save()`, `findAll()`, `findById()`, `deleteById()` etc. — ohne eine einzige Zeile Implementierungscode. Analog zum Prisma Client.

---

### 37. Was bedeutet `spring.jpa.hibernate.ddl-auto=validate`?

- [A] Hibernate erstellt Tabellen automatisch
- [B] Hibernate löscht und erstellt Tabellen bei jedem Start neu
- [C] Hibernate prüft, ob Entity-Klassen zum DB-Schema passen, ändert es aber nie
- [D] Hibernate ignoriert das Schema komplett

**Antwort: [C]**
`validate` = Hibernate vergleicht Entity-Annotations mit dem vorhandenen DB-Schema. Stimmt etwas nicht überein, wirft es einen Fehler beim Start. Das Schema wird von Flyway verwaltet — Hibernate darf es nie verändern. Sicherer als `update` (das könnte Spalten droppen).

---

### 38. Was ist Flyway?

- [A] Ein Java-Webserver
- [B] Ein Tool für versionierte Datenbank-Migrationen
- [C] Ein Frontend-Build-Tool
- [D] Ein Git-Plugin für automatische Deployments

**Antwort: [B]**
Flyway führt versionierte SQL-Migrations-Dateien (`V1__initial_schema.sql`, `V2__...`) beim App-Start aus. Es trackt in der Tabelle `flyway_schema_history`, welche Migrationen bereits angewendet wurden. Analog zu `prisma migrate deploy` oder `golang-migrate`.

---

### 39. Wie lautet die Namenskonvention für Flyway-Migrationsdateien?

- [A] `migration_001.sql`
- [B] `V{nummer}__{beschreibung}.sql` (doppelter Unterstrich)
- [C] `schema-{datum}.sql`
- [D] `{beschreibung}_v{nummer}.sql`

**Antwort: [B]**
Flyway erwartet das Format `V{Version}__{Beschreibung}.sql` mit doppeltem Unterstrich. Beispiel: `V1__initial_schema.sql`. Die Dateien liegen in `src/main/resources/db/migration/`.

---

### 40. Was ist der Unterschied zwischen `ddl-auto=update` und Flyway-Migrations?

- [A] Kein Unterschied — beide machen dasselbe
- [B] `update` lässt Hibernate das Schema aus Annotations ableiten (riskant), Flyway nutzt versionierte SQL-Dateien (sicher/reproduzierbar)
- [C] Flyway ist nur für Tests geeignet
- [D] `update` ist sicherer als Flyway

**Antwort: [B]**
`ddl-auto=update` lässt Hibernate Tabellen basierend auf Entity-Annotations erstellen/ändern — das kann ungewollt Spalten droppen. Flyway-Migrations sind explizite, versionierte SQL-Dateien und damit reproduzierbar und sicher. In Produktion wird Flyway (oder Liquibase) verwendet, nie `update`.

---

### 41. Warum hat die Entity `Iban.java` einen `protected`-Konstruktor ohne Parameter?

- [A] Damit der Controller die Entity erstellen kann
- [B] Damit Hibernate die Entity per Reflection instanziieren kann
- [C] Damit die Entity in Tests verwendet werden kann
- [D] Java erfordert immer einen leeren Konstruktor

**Antwort: [B]**
JPA/Hibernate instanziiert Entities per Reflection: `clazz.getDeclaredConstructor().newInstance()`. Dafür braucht es einen parameterlosen Konstruktor. `protected` statt `public`, damit nur Hibernate (und Subklassen) ihn nutzen — externe Aufrufer verwenden den vollständigen Konstruktor.

---

## Block 5 — Maven

### 42. Was ist Maven?

- [A] Ein Java-Webserver
- [B] Ein Build-Tool und Dependency-Manager für Java
- [C] Ein Versionskontrollsystem
- [D] Eine Test-Library

**Antwort: [B]**
Maven ist Javas Standard-Build-Tool und Dependency-Manager. Die `pom.xml` ist die zentrale Konfigurationsdatei — analog zu `package.json` in Node.js oder `go.mod` in Go. Maven kompiliert, testet, paketiert und verwaltet Dependencies.

---

### 43. Was ist die `pom.xml`?

- [A] Eine Laufzeit-Konfigurationsdatei für die Datenbank
- [B] Das Project Object Model — zentrale Build- und Dependency-Konfiguration
- [C] Eine HTML-Template-Datei
- [D] Die Datei, die JPA-Entities definiert

**Antwort: [B]**
POM = Project Object Model. Die `pom.xml` definiert Projekt-Koordinaten (`groupId`, `artifactId`, `version`), Dependencies, Build-Plugins und erbt standardisierte Konfiguration vom `spring-boot-starter-parent`.

---

### 44. Was bedeutet `<scope>runtime</scope>` bei einer Maven-Dependency?

- [A] Die Dependency ist nur zum Kompilieren verfügbar
- [B] Die Dependency ist nur zur Laufzeit verfügbar, nicht zum Kompilieren
- [C] Die Dependency wird nicht heruntergeladen
- [D] Die Dependency ist nur in Tests verfügbar

**Antwort: [B]**
`runtime` Scope bedeutet: Der Code wird nicht direkt importiert (Compile-Zeit), aber zur Laufzeit benötigt. Beispiel: Der PostgreSQL-JDBC-Treiber wird von Hibernate intern geladen, nie direkt im eigenen Code referenziert.

---

### 45. Was ist der `spring-boot-starter-parent`?

- [A] Ein Plugin, das Tomcat installiert
- [B] Ein Parent-POM, das Default-Versionen für alle Spring-Boot-Dependencies setzt
- [C] Eine Konfiguration für die Datenbank
- [D] Ein Test-Framework

**Antwort: [B]**
Der `spring-boot-starter-parent` ist ein Parent-POM, das Versionen aller abhängigen Libraries koordiniert. Deshalb braucht man bei `spring-boot-starter-web` keine Version anzugeben — sie kommt vom Parent. Analog zu einer shared Config in einem Monorepo.

---

### 46. Was erzeugt `mvn package`?

- [A] Nur die kompilierten `.class`-Dateien
- [B] Eine ausführbare `.jar`-Datei mit eingebettetem Tomcat
- [C] Ein Docker-Image
- [D] Eine `.war`-Datei für einen externen Anwendungsserver

**Antwort: [B]**
`mvn package` kompiliert, testet und erzeugt eine Fat-JAR-Datei (Self-contained Archiv mit eingebettetem Tomcat + allen Dependencies). Aufruf: `java -jar target/iban-validator-0.0.1-SNAPSHOT.jar`. Analog zu `pnpm build`.

---

### 47. Was ist der `mvnw` (Maven Wrapper)?

- [A] Ein alternativer Paketmanager
- [B] Ein Skript, das die exakte Maven-Version herunterlädt und pinnt
- [C] Ein Proxy für Maven Central
- [D] Ein Docker-Container für Maven

**Antwort: [B]**
Der Maven Wrapper (`mvnw` / `mvnw.cmd`) stellt sicher, dass jeder Entwickler und jede CI-Pipeline exakt die gleiche Maven-Version verwendet (hier 3.9.12). Analog zu corepack + pnpm mit gepinnter Version in `package.json`.

---

### 48. Welcher Maven-Befehl startet die App im Entwicklungsmodus?

- [A] `mvn start`
- [B] `mvn run`
- [C] `mvn spring-boot:run`
- [D] `mvn dev`

**Antwort: [C]**
`mvn spring-boot:run` kompiliert den Code und startet die App direkt — ohne eine `.jar`-Datei zu erzeugen. Man kann Profile aktivieren: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`. Analog zu `pnpm dev`.

---

## Block 6 — Docker & Docker Compose

### 49. Wie viele Docker-Services hat dieses Projekt in der Produktions-Konfiguration?

- [A] 2 (Backend + Frontend)
- [B] 3 (PostgreSQL + Backend + Frontend)
- [C] 4 (PostgreSQL + Backend + Frontend + Reverse Proxy)
- [D] 1 (Alles in einem Container)

**Antwort: [B]**
Das Projekt hat 3 Services: `postgres` (PostgreSQL 17), `backend` (Spring Boot / Java 21) und `frontend` (Nginx mit SPA + API-Proxy). Kein separater Reverse-Proxy-Container — Nginx im Frontend übernimmt beides.

---

### 50. Warum gibt es keinen separaten Reverse-Proxy-Container?

- [A] Docker unterstützt keinen vierten Container
- [B] Die Frontend-Nginx dient bereits SPA-Dateien und proxied `/api/*` zum Backend
- [C] Spring Boot hat einen eingebauten Reverse Proxy
- [D] PostgreSQL übernimmt das Routing

**Antwort: [B]**
Da die Nginx im Frontend-Container bereits läuft (um Static Files zu serven), ist ein `location /api/`-Block trivial hinzufügbar. Ein separater Reverse-Proxy-Container wäre unnötiger Overhead für eine Single-Backend-Architektur (ADR #1).

---

### 51. Was bewirkt `depends_on: postgres: condition: service_healthy` im Backend-Service?

- [A] Es startet das Backend immer vor PostgreSQL
- [B] Es wartet, bis der PostgreSQL-Healthcheck erfolgreich ist, bevor das Backend startet
- [C] Es prüft die Backend-Gesundheit
- [D] Es stoppt PostgreSQL, wenn das Backend fehlschlägt

**Antwort: [B]**
`condition: service_healthy` stellt sicher, dass Docker Compose den Backend-Container erst startet, nachdem PostgreSQL seinen Healthcheck (`pg_isready`) bestanden hat. Das verhindert, dass Flyway eine noch nicht bereite Datenbank erreicht.

---

### 52. Wo werden die Datenbank-Credentials in Docker Compose konfiguriert?

- [A] Hardcoded in der `docker-compose.yml`
- [B] In einer `.env`-Datei, referenziert via `${POSTGRES_USER}`
- [C] In der `application.properties`
- [D] Direkt im Java-Code

**Antwort: [B]**
Die Credentials stehen in einer `.env`-Datei (nicht im Repository). Docker Compose liest `${POSTGRES_USER}` und `${POSTGRES_PASSWORD}` automatisch daraus. Das Backend erhält die Werte als Environment-Variablen (`SPRING_DATASOURCE_USERNAME`).

---

### 53. Was ist ein Docker Volume und wofür wird `postgres-data` verwendet?

- [A] Ein temporärer Speicher, der beim Container-Stopp gelöscht wird
- [B] Ein persistenter Speicher, der Datenbank-Daten über Container-Neustarts hinweg erhält
- [C] Ein Backup-Verzeichnis
- [D] Ein Log-Verzeichnis

**Antwort: [B]**
Docker Volumes sind persistente Speicher außerhalb des Container-Dateisystems. `postgres-data` mapped auf `/var/lib/postgresql/data` — die DB-Daten bleiben erhalten, auch wenn der Container neu erstellt wird.

---

### 54. Was macht die Nginx-Konfiguration `try_files $uri $uri/ /index.html`?

- [A] Sie leitet alle API-Requests um
- [B] Sie liefert für jede unbekannte Route die `index.html` aus (SPA-Routing)
- [C] Sie blockiert nicht existierende Dateien
- [D] Sie komprimiert alle Dateien

**Antwort: [B]**
Das ist das SPA-Routing-Pattern: Wenn keine physische Datei gefunden wird, wird `index.html` ausgeliefert. React Router übernimmt dann das Client-Side-Routing. Ohne diesen Fallback würden direkte URL-Aufrufe zu 404-Fehlern führen.

---

### 55. Was bewirkt `proxy_pass http://backend:8080/api/` in der Nginx-Konfiguration?

- [A] Es cacht API-Responses
- [B] Es leitet `/api/*`-Requests an den Backend-Container auf Port 8080 weiter
- [C] Es blockiert externe API-Zugriffe
- [D] Es erstellt eine WebSocket-Verbindung

**Antwort: [B]**
`proxy_pass` leitet Requests, die mit `/api/` beginnen, an den Docker-internen Service `backend` auf Port 8080 weiter. Docker Compose macht den Service-Namen `backend` zum DNS-Namen im internen Netzwerk.

---

### 56. Warum exponiert nur der Frontend-Container Port 80 nach außen?

- [A] Weil Backend und Datenbank keine Netzwerkverbindung brauchen
- [B] Weil der Browser nur Port 80 unterstützt
- [C] Weil Nginx als einziger Entry Point fungiert und intern zu Backend/DB routed
- [D] Weil Docker nur einen Port gleichzeitig exponieren kann

**Antwort: [C]**
Der Frontend-Container (Nginx) ist der einzige öffentliche Entry Point. Er liefert die SPA aus und proxied `/api/*`-Requests intern zum Backend. Backend und PostgreSQL brauchen keine externen Ports — sie kommunizieren über das Docker-interne Netzwerk.

---

### 57. Wie startet man die gesamte Produktionsumgebung?

- [A] `docker run iban`
- [B] `docker compose up --build`
- [C] `docker build -t iban .`
- [D] `npm start`

**Antwort: [B]**
`docker compose up --build` baut alle Docker-Images (Backend, Frontend) neu und startet alle 3 Services. Die App ist dann unter `http://localhost` erreichbar (Port 80).

---

## Block 7 — Frontend (React, TypeScript, Vite)

### 58. Welches Frontend-Framework wird in diesem Projekt verwendet?

- [A] Angular mit Material UI
- [B] Vue.js mit Vuetify
- [C] React mit TypeScript, Vite und Tailwind CSS
- [D] Svelte mit SvelteKit

**Antwort: [C]**
Das Frontend nutzt React 18+ mit TypeScript (strict mode), Vite als Build-Tool und Dev-Server, Tailwind CSS für Styling und shadcn/ui für UI-Komponenten.

---

### 59. Was ist shadcn/ui und wie unterscheidet es sich von z. B. Material UI?

- [A] Ein npm-Package, das als Dependency installiert wird
- [B] Kopierbarer Source-Code für UI-Komponenten, der ins Projekt eingebettet wird
- [C] Ein CSS-Framework ohne JavaScript
- [D] Ein Backend-Framework für REST-APIs

**Antwort: [B]**
shadcn/ui ist keine Dependency — die Komponenten werden als Source-Code ins Projekt kopiert (z. B. `components/ui/button.tsx`). Sie nutzen Tailwind CSS + `class-variance-authority` und sind vollständig anpassbar. Im Projekt: Badge, Button, Card, Input, Label.

---

### 60. Was macht die Funktion `formatIban()` im Frontend?

- [A] Sie sendet die IBAN an die API
- [B] Sie entfernt Nicht-Alphanumerisches, konvertiert zu Uppercase und gruppiert in 4er-Blöcke
- [C] Sie validiert die IBAN per Modulo 97
- [D] Sie speichert die IBAN in localStorage

**Antwort: [B]**
`formatIban()` bereinigt die Eingabe (`replace(/[^a-zA-Z0-9]/g, '')`), konvertiert zu Uppercase und formatiert in 4er-Gruppen mit Leerzeichen (DIN 5008). Beispiel: `de89370400440532013000` → `DE89 3704 0044 0532 0130 00`.

---

### 61. Was macht `cleanIban()` und warum wird es vor dem API-Aufruf verwendet?

- [A] Es formatiert die IBAN in 4er-Gruppen
- [B] Es entfernt alle Nicht-Alphanumerischen Zeichen und konvertiert zu Uppercase
- [C] Es validiert die Prüfziffern
- [D] Es speichert die IBAN in der Datenbank

**Antwort: [B]**
`cleanIban()` entfernt Leerzeichen, Bindestriche, Punkte etc. und macht alles Uppercase. Die API erwartet eine reine alphanumerische IBAN ohne Trennzeichen — die Formatierung ist nur für die Anzeige.

---

### 62. Warum wird im API-Client `fetch` statt `axios` verwendet?

- [A] `axios` ist in TypeScript nicht verfügbar
- [B] `fetch` ist die native Browser-API und benötigt keine zusätzliche Dependency
- [C] `fetch` ist schneller als `axios`
- [D] `axios` unterstützt kein JSON

**Antwort: [B]**
`fetch` ist die native Web-API — keine zusätzliche Dependency nötig. Für die drei einfachen API-Aufrufe (validate, save, getAll) reicht `fetch` vollkommen aus. `axios` wäre erst bei komplexeren Anforderungen (Interceptors, automatische Retries) sinnvoll.

---

### 63. Wie löst Vite den Import-Alias `@/components/...` auf?

- [A] Über eine Webpack-Konfiguration
- [B] Über die `resolve.alias`-Konfiguration in `vite.config.ts`, die `@` auf `./src` mappt
- [C] Über eine globale TypeScript-Einstellung
- [D] Das ist ein Node.js-Native-Feature

**Antwort: [B]**
In `vite.config.ts`: `resolve: { alias: { '@': path.resolve(__dirname, './src') } }`. Das mapped `@/components/IbanInput` auf `./src/components/IbanInput`. Die `tsconfig.app.json` hat das gleiche Mapping für TypeScript-Typchecking.

---

### 64. Was ist der Unterschied zwischen den beiden Buttons „Validieren" und „Validieren & Speichern"?

- [A] Beide machen den gleichen API-Aufruf
- [B] „Validieren" ruft `POST /api/ibans/validate` auf (ohne Speicherung), „Speichern" ruft `POST /api/ibans` auf (mit DB-Speicherung)
- [C] „Validieren" prüft nur im Frontend, „Speichern" prüft im Backend
- [D] „Validieren" nutzt die lokale API, „Speichern" nutzt openiban.com

**Antwort: [B]**
Zwei separate Endpunkte: `POST /api/ibans/validate` validiert die IBAN und gibt das Ergebnis zurück, `POST /api/ibans` validiert UND speichert in der Datenbank. Die Validierungslogik ist identisch (DRY über `buildResponse()`).

---

### 65. Warum verwendet die IBAN-Eingabe die CSS-Klasse `font-mono tracking-wider`?

- [A] Es ist eine Pflicht von shadcn/ui
- [B] Monospace-Schrift und breiteres Tracking machen IBANs lesbarer (feste Zeichenbreite)
- [C] Es beschleunigt das Rendering
- [D] Es verhindert Copy-Paste-Fehler

**Antwort: [B]**
Monospace-Schrift (`font-mono`) gibt jedem Zeichen die gleiche Breite — wichtig für IBANs mit 15–34 Zeichen. `tracking-wider` vergrößert den Buchstabenabstand. Zusammen mit der 4er-Gruppierung wird die IBAN deutlich lesbarer.

---

### 66. Was passiert im Frontend, wenn der Benutzer die Eingabe ändert?

- [A] Die IBAN wird sofort an die API geschickt
- [B] Das vorherige Validierungsergebnis wird gelöscht und die Eingabe neu formatiert
- [C] Die Seite wird neu geladen
- [D] Die Eingabe wird direkt in die Datenbank gespeichert

**Antwort: [B]**
Bei `onChange` wird `formatIban()` auf die Eingabe angewendet und der Validierungs-State zurückgesetzt (`result: null, error: null`). Die API wird erst beim Button-Klick aufgerufen — nicht bei jeder Eingabeänderung.

---

### 67. Wann lädt die `IbanList`-Komponente die gespeicherten IBANs?

- [A] Bei jedem Render
- [B] Einmalig beim Mounten via `useEffect` mit leerem Dependency-Array
- [C] Alle 5 Sekunden per Polling
- [D] Nur wenn der Benutzer einen Button klickt

**Antwort: [B]**
`useEffect(() => { void fetchIbans() }, [fetchIbans])` — die Liste wird einmalig beim Mounten geladen. `fetchIbans` ist mit `useCallback` memoisiert und hat ein leeres Dependency-Array `[]`, sodass es sich nie ändert.

---

## Block 8 — Testing (JUnit, MockMvc, Vitest)

### 68. Was ist der Unterschied zwischen den Unit-Tests und den Integrationstests im Backend?

- [A] Kein Unterschied — beide brauchen den vollen Spring-Kontext
- [B] Unit-Tests testen den Service direkt (kein Spring), Integrationstests laden den Web-Layer mit MockMvc
- [C] Unit-Tests laufen im Browser, Integrationstests auf dem Server
- [D] Integrationstests testen nur die Datenbank

**Antwort: [B]**
`IbanValidationServiceTest` sind pure Unit-Tests: `new IbanValidationService()` ohne Spring. `IbanControllerTest` nutzt `@WebMvcTest` für einen Mini-Spring-Kontext (nur Web-Layer) mit MockMvc als Fake-HTTP-Client und gemockten Services.

---

### 69. Was ist `@WebMvcTest(IbanController.class)`?

- [A] Es startet die gesamte Spring-Boot-Anwendung inkl. Datenbank
- [B] Es lädt nur den Web-Layer (Controller + JSON-Serialisierung + Validation), keine DB/Services
- [C] Es ist ein Frontend-Test-Framework
- [D] Es generiert automatisch API-Dokumentation

**Antwort: [B]**
`@WebMvcTest` startet einen "dünnen" Spring-Kontext: nur Controller, Jackson-Serialisierung und Jakarta Validation. Keine echten Services, keine Datenbank. Abhängigkeiten werden per `@MockitoBean` gemockt. Analog zu Supertest mit isoliertem Router in Express.

---

### 70. Was bewirkt `@MockitoBean` in den Controller-Tests?

- [A] Es erstellt einen echten Service mit Datenbankzugriff
- [B] Es erstellt eine Mock-Implementierung und registriert sie als Spring Bean
- [C] Es deaktiviert den Test
- [D] Es startet einen separaten Testserver

**Antwort: [B]**
`@MockitoBean` erstellt ein Mockito-Mock-Objekt und registriert es im Spring-Kontext als Bean. Der Controller erhält den Mock per Constructor Injection statt des echten Services. Analog zu `vi.mock('./service')` in Vitest.

---

### 71. Was macht `when(service.validate("DE89...")).thenReturn(result)`?

- [A] Es testet, ob `validate` aufgerufen wurde
- [B] Es definiert, was der Mock zurückgeben soll, wenn er mit diesen Argumenten aufgerufen wird
- [C] Es ruft die echte `validate`-Methode auf
- [D] Es wartet auf das Ergebnis der Validierung

**Antwort: [B]**
`when().thenReturn()` ist Mockito-Syntax zum Definieren von Mock-Verhalten: „Wenn `validate` mit genau diesem Argument aufgerufen wird, gib dieses Ergebnis zurück." Analog zu `vi.mocked(service.validate).mockReturnValue({...})` in Vitest.

---

### 72. Was ist MockMvc?

- [A] Ein echtes HTTP-Netzwerk-Tool
- [B] Ein Fake-HTTP-Client, der Requests direkt an den Controller schickt (ohne Netzwerk)
- [C] Ein Datenbank-Mock
- [D] Ein CSS-Testing-Framework

**Antwort: [B]**
MockMvc simuliert HTTP-Requests, ohne einen echten Server zu starten. Requests gehen direkt an den DispatcherServlet. `perform(post("/api/ibans/validate").content(...))` ist analog zu `supertest(app).post('/api/ibans/validate').send(...)` in Node.js.

---

### 73. Wie prüft man in MockMvc, ob ein JSON-Feld einen bestimmten Wert hat?

- [A] `andExpect(content("valid"))`
- [B] `andExpect(jsonPath("$.valid").value(true))`
- [C] `andAssert(json.valid == true)`
- [D] `assertEquals(response.valid, true)`

**Antwort: [B]**
JsonPath-Ausdrücke (`$.valid`, `$.bankName`) prüfen einzelne Felder der JSON-Response. `andExpect(jsonPath("$.valid").value(true))` ist analog zu `expect(res.body.valid).toBe(true)` in Vitest/Jest.

---

### 74. Warum braucht der Test `validateEmptyIbanReturnsBadRequest` kein Mock-Setup?

- [A] Weil leere IBANs automatisch gültig sind
- [B] Weil `@Valid` + `@NotBlank` die Anfrage ablehnt, BEVOR die Controller-Methode aufgerufen wird
- [C] Weil MockMvc keine Mocks unterstützt
- [D] Weil die Datenbank den Fehler abfängt

**Antwort: [B]**
`@Valid` auf dem Parameter und `@NotBlank` auf `IbanRequest.iban` sorgen dafür, dass Spring die Validierung durchführt, bevor die Controller-Methode aufgerufen wird. Der Service-Mock wird nie erreicht — daher kein `when().thenReturn()` nötig.

---

### 75. In welcher Reihenfolge stehen bei JUnit 5 `assertEquals()` die Parameter?

- [A] `assertEquals(actual, expected)`
- [B] `assertEquals(expected, actual)`
- [C] Die Reihenfolge ist egal
- [D] `assertEquals(message, expected, actual)`

**Antwort: [B]**
JUnit-Konvention: Erwartet zuerst, dann tatsächlich: `assertEquals("Commerzbank", result.bankName())`. Das ist umgekehrt zu Vitest/Jest: `expect(result.bankName).toBe("Commerzbank")`. Bei Fehlern zeigt JUnit: „Expected: Commerzbank, Actual: null".

---

### 76. Was ist Vitest und wofür wird es im Frontend verwendet?

- [A] Ein Bundler für React-Komponenten
- [B] Ein Test-Framework, das nativ mit Vite integriert ist (Vitest + jsdom + React Testing Library)
- [C] Ein Linting-Tool für TypeScript
- [D] Ein HTTP-Client für API-Tests

**Antwort: [B]**
Vitest ist ein schnelles Test-Framework, das direkt den Vite-Bundler nutzt. Im Projekt wird es mit jsdom (DOM-Simulation) und React Testing Library (Rendering + Assertions) für Komponenten-Tests verwendet.

---

### 77. Was testen die drei Frontend-Tests in `IbanInput.test.tsx`?

- [A] Die Modulo-97-Validierung im Browser
- [B] Ob Input-Feld, Validieren-Button und Speichern-Button gerendert werden (Smoke-Tests)
- [C] End-to-End-Flows mit echtem Backend
- [D] Datenbank-Abfragen

**Antwort: [B]**
Die drei Tests sind Smoke-Tests: Sie prüfen, ob die Kernelemente (`placeholder`, „Validieren"-Button, „Validieren & Speichern"-Button) korrekt gerendert werden. Es gibt noch keine Tests für User-Interaktion oder API-Aufrufe.

---

### 78. Wie werden alle Backend-Tests ausgeführt?

- [A] `mvn test` oder `mvn verify -B`
- [B] `npm test`
- [C] `java -jar test.jar`
- [D] `gradle test`

**Antwort: [A]**
`mvn test` führt alle JUnit-5-Tests aus. `mvn verify -B` macht dasselbe im Batch-Modus (keine interaktive Eingabe) — ideal für CI/CD. Die Frontend-Tests laufen separat mit `pnpm test`.

---

## Block 9 — API Design & Error Handling

### 79. Welche drei API-Endpunkte bietet das Backend?

- [A] `GET /validate`, `POST /save`, `DELETE /ibans`
- [B] `POST /api/ibans/validate`, `POST /api/ibans`, `GET /api/ibans`
- [C] `PUT /api/ibans`, `PATCH /api/ibans`, `GET /api/ibans`
- [D] `GET /api/validate/{iban}`, `POST /api/save`, `GET /api/list`

**Antwort: [B]**
Drei Endpunkte: `POST /api/ibans/validate` (Validierung ohne Speicherung), `POST /api/ibans` (Validierung + Speicherung) und `GET /api/ibans` (alle gespeicherten IBANs abrufen).

---

### 80. Was bedeutet `ResponseEntity<IbanResponse>` als Rückgabetyp?

- [A] Es gibt immer HTTP 200 zurück
- [B] Es erlaubt, HTTP-Statuscode und Body als typisiertes Objekt zusammen zurückzugeben
- [C] Es serialisiert automatisch zu XML
- [D] Es ist ein Fehler-Wrapper

**Antwort: [B]**
`ResponseEntity` ist ein Wrapper, der HTTP-Statuscode, Headers und Body kombiniert. `ResponseEntity.ok(body)` = Status 200 + JSON-Body. Man könnte auch `ResponseEntity.status(201).body(...)` für andere Statuscodes verwenden.

---

### 81. Was macht `@RestControllerAdvice` im Projekt?

- [A] Es loggt alle Requests
- [B] Es ist ein globaler Error-Handler, der Exceptions fängt und konsistente JSON-Fehlerantworten liefert
- [C] Es generiert API-Dokumentation
- [D] Es aktiviert CORS

**Antwort: [B]**
`@RestControllerAdvice` im `GlobalExceptionHandler` fängt Exceptions aus allen Controllern: `MethodArgumentNotValidException` → HTTP 400, allgemeine `Exception` → HTTP 500. Jede Fehlerantwort ist konsistentes JSON. Analog zu Express Error-Handling Middleware.

---

### 82. Was passiert, wenn ein Client `{"iban": ""}` an `/api/ibans/validate` schickt?

- [A] Die IBAN wird als gültig validiert
- [B] `@NotBlank` greift → HTTP 400 mit strukturierter Fehlermeldung
- [C] Der Server gibt HTTP 500 zurück
- [D] Die leere IBAN wird in der Datenbank gespeichert

**Antwort: [B]**
`@Valid` auf dem Parameter triggert die Jakarta-Validation. `@NotBlank` auf `IbanRequest.iban` lehnt leere Strings und `null` ab. Der `GlobalExceptionHandler` fängt die `MethodArgumentNotValidException` und gibt HTTP 400 mit JSON-Fehlermeldung zurück.

---

### 83. Was ist „Graceful Degradation" im Kontext der externen API?

- [A] Die Anwendung stürzt kontrolliert ab
- [B] Wenn openiban.com nicht erreichbar ist, funktioniert die App trotzdem — nur ohne Bankname
- [C] Die Datenbank wird beim Fehler automatisch bereinigt
- [D] Das Frontend zeigt einen Ladebalken

**Antwort: [B]**
Die externe API (openiban.com) ist in einen `try/catch` gewickelt. Bei Fehler wird `null` zurückgegeben — der Controller nutzt dann nur die lokale Validierung. Die IBAN wird trotzdem korrekt als gültig/ungültig erkannt, nur der Bankname fehlt möglicherweise.

---

### 84. Was bedeutet `validationMethod: "local"` vs. `"external"` in der API-Response?

- [A] `local` = Frontend-Validierung, `external` = Backend-Validierung
- [B] `local` = Eigen-Implementierung (Mod 97 + BLZ-Lookup), `external` = Bankname über openiban.com aufgelöst
- [C] `local` = Datenbank-Abfrage, `external` = Cache-Abfrage
- [D] `local` = Docker, `external` = Cloud

**Antwort: [B]**
`local` bedeutet, Mod-97-Validierung und BLZ-Auflösung liefen komplett mit eigener Logik. `external` bedeutet, der Bankname wurde über die openiban.com-API aufgelöst, weil die BLZ nicht in der lokalen `KNOWN_BANKS`-Map war.

---

### 85. Was ist der `RestClient` in Spring und wofür wird er im Projekt verwendet?

- [A] Ein Datenbank-Client
- [B] Ein fluent HTTP-Client (seit Spring 6.1) für externe API-Aufrufe
- [C] Ein Test-Framework
- [D] Ein WebSocket-Client

**Antwort: [B]**
`RestClient` (seit Spring 6.1) ist ein fluenter HTTP-Client — Nachfolger von `RestTemplate`. Im Projekt wird er in `ExternalIbanApiService` für den Aufruf von `openiban.com/validate/{iban}` verwendet. Analog zu `fetch()` oder `axios` in JavaScript.

---

### 86. Werden auch ungültige IBANs in der Datenbank gespeichert?

- [A] Nein, nur gültige IBANs werden gespeichert
- [B] Ja, beim Endpunkt `POST /api/ibans` werden sowohl gültige als auch ungültige IBANs gespeichert
- [C] Nur wenn der Benutzer es explizit bestätigt
- [D] Nur bei externen Validierungen

**Antwort: [B]**
`validateAndSaveIban()` speichert das Validierungsergebnis unabhängig davon, ob die IBAN gültig ist. Das `valid`-Feld in der DB speichert den Status. Das ermöglicht eine Audit-Historie aller Validierungsversuche.

---

## Block 10 — Architecture Decisions

### 87. Warum wurde PostgreSQL statt H2 (embedded DB) gewählt?

- [A] H2 ist nicht kompatibel mit Java 21
- [B] PostgreSQL zeigt realistische Produktions-Patterns: Flyway, Docker, Environment-Variablen
- [C] H2 ist langsamer als PostgreSQL
- [D] Spring Boot unterstützt H2 nicht

**Antwort: [B]**
H2 wäre einfacher (kein Docker-Container nötig), aber PostgreSQL zeigt Patterns, die in Produktion relevant sind: Flyway-Migrations gegen eine echte DB, Docker Compose Service-Orchestrierung, `depends_on` mit Healthchecks, Environment-Variablen für Credentials (ADR #4).

---

### 88. Warum wird Maven statt Gradle verwendet?

- [A] Gradle unterstützt kein Java 21
- [B] Maven (`pom.xml`) ist deklarativ und für Java-Neulinge leichter verständlich als Gradle (Groovy/Kotlin DSL)
- [C] Maven ist schneller als Gradle
- [D] Spring Boot erfordert Maven

**Antwort: [B]**
Maven's `pom.xml` ist deklaratives XML — konzeptionell nah an `package.json`. Gradle erfordert zusätzlich Groovy oder Kotlin DSL. Für ein einfaches Projekt (compile → test → package) bringen Gradle's Stärken (Build-Scripts, inkrementelle Builds) keinen Vorteil (ADR #12).

---

### 89. Warum verwendet das Projekt Java 21 statt Java 25?

- [A] Java 25 existiert noch nicht
- [B] Java 21 ist der aktuelle LTS-Release mit voller Ökosystem-Unterstützung; Java 25 ist erst 6 Monate alt
- [C] Java 25 hat keine Records
- [D] Maven unterstützt Java 25 nicht

**Antwort: [B]**
Java 21 (September 2023) ist der industrie-standard LTS-Release. Spring Boot 3.x, Hibernate, Mockito sind vollständig dagegen getestet. Java 25 (September 2025) ist erst 6 Monate alt — Teams migrieren LTS-zu-LTS nach 6–12 Monaten GA-Reife (ADR #11).

---

### 90. Warum wird Spring Boot 3.5.x statt 4.0.x verwendet?

- [A] Spring Boot 4.0 existiert noch nicht
- [B] 3.5.x ist ein non-breaking Upgrade von 3.4.x; 4.0 erfordert umfangreiche Migrationsarbeit ohne Lerngewinn
- [C] 3.5.x ist schneller als 4.0.x
- [D] 4.0 unterstützt kein PostgreSQL

**Antwort: [B]**
3.4→3.5 ist ein Minor-Upgrade (gleiches Jakarta EE 10, Jackson 2, Spring Framework 6.x). 4.0 erfordert neue Starter-Namen, Jackson 3, `@MockitoBean`-Migration etc. — das ist Migrationsarbeit ohne Bezug zu den Lernzielen dieses Projekts (ADR #14).

---

### 91. Was bedeutet „Convention over Configuration" in Spring Boot?

- [A] Man muss jede Konfiguration manuell schreiben
- [B] Spring konfiguriert sinnvolle Defaults automatisch — man überschreibt nur bei Bedarf
- [C] Es gibt keine Konfigurationsdateien
- [D] Konventionen werden vom Frontend vorgegeben

**Antwort: [B]**
Man sagt Spring was man will (über Annotations/Dependencies), Spring konfiguriert das wie automatisch. Beispiel: Füge `spring-boot-starter-web` hinzu → Tomcat, Jackson, Routing sind automatisch konfiguriert. Man überschreibt nur Abweichungen (z. B. `server.port=8080`).

---

### 92. Warum wird kein Lombok im Projekt verwendet?

- [A] Lombok ist in Java 21 nicht verfügbar
- [B] Expliziter Code ist für Lernzwecke verständlicher; Records ersetzen Lombok für DTOs
- [C] Lombok ist ein Sicherheitsrisiko
- [D] Spring Boot verbietet Lombok

**Antwort: [B]**
Lombok generiert Code unsichtbar zur Compile-Zeit (Getter, Setter, Konstruktoren). Für Lernzwecke ist expliziter Code besser verständlich. Java Records ersetzen Lombok für DTOs (immutable, kein Boilerplate). Die Entity `Iban.java` nutzt handgeschriebene Getter.

---

### 93. Warum liegt das Schema in SQL-Dateien (Flyway) und nicht in JPA-Annotations?

- [A] JPA unterstützt keine Schema-Definition
- [B] SQL-Dateien sind die Schema-Quelle (schema-first); Hibernate validiert nur, ändert aber nie das Schema
- [C] Flyway ist schneller als Hibernate
- [D] SQL-Dateien sind einfacher als Annotations

**Antwort: [B]**
Schema-first-Ansatz: Die SQL-Datei (`V1__initial_schema.sql`) ist die Single Source of Truth für das DB-Schema. `ddl-auto=validate` stellt sicher, dass Entity und Schema übereinstimmen, ohne dass Hibernate riskante Schema-Änderungen macht (ADR #6).

---

## Block 11 — Security & CORS

### 94. Was ist CORS und warum wird es in der Entwicklung gebraucht?

- [A] Ein Verschlüsselungsprotokoll
- [B] Cross-Origin Resource Sharing — erlaubt dem Frontend auf `localhost:5173` Requests an das Backend auf `localhost:8080` zu senden
- [C] Ein Datenbank-Zugriffsprotokoll
- [D] Ein Docker-Netzwerk-Feature

**Antwort: [B]**
Browser blockieren Requests an andere Origins (Port = anderer Origin). In der Entwicklung läuft das Frontend auf Port 5173 (Vite) und das Backend auf Port 8080. CORS-Header erlauben diese Cross-Origin-Requests. In Produktion nicht nötig (Nginx proxied alles über Port 80).

---

### 95. Warum ist die CORS-Konfiguration mit `@Profile("dev")` annotiert?

- [A] Damit CORS immer aktiv ist
- [B] Damit CORS nur im Entwicklungsprofil aktiv ist — in Produktion nicht nötig
- [C] CORS funktioniert nur mit dem dev-Profil
- [D] Es ist eine Spring-Boot-Pflicht

**Antwort: [B]**
In Produktion wird alles über Nginx auf Port 80 ausgeliefert (Same Origin) — CORS ist irrelevant. `@Profile("dev")` stellt sicher, dass die offene CORS-Config nur beim Entwickeln aktiv ist, aktiviert mit `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

---

### 96. Was bewirkt `@Value("${app.cors.allowed-origins:http://localhost}")` in der CORS-Config?

- [A] Es setzt einen festen Wert
- [B] Es liest den Wert aus der Property-Datei, mit `http://localhost` als Fallback
- [C] Es definiert eine Umgebungsvariable
- [D] Es erstellt einen neuen Bean

**Antwort: [B]**
`@Value("${key:default}")` liest einen Wert aus `application-dev.properties`. Falls der Key nicht existiert, wird der Default (`http://localhost`) verwendet. Analog zu `process.env.CORS_ORIGINS ?? 'http://localhost'` in Node.js.

---

### 97. Welche Security-Headers setzt die Nginx-Konfiguration?

- [A] Keine
- [B] `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`
- [C] Nur `Content-Security-Policy`
- [D] Nur `Strict-Transport-Security`

**Antwort: [B]**
Die `nginx.conf` setzt drei Security-Headers: `X-Content-Type-Options: nosniff` (verhindert MIME-Sniffing), `X-Frame-Options: DENY` (verhindert Iframe-Embedding) und `Referrer-Policy: no-referrer-when-downgrade`. HSTS fehlt, weil kein HTTPS konfiguriert ist.

---

## Block 12 — DDD Concepts

### 98. Was ist „Primitive Obsession" und wie zeigt sie sich im aktuellen Code?

- [A] Zu viele primitive Datentypen werden als Konstanten gespeichert
- [B] Die IBAN wird überall als `String` durchgereicht statt als fachlicher Typ
- [C] Es werden zu viele int-Variablen verwendet
- [D] Alle Felder sind public

**Antwort: [B]**
Primitive Obsession = fachliche Konzepte werden durch primitive Typen dargestellt. Die IBAN ist überall ein `String` — nichts verhindert, dass ein nicht-normalisierter String verwendet wird. Ein Value Object wie `IbanNumber` würde den fachlichen Typ abbilden und Normalisierung im Konstruktor erzwingen.

---

### 99. Was ist ein Value Object im DDD-Kontext?

- [A] Ein Objekt mit einer Datenbank-ID
- [B] Ein immutables Objekt, das durch seine Werte definiert ist (nicht durch eine Identität)
- [C] Ein Objekt, das nur Getter hat
- [D] Ein JSON-Objekt

**Antwort: [B]**
Ein Value Object ist durch seine Werte definiert, nicht durch eine ID. Zwei `IbanNumber("DE89...")` sind gleich, wenn der Wert gleich ist. Value Objects sind immutable und können sich selbst validieren/normalisieren. Analog zu `Readonly<{value: string}>` in TypeScript.

---

### 100. Was ist das „Anemic Domain Model"-Problem im aktuellen Code?

- [A] Die Datenbank hat zu wenige Spalten
- [B] Die Entity `Iban.java` ist ein reiner Datenbehälter ohne fachliches Verhalten
- [C] Es gibt zu viele Services
- [D] Die API hat zu wenige Endpunkte

**Antwort: [B]**
`Iban.java` hat nur Felder + Getter, kein Verhalten. Die Fachlogik (Normalisierung, Mod-97, BLZ-Extraktion) steckt im `IbanValidationService`. In DDD gehört fachliches Verhalten in die Domänenobjekte — z. B. `IbanNumber.countryCode()`, `IbanNumber.bban()`.

---

### 101. Was ist die „Dependency Rule" in der Clean Architecture?

- [A] Dependencies müssen immer aktuell sein
- [B] Abhängigkeiten zeigen immer nach innen — die Domain kennt keine Frameworks
- [C] Jede Klasse muss eine Dependency haben
- [D] Dependencies müssen alphabetisch sortiert sein

**Antwort: [B]**
Die Dependency Rule besagt: Äußere Schichten (Controller, Infrastruktur) kennen innere (Domain), aber nie umgekehrt. Die Domain definiert Interfaces (Ports), die von der Infrastruktur implementiert werden (Adapters). Die Domain ist framework-frei.

---

### 102. Was ist ein „Port" in der Hexagonalen Architektur?

- [A] Ein Netzwerk-Port wie 8080
- [B] Ein Interface an der Domänengrenze, das definiert was die Domain braucht
- [C] Ein Docker-Port-Mapping
- [D] Ein Maven-Plugin

**Antwort: [B]**
Ein Port ist ein Interface, das die Domain definiert: „Ich brauche jemanden, der Bankinformationen extern auflöst." Die Implementierung (`OpenIbanApiAdapter` → openiban.com) ist ein Adapter. Die Domain weiß nicht, ob REST, SOAP oder ein Mock dahintersteckt.

---

### 103. Warum wird das vollständige DDD-Refactoring (Hexagonale Architektur) im Projekt NICHT umgesetzt?

- [A] DDD ist veraltet
- [B] Die Domänen-Komplexität rechtfertigt die Architektur-Komplexität nicht (YAGNI)
- [C] Spring Boot unterstützt DDD nicht
- [D] Die Tests würden nicht mehr funktionieren

**Antwort: [B]**
Das Projekt hat ~130 Zeilen Logik, 1 Domänenregel, 1 DB-Tabelle. Vollständiges DDD (20+ Dateien, 10+ Packages) wäre Indirektion ohne Nutzen. Drei gezielte Verbesserungen (Value Object, Orchestrierung verschieben, Mod97Validator extrahieren) lösen die realen Probleme.

---

### 104. Welches Prinzip beschreibt am besten die Architektur-Entscheidung dieses Projekts?

- [A] Over-Engineering für maximale Flexibilität
- [B] YAGNI + proportionale Architektur — Komplexität passt zur Domäne
- [C] Alle Design Patterns immer anwenden
- [D] Architektur spielt keine Rolle

**Antwort: [B]**
YAGNI (You Aren't Gonna Need It) + proportionale Architektur: Die Architektur-Komplexität soll proportional zur Domänen-Komplexität sein. Interfaces, Ports, JPA/Domain-Trennung werden erst eingeführt, wenn ein realer Bedarf entsteht — nicht prophylaktisch.
