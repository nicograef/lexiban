package de.nicograef.iban.model;

import java.time.Instant;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * JPA Entity — maps to the "ibans" table.
 * The IBAN string itself is the natural primary key (no surrogate ID).
 *
 * Each IBAN exists exactly once in the database. Repeated validation requests
 * for the same IBAN return the cached result (lookup by PK).
 *
 * Implements Persistable<String> because JPA cannot auto-detect whether a
 * String-PK entity is "new" (INSERT) or "existing" (UPDATE). With
 * auto-generated
 * Long IDs, JPA checks id == null → new. With String PKs, we must tell JPA
 * explicitly via isNew().
 * TS analogy: like implementing a custom "isNewRecord" flag for an ORM.
 *
 * Must be a mutable class (not a Record) because Hibernate uses reflection
 * + no-arg constructor for instantiation.
 */
@Entity
@Table(name = "ibans")
public class Iban implements Persistable<String> {

    @Id
    @Column(nullable = false, length = 34)
    private String iban;

    @Column(length = 255)
    private String bankName;

    @Column(length = 20)
    private String bankIdentifier;

    @Column(nullable = false)
    private boolean valid;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Transient flag to track whether this entity is new (not yet persisted).
     * JPA uses this via Persistable.isNew() to decide between persist (INSERT)
     * and merge (UPDATE). After Hibernate loads an entity from DB, this is false.
     * After creating via constructor, this is true.
     */
    @Transient
    private boolean isNew = false;

    // Required by JPA/Hibernate (reflection-based instantiation).
    // When Hibernate loads from DB, isNew stays false → merge behavior.
    protected Iban() {
    }

    public Iban(String iban, String bankName, String bankIdentifier,
            boolean valid, String reason) {
        this.iban = iban;
        this.bankName = bankName;
        this.bankIdentifier = bankIdentifier;
        this.valid = valid;
        this.reason = reason;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isNew = true; // New entity → JPA should INSERT, not UPDATE
    }

    // ── Persistable implementation ──

    @Override
    public String getId() {
        return iban;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }

    // ── Getters ──

    public String getIban() {
        return iban;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankIdentifier() {
        return bankIdentifier;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
