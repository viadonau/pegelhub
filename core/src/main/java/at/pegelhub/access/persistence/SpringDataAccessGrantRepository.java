package at.pegelhub.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataAccessGrantRepository extends JpaRepository<AccessGrantEntity, UUID> {

    List<AccessGrantEntity> findByConnectorId(UUID connectorId);
}
