package de.nicograef.iban.repository;

import de.nicograef.iban.model.Iban;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository — auto-generates CRUD operations for the Iban entity. */
public interface IbanRepository extends JpaRepository<Iban, String> {

    /** Returns all IBANs ordered by creation date (newest first). */
    List<Iban> findAllByOrderByCreatedAtDesc();
}
