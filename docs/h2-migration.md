# How to switch from PostgreSQL to H2

> Ausgelagert aus [decisions.md](decisions.md) — dort steht die Entscheidung, warum PostgreSQL beibehalten wurde (Decision #4).

H2 is an embedded, in-memory (or file-based) Java database — think of it as the JVM equivalent of SQLite. It runs inside the Spring Boot process, requires no Docker container, and needs zero configuration. This document describes every change needed to replace PostgreSQL + Flyway with H2.

## Trade-offs

- **You gain**: No Docker dependency for development or production. Faster startup. Simpler `docker-compose.yml` (only 2 services). Zero database credentials.
- **You lose**: Flyway-based migration workflow. A "real" database that matches production setups. Docker Compose service orchestration learning (`depends_on`, healthchecks). Environment-variable-based DB config.

**In production**: Professional teams use H2 exclusively for tests (`@DataJpaTest`), never in production. Real applications use PostgreSQL, MySQL, or managed databases (RDS, Cloud SQL). This switch is fine for a demo/coding-challenge, but be prepared to explain the difference in an interview.

## Step-by-step migration

### 1. Replace dependencies in `pom.xml`

Remove the PostgreSQL driver and both Flyway dependencies. Add the H2 driver instead:

```xml
<!-- REMOVE these 3 dependencies: -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- ADD this dependency: -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

> **Why `scope=runtime`?** Same as PostgreSQL — the JDBC driver is only needed when the app runs, not when compiling. Your Java code talks to JPA/Hibernate interfaces, never directly to H2 classes. ≈ In Node.js terms: it's like `pg` being a runtime peer dependency of Prisma.

### 2. Replace `application.properties` database config

Replace the datasource and Flyway sections:

```properties
# ── Database connection (H2 embedded) ──
# file: prefix stores data in a file. mem: would be in-memory only (lost on restart).
# DB_CLOSE_DELAY=-1 keeps the DB alive as long as the JVM runs.
# ≈ Using SQLite with a file path in Node.js
spring.datasource.url=jdbc:h2:file:./data/iban-db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ── JPA / Hibernate ──
# Switch from "validate" to "update" because there are no Flyway migrations.
# Hibernate will auto-create/alter tables based on your @Entity classes.
# ≈ prisma db push (sync schema from code, no migration history)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Disable Flyway (no longer needed)
spring.flyway.enabled=false

# Optional: enable H2's web console for debugging at http://localhost:8080/h2-console
# spring.h2.console.enabled=true
# spring.h2.console.path=/h2-console
```

> **`ddl-auto=update` vs `validate`**: With PostgreSQL + Flyway, we used `validate` — Hibernate only checked that the entity matched the DB schema (Flyway created it). With H2, we switch to `update` — Hibernate auto-creates tables from `@Entity` annotations. This is fine for a demo but dangerous in production (it can silently drop columns). ≈ The difference between `prisma migrate deploy` (safe, versioned) and `prisma db push` (convenient, destructive).

### 3. Delete the Flyway migration files

```bash
rm -rf backend/src/main/resources/db/migration/
```

The schema is now derived from JPA annotations in `Iban.java` rather than from SQL files.

### 4. Simplify `docker-compose.yml`

Remove the `postgres` service and the `postgres-data` volume. Remove `depends_on` and DB environment variables from the backend:

```yaml
name: lexiban

services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: lexiban-backend
    restart: unless-stopped

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: lexiban-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    depends_on:
      backend:
        condition: service_started
```

### 5. Add `data/` to `.gitignore`

H2 stores data in `backend/data/iban-db.mv.db`. This file should not be committed:

```gitignore
# H2 database files
backend/data/
```

### 6. Update `application-dev.properties`

Remove the CORS-only dev profile config if it references DB settings. The current one only has CORS config, so no change needed.

### 7. Verify

```bash
cd backend && mvn clean test    # all 16 tests should still pass
cd backend && mvn spring-boot:run   # starts without Docker, creates data/iban-db.mv.db
```

## What changes conceptually

| Aspect                     | PostgreSQL (current)                                 | H2 (after switch)                                         |
| -------------------------- | ---------------------------------------------------- | --------------------------------------------------------- |
| **Schema source of truth** | `db/migration/V1__*.sql` (Flyway)                    | `Iban.java` JPA annotations (Hibernate `ddl-auto=update`) |
| **Docker services**        | 3 (postgres, backend, frontend)                      | 2 (backend, frontend)                                     |
| **Dev workflow**           | `docker compose up postgres` + `mvn spring-boot:run` | Just `mvn spring-boot:run`                                |
| **Data persistence**       | Docker volume (`postgres-data`)                      | Local file (`data/iban-db.mv.db`)                         |
| **Credentials**            | Environment variables (`SPRING_DATASOURCE_*`)        | Hardcoded defaults (`sa` / empty)                         |
| **Production-readiness**   | Production-grade                                     | Demo only                                                 |
