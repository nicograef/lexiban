# AGENTS.md — Lexiban

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
| [questions.json](quiz/questions.json)   | Single-Choice-Fragenkatalog zu IBAN, Java, Spring Boot, DDD              | **Quiz/Prüfungsvorbereitung** — JSON-Datenquelle für die Quiz-App |
| [future.md](docs/future.md)             | Backlog möglicher Erweiterungen (Auth, HTTPS, CI/CD, Dark Mode etc.)     | Neue Features planen, Erweiterbarkeit diskutieren                 |
| [presentation.md](docs/presentation.md) | Präsentations-Skript für das Vorstellungsgespräch                        | Präsentation vorbereiten oder überarbeiten                        |

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
