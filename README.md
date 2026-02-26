# IBAN Validator

Single-Page-App zur Validierung und Speicherung von IBANs. Coding-Challenge als Bewerbungsprojekt.

## Features

- **IBAN-Eingabe** mit automatischer 4er-Gruppen-Formatierung (Leerzeichen/Trennzeichen erlaubt)
- **Backend-Validierung** via eigener Modulo-97-Prüfziffer-Logik
- **Externe API** als Fallback (openiban.com) für Bankauflösung
- **Banknamen-Auflösung** für 3 bekannte Banken (Deutsche Bank, Commerzbank, Berliner Sparkasse)
- **IBAN-Speicherung** in PostgreSQL
- **Gespeicherte IBANs** im Frontend als Liste anzeigen

## Architektur

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
├── backend/                          # Spring Boot App
│   ├── pom.xml                       # Maven Build + Dependencies (≈ package.json)
│   ├── Dockerfile
│   ├── src/main/java/com/iban/
│   │   ├── IbanApplication.java      # Einstiegspunkt (≈ index.ts)
│   │   ├── config/
│   │   │   ├── CorsConfig.java       # CORS-Konfiguration (nur Dev-Profil)
│   │   │   └── GlobalExceptionHandler.java  # Zentrales Error-Handling (≈ Express error MW)
│   │   ├── controller/
│   │   │   └── IbanController.java   # REST-Endpunkte (≈ Express Router)
│   │   ├── service/
│   │   │   ├── IbanValidationService.java   # Mod-97-Logik + BLZ-Lookup
│   │   │   └── ExternalIbanApiService.java  # openiban.com Fallback-Client
│   │   ├── model/
│   │   │   └── Iban.java             # JPA Entity (≈ Prisma Model)
│   │   └── repository/
│   │       └── IbanRepository.java   # DB-Zugriff (≈ Prisma Client)
│   └── src/test/java/com/iban/
│       ├── controller/IbanControllerTest.java   # MockMvc-Integrationstests
│       └── service/IbanValidationServiceTest.java  # Unit-Tests
├── frontend/                         # React SPA
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf                    # Serves SPA + proxies /api → Backend
│   ├── vite.config.ts
│   ├── src/
│   │   ├── App.tsx                   # Root-Komponente
│   │   ├── components/
│   │   │   └── IbanInput.tsx         # IBAN-Eingabe + Liste
│   │   ├── services/
│   │   │   └── api.ts               # API-Client (fetch)
│   │   └── __tests__/
│   │       └── IbanInput.test.tsx    # Vitest + React Testing Library
│   └── src/components/ui/            # shadcn/ui Primitives (Badge, Button, Card, Input, Label)
├── docker-compose.yml                # 3 Services: postgres, backend, frontend
├── README.md                         # ← Du bist hier
├── AGENTS.md                         # Kontext für Coding-Agents
└── docs/                             # Weitere Dokumentation
    ├── decisions.md                  # Architecture Decision Records
    ├── future.md                    # Backlog möglicher Erweiterungen
    ├── iban.md                      # Fachliches IBAN-Wissen
    ├── lernfragen.md                # Java/Spring-Boot-Lernguide
    ├── presentation.md              # Präsentations-Skript
    └── h2-migration.md              # Anleitung: PostgreSQL → H2 Umstieg
```

## Lokale Entwicklung

```bash
# 1. Datenbank starten
cp .env.example .env
docker compose up postgres

# 2. Backend starten (neues Terminal)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend starten (neues Terminal)
cd frontend && pnpm install && pnpm dev
# Frontend: http://localhost:5173 | API: http://localhost:8080/api
```

## Produktion

```bash
cp .env.example .env
docker compose up --build
# App: http://localhost
```

Drei Docker-Services: `postgres` (PostgreSQL 17), `backend` (Java 21), `frontend` (Nginx).
Frontend wird als Static Build via Nginx ausgeliefert und proxied `/api` ans Backend.
Kein separater Reverse-Proxy-Container — die Frontend-Nginx übernimmt beides.

## Tests

```bash
# Backend (JUnit 5 + MockMvc)
cd backend && mvn verify -B

# Frontend (Vitest + React Testing Library)
cd frontend && pnpm lint && pnpm test
```

## API-Endpunkte

| Methode | Pfad                  | Beschreibung                       |
| ------- | --------------------- | ---------------------------------- |
| POST    | `/api/ibans/validate` | IBAN validieren (ohne Speicherung) |
| POST    | `/api/ibans`          | IBAN validieren und speichern      |
| GET     | `/api/ibans`          | Alle gespeicherten IBANs abrufen   |

### Beispiel: POST `/api/ibans/validate`

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

### Backend — IBAN-Validierung (eigene Modulo-97-Implementierung)

1. Trennzeichen (Leerzeichen, Bindestriche, Punkte) entfernen und uppercase.
2. Länge prüfen (DE = 22 Zeichen).
3. Modulo-97-Prüfung: Ersten 4 Zeichen ans Ende verschieben, Buchstaben in Zahlen umwandeln (A=10, B=11, ...), Modulo 97 == 1.
4. BLZ extrahieren (Stellen 5–12 bei DE-IBANs) und gegen bekannte Banken matchen.

Bekannte Banken: Deutsche Bank (50070010), Commerzbank (50040000), Berliner Sparkasse (10050000).

### Backend — Externe API (Fallback)

- `GET https://openiban.com/validate/{iban}?getBIC=true&validateBankCode=true`
- Wird aufgerufen wenn lokale Validierung keine Bank findet oder als zusätzliche Verifikation.

### Frontend — Eingabeformat

- Eingabe: Freitext, automatisches Formatting zu 4er-Gruppen (DE89 3704 0044 0532 0130 00).
- Vor API-Aufruf: Alle Nicht-Alphanumerischen Zeichen entfernen.

## Coding-Konventionen

- Kein Lombok — plain Java Records für DTOs
- Spring Data JPA für Persistenz, Constructor Injection (kein `@Autowired` auf Feldern)
- Schema-Quelle: Flyway SQL-Migrations (nicht Hibernate `ddl-auto`), Hibernate validiert nur
- CORS nur in Dev-Profil offen, in Prod via Nginx-Proxy
- Fehlerbehandlung: `@RestControllerAdvice` mit konsistenten Error-Responses
- Environment-Variablen für DB-Credentials (nicht hardcoded)
- Frontend: Strict TypeScript, ESLint mit `--max-warnings=0`, Tailwind CSS + shadcn/ui
