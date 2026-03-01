-- ── Flyway migration V1: Initial schema for IBAN validator ──
--
-- Flyway is a database migration tool (≈ Prisma Migrate or golang-migrate).
-- It runs SQL files in version order (V1__, V2__, ...) on application startup.
--
-- How it works:
-- 1. Spring Boot starts → Flyway checks the "flyway_schema_history" table
-- 2. If V1 hasn't been applied yet, Flyway runs this script
-- 3. Flyway records V1 as applied (won't run again on next startup)
--
-- Naming convention: V{number}__{description}.sql
-- The double underscore (__) separates the version from the description.
-- Files live in: src/main/resources/db/migration/
--
-- This is the ONLY place where the DB schema is defined.
-- The JPA entity (Iban.java) must match this schema exactly because
-- we set spring.jpa.hibernate.ddl-auto=validate (Hibernate validates
-- but never modifies the schema).
--
-- Design: The IBAN itself is the natural primary key. Each IBAN exists
-- exactly once — the table acts as a lookup cache for validation results.
-- Repeated requests for the same IBAN return the cached result instantly.
-- See docs/iban-as-entity-refactoring.md for the full rationale.
CREATE TABLE
    IF NOT EXISTS ibans (
        iban VARCHAR(34) PRIMARY KEY,
        bank_name VARCHAR(255),
        valid BOOLEAN NOT NULL,
        reason VARCHAR(255),
        created_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );

-- No separate index needed — the PRIMARY KEY creates a B-tree index on `iban`.
-- Column documentation (visible in psql with \d+ ibans)
COMMENT ON TABLE ibans IS 'Unique IBANs with cached validation results (one row per IBAN)';

COMMENT ON COLUMN ibans.iban IS 'Normalized IBAN string (uppercase, no separators) — natural primary key';

COMMENT ON COLUMN ibans.bank_name IS 'Resolved bank name (if known)';

COMMENT ON COLUMN ibans.valid IS 'Whether the IBAN passed Modulo-97 validation';

COMMENT ON COLUMN ibans.reason IS 'Human-readable reason when IBAN is invalid (null when valid)';

COMMENT ON COLUMN ibans.created_at IS 'Timestamp when the IBAN was first validated';