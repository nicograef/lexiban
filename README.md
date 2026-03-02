# IBAN Validator

Single-Page-App zur Validierung und Speicherung von IBANs. Coding-Challenge als Bewerbungsprojekt.

## Features

- **IBAN-Eingabe** mit automatischer 4er-Gruppen-Formatierung (Leerzeichen/Trennzeichen erlaubt)
- **Backend-Validierung** via eigener Modulo-97-Prüfziffer-Logik
- **Externe API** als Fallback (openiban.com) für Bankauflösung
- **Banknamen-Auflösung** für bekannte deutsche Banken (via BLZ-Lookup)
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
├── backend/       # Spring Boot App (Controller → Service → Repository)
├── frontend/      # React SPA (Vite + TypeScript + shadcn/ui)
├── docs/          # Weitere Dokumentation (ADRs, Fachliches, Lernguide)
├── quiz/          # Quiz-App (Single-Choice-Fragen zu IBAN, Java, Spring Boot, DDD)
└── docker-compose.yml
```

Das Backend folgt einer klassischen Schichtenarchitektur: Controller (REST-Endpunkte) → Service (Geschäftslogik) → Repository (DB-Zugriff). Flyway-Migrations verwalten das DB-Schema.

## Lokale Entwicklung

```bash
# 1. Datenbank starten
docker compose up postgres

# 2. Backend starten (neues Terminal, vom Projekt-Root)
#    .env exportieren → Spring liest POSTGRES_USER/PASSWORD daraus (Single Source of Truth)
set -a && source .env && set +a
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend starten (neues Terminal)
cd frontend && pnpm install && pnpm dev
# Frontend: http://localhost:5173 | API: http://localhost:8080/api
```

## Produktion

```bash
docker compose up --build
# App: http://localhost
```

Drei Docker-Services: `postgres` (PostgreSQL 17), `backend` (Java 21), `frontend` (Nginx).
Frontend wird als Static Build via Nginx ausgeliefert und proxied `/api` ans Backend.
Kein separater Reverse-Proxy-Container — die Frontend-Nginx übernimmt beides.

## Tests

```bash
# Backend (JUnit 5 + MockMvc)
cd backend && ./mvnw verify -B

# Frontend (Vitest + React Testing Library)
cd frontend && pnpm lint && pnpm test
```

## API-Endpunkte

| Methode | Pfad                | Beschreibung                                      |
| ------- | ------------------- | ------------------------------------------------- |
| POST    | `/api/ibans`        | IBAN validieren und speichern (oder Cache-Lookup) |
| GET     | `/api/ibans`        | Alle gespeicherten IBANs abrufen                  |
| DELETE  | `/api/ibans/{iban}` | Gespeicherte IBAN löschen (Cache-Invalidierung)   |

### Beispiel: POST `/api/ibans`

```json
// Request
{ "iban": "DE89370400440532013000" }

// Response (erstmalig: Validierung + Speicherung)
{
  "valid": true,
  "iban": "DE89370400440532013000",
  "bankName": "Commerzbank",
  "reason": null
}

// Wiederholte Anfrage: sofortiger Cache-Lookup, keine erneute Validierung
```

## Implementierungsdetails

### IBAN-Validierung (eigene Modulo-97-Implementierung)

1. Trennzeichen (Leerzeichen, Bindestriche, Punkte) entfernen und uppercase.
2. Länge prüfen (DE = 22 Zeichen).
3. Modulo-97-Prüfung: Ersten 4 Zeichen ans Ende verschieben, Buchstaben in Zahlen umwandeln (A=10, B=11, ...), Modulo 97 == 1.
4. BLZ extrahieren (Stellen 5–12 bei DE-IBANs) und gegen bekannte Banken matchen.

### Externe API (Fallback)

Wird über openiban.com aufgerufen, wenn die lokale Validierung keine Bank findet oder als zusätzliche Verifikation.

### Frontend — Eingabeformat

- Eingabe: Freitext, automatisches Formatting zu 4er-Gruppen.
- Vor API-Aufruf: Alle Nicht-Alphanumerischen Zeichen entfernen.

## Coding-Konventionen

- Kein Lombok — plain Java Records für DTOs
- Spring Data JPA für Persistenz, Constructor Injection (kein `@Autowired` auf Feldern)
- Schema-Quelle: Flyway SQL-Migrations (nicht Hibernate `ddl-auto`), Hibernate validiert nur
- CORS nur in Dev-Profil offen, in Prod via Nginx-Proxy
- Fehlerbehandlung: `@RestControllerAdvice` mit konsistenten Error-Responses
- Environment-Variablen für DB-Credentials (nicht hardcoded)
- Frontend: Strict TypeScript, ESLint mit `--max-warnings=0`, Tailwind CSS + shadcn/ui
