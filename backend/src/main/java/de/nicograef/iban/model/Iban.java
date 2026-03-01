package de.nicograef.iban.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the "ibans" table.
 * Must be a mutable class (not a Record) because Hibernate uses reflection.
 * See lernfragen.md → "JPA und Hibernate" and "Records vs. Klassen".
 */
@Entity
@Table(name = "ibans")
public class Iban {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 34)
    private String iban;

    @Column(length = 255)
    private String bankName;

    @Column(length = 20)
    private String bankIdentifier;

    @Column(nullable = false)
    private boolean valid;

    @Column(nullable = false, length = 20)
    private String validationMethod;

    @Column(nullable = false)
    private Instant createdAt;

    // Required by JPA/Hibernate (reflection-based instantiation).
    protected Iban() {
    }

    public Iban(String iban, String bankName, String bankIdentifier, boolean valid, String validationMethod) {
        this.iban = iban;
        this.bankName = bankName;
        this.bankIdentifier = bankIdentifier;
        this.valid = valid;
        this.validationMethod = validationMethod;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

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

    public String getValidationMethod() {
        return validationMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
