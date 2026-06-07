package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class AccessGrantRepositoryAdapter implements AccessGrantRepository {

    private final SpringDataAccessGrantRepository accessGrants;

    AccessGrantRepositoryAdapter(SpringDataAccessGrantRepository accessGrants) {
        this.accessGrants = requireNonNull(accessGrants);
    }

    @Override
    public AccessGrant save(AccessGrant accessGrant) {
        requireNonNull(accessGrant);
        return toDomain(accessGrants.save(toEntity(accessGrant)));
    }

    @Override
    public Optional<AccessGrant> findById(AccessGrantId id) {
        requireNonNull(id);
        return accessGrants.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<AccessGrant> findAll() {
        return accessGrants.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AccessGrant> findByConnectorId(ConnectorId connectorId) {
        requireNonNull(connectorId);
        return accessGrants.findByConnectorId(connectorId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private AccessGrantEntity toEntity(AccessGrant accessGrant) {
        return new AccessGrantEntity(
                accessGrant.id().value(),
                accessGrant.connectorId().value(),
                accessGrant.resource().type(),
                accessGrant.resource().id(),
                accessGrant.permission());
    }

    private AccessGrant toDomain(AccessGrantEntity accessGrant) {
        return new AccessGrant(
                new AccessGrantId(accessGrant.id()),
                new ConnectorId(accessGrant.connectorId()),
                new AccessResourceRef(accessGrant.resourceType(), accessGrant.resourceId()),
                accessGrant.permission());
    }
}
