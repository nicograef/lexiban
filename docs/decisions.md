# Architecture Decisions

Simplifications and trade-offs documented for this project.

---

## 1. Reverse-proxy only in production

**Dev** (`docker-compose.yml`): Frontend nginx serves static files _and_ proxies `/api/*` to the backend — no separate reverse-proxy container needed (3 services total).

**Prod** (`docker-compose.prod.yml`): A dedicated reverse-proxy container handles TLS termination via Let's Encrypt + Certbot, routing `/api/*` to the backend and `/*` to the frontend (5 services total).

## 2. No separate dev Docker Compose file

Dev workflow: `docker compose up postgres`, then run backend/frontend locally. Running Maven/Node inside containers with volume mounts adds complexity without benefit for a solo developer (slower file watching, harder debugging).

## 3. Network isolation only in production

**Dev** (`docker-compose.yml`): All services share the default Compose network — no explicit networks defined.

**Prod** (`docker-compose.prod.yml`): Two networks — `app-network` (reverse-proxy ↔ frontend ↔ backend) and `db-network` (backend ↔ postgres, `internal: true` — not reachable from outside).

## 4. PostgreSQL over SQLite / H2

SQLite has poor Hibernate/Flyway support. H2 would work but removes learning value (Docker orchestration, Flyway migrations, env-var config). PostgreSQL matches a real production setup with minimal overhead.

## 5. Controller → Service → Repository

Standard Spring Boot layered architecture. With 1 entity and 2 endpoints it feels heavy, but it's the expected pattern, makes testing trivial (`@WebMvcTest` with mocked services), and each layer is just 1 file.

## 6. Flyway migrations

Schema-first via Flyway SQL files, not code-first via `ddl-auto=update`. Versioned, reproducible migrations. `ddl-auto=validate` ensures JPA matches the DB without modifying it.

## 7. shadcn/ui components

5 components (Badge, Button, Card, Input, Label) copied as source code — not a runtime dependency. They provide accessible, Tailwind-based UI primitives. Removing them would mean reimplementing the same styling inline.

## 8. DRY up IbanController _(superseded by ADR 15)_

Originally extracted shared logic into a `buildResponse()` method in the controller. ADR 15 moved all orchestration into `IbanService.validateOrLookup()`, making the controller thin enough that no shared helper is needed.

## 9. Jakarta Validation (`@Valid` / `@NotBlank`)

Idiomatic Spring Boot request validation via annotations. Handled by the framework before the controller method runs. Error responses generated automatically by `GlobalExceptionHandler`.

## 10. Strict TypeScript + ESLint

`strict: true`, `noUncheckedIndexedAccess`, `strictTypeChecked`, `--max-warnings=0`. Catches real bugs at compile time — the bare minimum for a professional TypeScript project.

## 11. Java 21 (LTS) over Java 25

Java 21 is the current industry-standard LTS. All frameworks/libraries are fully tested against it. Java 25 (September 2025) is only 6 months old — teams upgrade LTS-to-LTS after 6–12 months of GA. This project uses no Java 25 features.

## 12. Maven over Gradle

Declarative XML (`pom.xml`) is readable for Java newcomers and conceptually close to `package.json`. Spring Initializr defaults to Maven. This project has a simple build — Gradle's strengths (build scripts as code, incremental compile, build cache) are overkill.

## 13. Maven 3.9.x over 4.0.0-rc

Maven 4.0.0 is still a Release Candidate — _"not safe for production use"_. Maven 3.9.12 is the recommended stable version. This project uses no Maven-4-specific features.

## 14. Spring Boot 3.5.11 (not 4.0.3)

Upgraded from 3.4.3 (EOL December 2025) to 3.5.11 (OSS support until June 2026). Minor version bump — no code changes, same Jakarta EE 10 / Spring Framework 6.x / Jackson 2.

Spring Boot 4.0.x was not chosen: major breaking changes (Spring Framework 7, Jackson 3, renamed starters, new module structure). Professional teams adopt new majors 6–12 months after GA. Reconsider when 3.5.x approaches EOL or learning goals shift to migration.

## 15. Natürlicher Primary Key + Rich Domain Model (DDD)

Replaced surrogate `BIGINT id` with the IBAN string as natural PK — a globally unique identifier needs no synthetic key. Each IBAN exists once (lookup-cache semantics).

Introduced `IbanNumber` Value Object (self-normalizing, structurally validated), extracted `Mod97Validator` as dedicated service, moved orchestration logic from Controller to Service. Removed `validationMethod` field (internal detail, not a business concern).

Trade-off: no `Persistable<String>` — simpler code, marginal performance cost (extra SELECT on save, mitigated by existing `findById()` check).
