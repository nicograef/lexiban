# AGENTS.md — IBAN Validator SPA

## Projektübersicht

Single-Page-App (React + Spring Boot) zur Validierung und Speicherung von IBANs.
Coding-Challenge für eine Bewerbung als Softwareentwickler.

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
6. Wie der externe API-Aufruf (RestTemplate/WebClient) an openiban.com funktioniert.
7. Wie die Tests (JUnit 5, MockMvc) strukturiert sind und was sie testen.
8. Wie Docker Compose alle drei Services (Frontend, Backend, DB) zusammenbringt.

### Kommunikationspräferenzen für Agents

- **Sprache**: Code und Kommentare auf Englisch, Erklärungen gerne auf Deutsch oder Englisch.
- **Erklärtiefe**: Bei Java/Spring-Code immer kurz erklären, was Annotationen und Patterns bedeuten.
- **Analogien**: Konzepte auf TypeScript/Node.js/Go/React mappen, wo sinnvoll.
- **Keine Abkürzungen**: Nicht davon ausgehen, dass Java-Idiome bekannt sind (z.B. Gradle vs Maven, Lombok, Bean-Lifecycle).
- **Tests erklären**: Bei Tests erklären, was `@WebMvcTest`, `@MockBean`, `MockMvc` etc. tun.
- **Immer lauffähig**: Änderungen sollten sofort testbar sein (Befehle zum Starten/Testen mitliefern).

---

## Anforderungen

1. **Freie Benutzereingabe**: IBAN-Feld erlaubt Leerzeichen/Trennzeichen, diese werden vor dem Senden ans Backend entfernt.
2. **Backend-Validierung**: IBAN wird im Backend ohne Trennzeichen via eigener Prüfziffer-Logik (Modulo 97) validiert.
3. **Externe API als Fallback**: Zusätzlich Anbindung an openiban.com zur Validierung/Bankauflösung.
4. **Banknamen-Auflösung**: Für 3 bekannte Banken den Namen anzeigen (BLZ-basiert):
   - Deutsche Bank (BLZ: 50070010)
   - Commerzbank (BLZ: 50040000)
   - Sparkasse (z.B. Berliner Sparkasse, BLZ: 10050000)
5. **IBAN-Speicherung**: Validierte IBANs in PostgreSQL persistieren.
6. **Gespeicherte IBANs auflisten**: Liste der gespeicherten IBANs im Frontend anzeigen.

## Tech-Stack

| Komponente        | Technologie                     |
| ----------------- | ------------------------------- |
| Backend           | Java 21, Spring Boot 3.x, Maven |
| Frontend          | React 18+ (Vite, TypeScript)    |
| Datenbank         | PostgreSQL 17                   |
| API-Kommunikation | REST (JSON)                     |
| Containerisierung | Docker Compose                  |

## Projektstruktur

```
/
├── backend/                  # Spring Boot App
│   ├── pom.xml
│   ├── src/main/java/com/iban/
│   │   ├── IbanApplication.java
│   │   ├── controller/
│   │   │   └── IbanController.java
│   │   ├── service/
│   │   │   ├── IbanValidationService.java
│   │   │   └── ExternalIbanApiService.java
│   │   ├── model/
│   │   │   └── Iban.java
│   │   └── repository/
│   │       └── IbanRepository.java
│   └── src/test/java/com/iban/
│       ├── controller/IbanControllerTest.java
│       └── service/IbanValidationServiceTest.java
├── frontend/                 # React SPA (also serves as reverse proxy in prod)
│   ├── package.json
│   ├── nginx.conf            # Serves SPA + proxies /api to backend
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   └── IbanInput.tsx  # Contains IbanInput + IbanList
│   │   ├── services/
│   │   │   └── api.ts
│   │   └── __tests__/
│   │       └── IbanInput.test.tsx
│   └── vite.config.ts
├── docker-compose.yml        # 3 services: postgres, backend, frontend
├── AGENTS.md
├── DECISIONS.md
└── README.md
```

## API-Endpunkte

| Methode | Pfad                  | Beschreibung                       |
| ------- | --------------------- | ---------------------------------- |
| POST    | `/api/ibans/validate` | IBAN validieren (ohne Speicherung) |
| POST    | `/api/ibans`          | IBAN validieren und speichern      |
| GET     | `/api/ibans`          | Alle gespeicherten IBANs abrufen   |

### POST `/api/ibans/validate` — Request/Response

```json
// Request
{ "iban": "DE89370400440532013000" }

// Response
{
  "valid": true,
  "iban": "DE89370400440532013000",
  "bankName": "Commerzbank",
  "bankIdentifier": "37040044",
  "validationMethod": "local"
}
```

## Implementierungsdetails

### Backend — IBAN-Validierung (eigene Implementierung)

1. Trennzeichen (Leerzeichen, Bindestriche, Punkte) entfernen und uppercase.
2. Länge prüfen (DE = 22 Zeichen).
3. Modulo-97-Prüfung: Ersten 4 Zeichen ans Ende verschieben, Buchstaben in Zahlen umwandeln (A=10, B=11, ...), Modulo 97 == 1.
4. BLZ extrahieren (Stellen 5–12 bei DE-IBANs) und gegen bekannte Banken matchen.

### Backend — Externe API (Fallback)

- `GET https://openiban.com/validate/{iban}?getBIC=true&validateBankCode=true`
- Wird aufgerufen wenn lokale Validierung keine Bank findet oder als zusätzliche Verifikation.

### Frontend — Eingabeformat

- Eingabe: Freitext, automatisches Formatting zu 4er-Gruppen (DE89 3704 0044 0532 0130 00).
- Vor API-Aufruf: Alle Nicht-Alphanumerischen Zeichen entfernen.

## Tests

- **Backend**: JUnit 5 + MockMvc für Controller-Tests, Unit-Tests für Validierungslogik.
- **Frontend**: Vitest + React Testing Library für Komponenten-Tests.

## Docker Compose Setup

Drei Services: `postgres` (PostgreSQL 17), `backend` (Java 21), `frontend` (Nginx).
Frontend wird als Static Build via Nginx ausgeliefert und proxied `/api` ans Backend.
Kein separater Reverse-Proxy-Container nötig — die Frontend-Nginx übernimmt beides.

## Coding-Konventionen

- Kein Lombok — plain Java Records für DTOs.
- Spring Data JPA für Persistenz.
- CORS nur in Dev-Profil offen, in Prod via Nginx-Proxy.
- Fehlerbehandlung: `@RestControllerAdvice` mit konsistenten Error-Responses.
- Environment-Variablen für DB-Credentials (nicht hardcoded).
