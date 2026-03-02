package de.nicograef.iban.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the "ibans" table.
 * The IBAN string itself is the natural primary key (no surrogate ID).
 *
 * Each IBAN exists exactly once in the database. Repeated validation requests
 * for the same IBAN return the cached result (lookup by PK).
 *
 * Note on Persistable<String>: With a String PK, JPA cannot auto-detect
 * whether an entity is "new" (INSERT) vs "existing" (UPDATE). Implementing
 * Persistable<String> with a transient isNew flag would let save() call
 * persist() directly (one INSERT) instead of merge() (SELECT + INSERT).
 * This would be slightly more performant, but we intentionally chose
 * simplicity and readability over that optimization. The service layer
 * already checks findById() before saving, so the extra SELECT from
 * merge() only hits an empty result — functionally identical.
 *
 * Must be a mutable class (not a Record) because Hibernate uses reflection
 * + no-arg constructor for instantiation.
 */
@Entity
@Table(name = "ibans")
public class Iban {

    @Id
    @Column(nullable = false, length = 34)
    private String iban;

    @Column(length = 255)
    private String bankName;

    @Column(nullable = false)
    private boolean valid;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    // Required by JPA/Hibernate (reflection-based instantiation).
    protected Iban() {
    }

    public Iban(String iban, String bankName,
            boolean valid, String reason) {
        this.iban = iban;
        this.bankName = bankName;
        this.valid = valid;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public String getIban() {
        return iban;
    }

    public String getBankName() {
        return bankName;
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
