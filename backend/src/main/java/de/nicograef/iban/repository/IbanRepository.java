package de.nicograef.iban.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.nicograef.iban.model.Iban;

/**
 * Spring Data JPA repository — auto-generates CRUD operations for the Iban
 * entity.
 */
public interface IbanRepository extends JpaRepository<Iban, String> {
}
