-- ── Flyway migration V1: Initial schema for Lexiban ──
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