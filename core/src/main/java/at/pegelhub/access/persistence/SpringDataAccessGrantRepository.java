package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAccessGrantRepository extends JpaRepository<AccessGrantEntity, UUID> {

    List<AccessGrantEntity> findByConnectorId(UUID connectorId);

    Optional<AccessGrantEntity> findFirstByConnectorIdAndResourceTypeAndResourceIdAndPermission(
            UUID connectorId,
            AccessResourceType resourceType,
            UUID resourceId,
            AccessPermission permission);
}
