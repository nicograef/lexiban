package com.iban.repository;

import com.iban.model.Iban;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Iban entity.
 *
 * ── How does this work with zero code? ──
 * By extending JpaRepository<Iban, Long>, Spring auto-generates a full CRUD
 * implementation at startup. The two type parameters are:
 *   - Iban  — the entity class this repository manages
 *   - Long  — the type of the entity's primary key (@Id field)
 *
 * This single interface gives you all these methods for free:
 *   .save(entity)     — INSERT or UPDATE  ≈ prisma.iban.create() / .update()
 *   .findById(id)     — SELECT by PK     ≈ prisma.iban.findUnique({ where: { id } })
 *   .findAll()        — SELECT *         ≈ prisma.iban.findMany()
 *   .deleteById(id)   — DELETE by PK     ≈ prisma.iban.delete({ where: { id } })
 *   .count()          — SELECT COUNT(*)  ≈ prisma.iban.count()
 *
 * You can also add custom queries just by naming a method:
 *   List<Iban> findByValid(boolean valid);  → generates: WHERE valid = ?
 *   ≈ prisma.iban.findMany({ where: { valid: true } })
 *
 * ── Analogy ──
 * In Prisma:   prisma.iban (auto-generated client)
 * In GORM/Go:  db.Find(&ibans) / db.Create(&iban)
 * In TypeORM:  getRepository(Iban).find()
 *
 * The magic: Spring scans for interfaces extending JpaRepository, creates a
 * proxy class implementing all SQL operations, and registers it as a bean.
 * So when the controller asks for IbanRepository via constructor injection,
 * Spring provides this auto-generated implementation.
 */
public interface IbanRepository extends JpaRepository<Iban, Long> {
}
