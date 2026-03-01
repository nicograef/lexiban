package de.nicograef.iban.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.nicograef.iban.model.Iban;

/**
 * Spring Data JPA repository — auto-generates CRUD operations for the Iban
 * entity.
 *
 * Generic type changed from Long to String because the IBAN itself is now the
 * primary key (natural key). All standard methods (findById, existsById, save,
 * findAll, deleteById) work automatically with the String PK.
 *
 * TS analogy: like changing a Prisma model from `id
 * Int @id @default(autoincrement())`
 * to `iban String @id`.
 */
public interface IbanRepository extends JpaRepository<Iban, String> {
}
