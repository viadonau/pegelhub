package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
class ConnectorRepositoryAdapter implements ConnectorRepository {

    private final SpringDataConnectorRepository connectors;

    ConnectorRepositoryAdapter(SpringDataConnectorRepository connectors) {
        this.connectors = requireNonNull(connectors);
    }

    @Override
    public Connector save(Connector connector) {
        requireNonNull(connector);
        return toDomain(connectors.save(toEntity(connector)));
    }

    @Override
    public Optional<Connector> findById(ConnectorId id) {
        requireNonNull(id);
        return connectors.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Connector> findAll() {
        return connectors.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Connector> findByKeycloakClientId(String keycloakClientId) {
        return connectors.findFirstByKeycloakClientId(keycloakClientId).map(this::toDomain);
    }

    private ConnectorEntity toEntity(Connector connector) {
        return new ConnectorEntity(
                connector.id().value(),
                connector.name(),
                connector.type().value(),
                connector.keycloakClientId(),
                connector.status().value());
    }

    private Connector toDomain(ConnectorEntity entity) {
        return new Connector(
                new ConnectorId(entity.id()),
                entity.name(),
                ConnectorType.from(entity.type()),
                entity.keycloakClientId(),
                MetadataStatus.from(entity.status()));
    }
}
