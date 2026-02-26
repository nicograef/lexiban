# Mögliche Erweiterungen

Features, die aus einem bestehenden Projekt bekannt sind und später in dieses Projekt übernommen werden könnten.

## Authentifizierung & Rollen

**Quelle:** Bereits erprobt mit JWT-basierter Auth (HS256, 12h Gültigkeit), Argon2id-Passwort-Hashing und drei Rollen (admin, senior_service, service).

**Anwendung für IBAN-Projekt:**

- Login-System für Benutzer, die IBANs validieren und speichern
- Admin-Bereich zur Verwaltung gespeicherter IBANs
- Rate-Limiting pro authentifiziertem Benutzer statt nur per IP

**Aufwand:** Mittel — Spring Security + JWT-Filter, Benutzer-Tabelle, Login-Seite

## HTTPS / Let's Encrypt

**Quelle:** Bereits erprobt mit vollständigem TLS-Setup (nginx, Certbot, automatische Zertifikatserneuerung alle 24h). Separate Compose-Datei für initiales Zertifikat (`docker-compose.initial-cert.yml`).

**Anwendung für IBAN-Projekt:**

- Produktions-Deployment mit HTTPS
- Security-Headers (HSTS, CSP enforced, X-Frame-Options)
- HTTP→HTTPS Redirect

**Aufwand:** Niedrig — nginx.conf anpassen, Certbot-Service in docker-compose.yml ergänzen

## Staging-Umgebung

**Quelle:** Bereits erprobt mit `docker-compose.staging.yml` und separater nginx-Konfiguration (HTTP, gebaute Images statt Dev-Server). Nützlich zum Testen des Production-Builds ohne TLS.

**Anwendung für IBAN-Projekt:**

- Staging-Compose-Datei mit gebauten Docker-Images
- Eigene nginx-Konfiguration ohne TLS

**Aufwand:** Niedrig — Compose-Datei aus bestehendem Projekt übernehmen und anpassen

## CI/CD mit Path-Filtering

**Quelle:** Bereits erprobt mit `dorny/paths-filter` in GitHub Actions, um Backend-CI nur bei Backend-Änderungen und Frontend-CI nur bei Frontend-Änderungen auszuführen. Spart CI-Minuten.

**Anwendung für IBAN-Projekt:**

- Aktuell laufen beide Jobs immer — bei wachsendem Projekt sinnvoll umzustellen

**Aufwand:** Niedrig — `changes`-Job aus bestehendem CI-Workflow übernehmen

## Integrationstests mit Test-Datenbank

**Quelle:** Bereits erprobt mit `test-integration.sh`-Skript, das eine PostgreSQL-Instanz in Docker startet, Migrationen ausführt, Tests laufen lässt und alles aufräumt. In CI als eigener Job mit PostgreSQL-Service-Container.

**Anwendung für IBAN-Projekt:**

- Spring Boot Integrationstests mit Testcontainers oder H2
- CI-Job, der den Backend gegen eine echte PostgreSQL testet

**Aufwand:** Mittel — Testcontainers-Dependency + Integrationstests schreiben

## Dark Mode

**Quelle:** Bereits erprobt mit `next-themes`, `.dark`-CSS-Klasse und vollständigem Dark-Mode-Farbschema in `index.css`. Toggle über ThemeProvider.

**Anwendung für IBAN-Projekt:**

- Dark-Mode-Farbvariablen in `index.css` ergänzen (Vorlage aus bestehendem Projekt vorhanden)
- ThemeProvider + Toggle-Button

**Aufwand:** Niedrig — CSS-Variablen + `next-themes` Package

## Toast-Benachrichtigungen

**Quelle:** Bereits erprobt mit Sonner für alle mutativen Aktionen (Erfolg/Fehler-Feedback).

**Anwendung für IBAN-Projekt:**

- Erfolgsmeldung nach IBAN-Speicherung
- Fehleranzeige bei API-Problemen als Toast statt inline

**Aufwand:** Niedrig — `sonner` Package + Toaster-Komponente

## Produktions-Deployment Automatisierung

**Quelle:** Bereits erprobt mit `.session_aliases` und `prod-update='git pull && sudo docker compose down && sudo docker compose up --build -d'` für schnelle Deployments.

**Anwendung für IBAN-Projekt:**

- Deployment-Alias für Production-Server
- GitHub Actions CD-Pipeline (auto-deploy auf Push zu main)

**Aufwand:** Niedrig bis Mittel

## Datenexport

**Quelle:** CSV/Excel-Export für Reporting-Daten ist eine bewährte Erweiterung.

**Anwendung für IBAN-Projekt:**

- Export gespeicherter IBANs als CSV
- Download-Button in der IBAN-Liste

**Aufwand:** Niedrig — Backend-Endpoint mit `text/csv` Response, Frontend-Download-Link
