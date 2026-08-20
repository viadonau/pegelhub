package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.error.MetadataConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class ConnectorServiceImpl implements ConnectorService {

    private final ConnectorRepository connectors;

    ConnectorServiceImpl(ConnectorRepository connectors) {
        this.connectors = requireNonNull(connectors);
    }

    @Override
    public Connector create(CreateConnectorCommand command) {
        requireNonNull(command);
        return connectors.save(Connector.create(command.name(), command.type()));
    }

    @Override
    @Transactional
    public Connector register(String keycloakClientId, at.pegelhub.shared.metadata.MetadataStatus status,
                              CreateConnectorCommand command) {
        requireNonNull(keycloakClientId);
        requireNonNull(status);
        requireNonNull(command);
        if (keycloakClientId.isBlank()) {
            throw new IllegalArgumentException("keycloakClientId must not be blank");
        }
        connectors.findByKeycloakClientId(keycloakClientId).ifPresent(existing -> {
            throw new MetadataConflictException("Connector already exists for Keycloak client id " + keycloakClientId);
        });
        return connectors.save(Connector.create(command.name(), command.type()).bind(keycloakClientId, status));
    }

    @Override
    @Transactional
    public Connector update(ConnectorId id, UpdateConnectorCommand command) {
        requireNonNull(command);
        return connectors.save(get(id).update(command.name(), command.type(), command.status()));
    }

    @Override
    public Connector get(ConnectorId id) {
        requireNonNull(id);
        return connectors.findById(id)
                .orElseThrow(() -> new NotFoundException("Connector not found: " + id.value()));
    }

    @Override
    public List<Connector> list() {
        return connectors.findAll();
    }
}
