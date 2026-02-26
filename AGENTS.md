# AGENTS.md — IBAN Validator SPA

> Kontext-Datei für Coding-Agents (GitHub Copilot, Cursor, etc.).
> Projektfakten (Setup, API, Struktur) stehen in der [README.md](README.md) — hier nicht dupliziert.

---

## Dokumentations-Landkarte

Dieses Projekt hat mehrere Markdown-Dateien mit unterschiedlichen Zwecken. **Lies die richtige Datei für die richtige Aufgabe:**

| Datei                                   | Inhalt                                                                   | Wann relevant?                                                    |
| --------------------------------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| [README.md](README.md)                  | Projekt-Setup, Architektur, API-Endpunkte, Test-Befehle, Projektstruktur | **Immer** — Single Source of Truth für Projektfakten              |
| [AGENTS.md](AGENTS.md) _(diese Datei)_  | Entwicklerprofil, Kommunikationsregeln, Coding-Konventionen              | **Immer** — regelt WIE du mit dem Entwickler kommunizierst        |
| [iban.md](docs/iban.md)                 | Fachliches Wissen: IBAN-Aufbau, Modulo-97-Algorithmus, BLZ, SEPA         | Änderungen an **Validierungslogik**, IBAN-Parsing, Fachbegriffe   |
| [decisions.md](docs/decisions.md)       | Architecture Decision Records (ADRs) mit Begründungen                    | **Architektur-Fragen**, warum etwas so ist wie es ist             |
| [lernfragen.md](docs/lernfragen.md)     | Java/Spring-Boot-Lernguide mit TS/Node/Go-Analogien                      | **Backend-Code verstehen**, Konzepte erklären, Annotations deuten |
| [future.md](docs/future.md)             | Backlog möglicher Erweiterungen (Auth, HTTPS, CI/CD, Dark Mode etc.)     | Neue Features planen, Erweiterbarkeit diskutieren                 |
| [presentation.md](docs/presentation.md) | Präsentations-Skript für das Vorstellungsgespräch                        | Präsentation vorbereiten oder überarbeiten                        |

---

## Entwicklerprofil — Nico Gräf

> **Wichtig für alle Coding-Agents**: Nico ist ein erfahrener Senior-Fullstack-Entwickler, der Java und Spring Boot jedoch **nicht** aus der täglichen Arbeit kennt. Er möchte jedes Detail dieses Projekts **verstehen und erklären können**. Generiert daher bei Backend-Änderungen immer kurze, verständliche Erklärungen (Kommentare im Code oder Erklärungen in der Antwort), die Java/Spring-Boot-Konzepte auf Konzepte aus seiner bekannten Welt (TypeScript, Node.js, Go) mappen.

### Auf einen Blick

| Eigenschaft   | Detail                                                                      |
| ------------- | --------------------------------------------------------------------------- |
| **Name**      | Nico Gräf                                                                   |
| **Rolle**     | Senior Software Engineer, ehem. Team Lead                                   |
| **Standort**  | Freiburg im Breisgau, Deutschland                                           |
| **Erfahrung** | 10+ Jahre Softwareentwicklung                                               |
| **Stärke**    | Fullstack-Webentwicklung, moderne SPA-Architektur, Cloud, DDD, Event-Driven |
| **Website**   | https://nicograef.de                                                        |

### Starke Kenntnisse (tägliche Praxis / Produktionserfahrung)

| Bereich               | Technologien                                                                  |
| --------------------- | ----------------------------------------------------------------------------- |
| **Sprachen**          | TypeScript, JavaScript (10+ J.), Go (3+ J.), SQL, HTML, CSS                   |
| **Frontend**          | React (Hauptframework), Vue.js, Angular, Tailwind CSS, shadcn/ui, Material UI |
| **Backend / Runtime** | Node.js, Express, Go-Services                                                 |
| **Datenbanken**       | PostgreSQL, MongoDB, Firebase/Firestore, CouchDB                              |
| **Cloud / Infra**     | AWS (Serverless, CDK), Google Cloud, Docker, nginx, Linux                     |
| **Architektur**       | REST APIs, Event-Driven, DDD, Serverless, E2EE, JWT, PWA, Electron            |
| **Testing**           | Vitest, React Testing Library, Jest, Test Coverage, CI/CD (Travis CI, GitLab) |
| **Tools / Sonstiges** | Git, GitHub, GitLab, Vite, pnpm/npm, zod, D3.js, Shopify                      |

### Grundkenntnisse (Studium / Nebenprojekte, ~2015–2016)

| Bereich             | Technologien                                                                  |
| ------------------- | ----------------------------------------------------------------------------- |
| **Sprachen**        | Java (Android-App, Sudoku-Solver, ML-Projekt), Python (ML), C++ (Arduino/UE4) |
| **Java-spezifisch** | Grundlegende OOP, Android SDK, Desktop-GUIs — aber kein Enterprise-Java       |

### Keine / kaum Erfahrung (Lernbedarf in diesem Projekt)

Diese Technologien sind **neu für Nico** und brauchen Erklärung:

| Technologie / Konzept      | Mapping auf bekannte Welt                                                   |
| -------------------------- | --------------------------------------------------------------------------- |
| **Spring Boot**            | ≈ Express.js / Go net/http mit Convention-over-Configuration                |
| **Spring Annotations**     | `@RestController` ≈ Express-Router, `@Service` ≈ Go struct mit Methoden     |
| **Dependency Injection**   | Spring DI ≈ manuelles Wiring in Go / Angulars DI-System                     |
| **Maven (pom.xml)**        | ≈ package.json + pnpm, Dependency-Management & Build-Tool                   |
| **Spring Data JPA**        | ≈ Prisma / TypeORM / GORM — ORM mit Repository-Pattern                      |
| **JPA Entities / Records** | ≈ TypeScript-Interfaces + DB-Schema, Java Records ≈ TS `type` / Go `struct` |
| **Flyway Migrations**      | ≈ Prisma Migrate / golang-migrate — SQL-Dateien für Schema-Versionierung    |
| **JUnit 5 + MockMvc**      | ≈ Vitest/Jest + Supertest — Unit- & Integrationstests                       |
| **application.properties** | ≈ .env-Dateien — Konfiguration pro Umgebung                                 |
| **@RestControllerAdvice**  | ≈ Express error-handling Middleware / Go error-Wrapping                     |
| **Java Records**           | ≈ TypeScript `type`/`interface` oder Go `struct` — immutable Datenklassen   |
| **BigInteger (Mod 97)**    | ≈ JavaScript BigInt — für große Zahlen bei der IBAN-Prüfzifferberechnung    |

### Lernziele für dieses Projekt

Nico will **alles verstehen und im Vorstellungsgespräch erklären können**:

1. Wie Spring Boot eine REST-API aufbaut (Controller → Service → Repository).
2. Wie Dependency Injection funktioniert und warum Spring es nutzt.
3. Wie JPA/Hibernate Entitäten auf DB-Tabellen mappt.
4. Wie Maven das Projekt baut und Abhängigkeiten verwaltet.
5. Wie die Modulo-97-Validierung intern funktioniert (BigInteger).
6. Wie der externe API-Aufruf (RestClient) an openiban.com funktioniert.
7. Wie die Tests (JUnit 5, MockMvc) strukturiert sind und was sie testen.
8. Wie Docker Compose alle drei Services (Frontend, Backend, DB) zusammenbringt.

---

## Kommunikationspräferenzen

- **Sprache**: Code und Kommentare auf Englisch, Erklärungen gerne auf Deutsch oder Englisch.
- **Erklärtiefe**: Bei Java/Spring-Code immer kurz erklären, was Annotationen und Patterns bedeuten.
- **Analogien**: Konzepte auf TypeScript/Node.js/Go/React mappen, wo sinnvoll.
- **Keine Abkürzungen**: Nicht davon ausgehen, dass Java-Idiome bekannt sind (z.B. Gradle vs Maven, Lombok, Bean-Lifecycle).
- **Tests erklären**: Bei Tests erklären, was `@WebMvcTest`, `@MockBean`, `MockMvc` etc. tun.
- **Immer lauffähig**: Änderungen sollten sofort testbar sein (Befehle zum Starten/Testen mitliefern).

---

## Coding-Konventionen

- Kein Lombok — plain Java Records für DTOs.
- Spring Data JPA für Persistenz.
- Constructor Injection (kein `@Autowired` auf Feldern).
- CORS nur in Dev-Profil offen, in Prod via Nginx-Proxy.
- Fehlerbehandlung: `@RestControllerAdvice` mit konsistenten Error-Responses.
- Environment-Variablen für DB-Credentials (nicht hardcoded).
- Frontend: Strict TypeScript, ESLint mit `--max-warnings=0`, Tailwind CSS + shadcn/ui.
- Schema-Quelle: Flyway SQL-Migrations (nicht Hibernate `ddl-auto`). Hibernate validiert nur.
