package de.nicograef.iban.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.nicograef.iban.model.Iban;

/**
 * Spring Data JPA repository — auto-generates CRUD operations for the Iban entity.
 * See lernfragen.md → "Jakarta, JPA und Hibernate" for how this works.
 */
public interface IbanRepository extends JpaRepository<Iban, Long> {
}
