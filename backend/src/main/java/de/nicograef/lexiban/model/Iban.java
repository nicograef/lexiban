package de.nicograef.lexiban.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity — maps to the "ibans" table. IBAN string as natural primary key.
 *
 * <p>Each IBAN exists exactly once (cache). Mutable class required by Hibernate (reflection +
 * no-arg constructor).
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
    protected Iban() {}

    public Iban(String iban, String bankName, boolean valid, String reason) {
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
