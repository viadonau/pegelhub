package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class AccessGrantRepositoryAdapter implements AccessGrantRepository {

    private final SpringDataAccessGrantRepository accessGrants;
    private final EntityManager entityManager;

    AccessGrantRepositoryAdapter(
            SpringDataAccessGrantRepository accessGrants,
            EntityManager entityManager) {
        this.accessGrants = requireNonNull(accessGrants);
        this.entityManager = requireNonNull(entityManager);
    }

    @Override
    public AccessGrant save(AccessGrant accessGrant) {
        requireNonNull(accessGrant);
        AccessGrantEntity saved = accessGrants.save(AccessGrantEntityMapper.toEntity(accessGrant));
        return AccessGrantEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public AccessGrant saveOrFindByAssignment(AccessGrant accessGrant) {
        requireNonNull(accessGrant);

        entityManager.createNativeQuery("""
                        insert into access_grant (id, connector_id, resource_type, resource_id, permission)
                        values (:id, :connectorId, :resourceType, :resourceId, :permission)
                        on conflict (connector_id, resource_type, resource_id, permission) do nothing
                        """)
                .setParameter("id", accessGrant.id().value())
                .setParameter("connectorId", accessGrant.connectorId().value())
                .setParameter("resourceType", accessGrant.resource().type().name())
                .setParameter("resourceId", accessGrant.resource().id())
                .setParameter("permission", accessGrant.permission().name())
                .executeUpdate();

        return findByAssignment(
                        accessGrant.connectorId(),
                        accessGrant.resource(),
                        accessGrant.permission())
                .orElseThrow(() -> new IllegalStateException("Access grant assignment not found after save attempt"));
    }

    @Override
    public Optional<AccessGrant> findById(AccessGrantId id) {
        requireNonNull(id);
        return accessGrants.findById(id.value()).map(AccessGrantEntityMapper::toDomain);
    }

    @Override
    public Optional<AccessGrant> findByAssignment(
            ConnectorId connectorId,
            AccessResourceRef resource,
            AccessPermission permission) {
        requireNonNull(connectorId);
        requireNonNull(resource);
        requireNonNull(permission);
        return accessGrants.findFirstByConnectorIdAndResourceTypeAndResourceIdAndPermission(
                        connectorId.value(),
                        resource.type(),
                        resource.id(),
                        permission)
                .map(AccessGrantEntityMapper::toDomain);
    }

    @Override
    public List<AccessGrant> findAll() {
        return accessGrants.findAll().stream()
                .map(AccessGrantEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<AccessGrant> findByConnectorId(ConnectorId connectorId) {
        requireNonNull(connectorId);
        return accessGrants.findByConnectorId(connectorId.value()).stream()
                .map(AccessGrantEntityMapper::toDomain)
                .toList();
    }
}
