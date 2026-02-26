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
backend/                  # Spring Boot App (Controller, Service, Model, Repository)
frontend/                 # React SPA (Vite, Tailwind CSS, TypeScript)
docker-compose.yml        # Produktion (postgres + backend + frontend)
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

## Tests

```bash
# Backend
cd backend && mvn verify -B

# Frontend
cd frontend && pnpm lint && pnpm test
```

## API-Endpunkte

| Methode | Pfad                  | Beschreibung                       |
| ------- | --------------------- | ---------------------------------- |
| POST    | `/api/ibans/validate` | IBAN validieren (ohne Speicherung) |
| POST    | `/api/ibans`          | IBAN validieren und speichern      |
| GET     | `/api/ibans`          | Alle gespeicherten IBANs abrufen   |
