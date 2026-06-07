package at.pegelhub.contact.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JPA repository for {@code Contact}s.
 */

@Repository
public interface SpringDataContactRepository extends JpaRepository<ContactEntity, UUID> {
}
