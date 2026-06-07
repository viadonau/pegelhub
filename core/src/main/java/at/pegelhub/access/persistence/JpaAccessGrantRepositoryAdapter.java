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
final class JpaAccessGrantRepositoryAdapter implements AccessGrantRepository {

    private final SpringDataAccessGrantRepository accessGrants;

    JpaAccessGrantRepositoryAdapter(SpringDataAccessGrantRepository accessGrants) {
        this.accessGrants = requireNonNull(accessGrants);
    }

    @Override
    public AccessGrant save(AccessGrant accessGrant) {
        requireNonNull(accessGrant);
        return toDomain(accessGrants.save(toJpa(accessGrant)));
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

    private JpaAccessGrant toJpa(AccessGrant accessGrant) {
        return new JpaAccessGrant(
                accessGrant.id().value(),
                accessGrant.connectorId().value(),
                accessGrant.resource().type(),
                accessGrant.resource().id(),
                accessGrant.permission(),
                accessGrant.validFrom(),
                accessGrant.validUntil(),
                accessGrant.includeFutureTimeSeries());
    }

    private AccessGrant toDomain(JpaAccessGrant accessGrant) {
        return new AccessGrant(
                new AccessGrantId(accessGrant.id()),
                new ConnectorId(accessGrant.connectorId()),
                new AccessResourceRef(accessGrant.resourceType(), accessGrant.resourceId()),
                accessGrant.permission(),
                accessGrant.validFrom(),
                accessGrant.validUntil(),
                accessGrant.includeFutureTimeSeries());
    }
}
