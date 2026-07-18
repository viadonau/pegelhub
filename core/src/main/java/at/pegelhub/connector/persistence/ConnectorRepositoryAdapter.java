package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.contact.persistence.ContactEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ConnectorRepositoryAdapter implements ConnectorRepository {

    private final SpringDataConnectorRepository connectors;

    ConnectorRepositoryAdapter(SpringDataConnectorRepository connectors) {
        this.connectors = connectors;
    }

    @Override
    public Connector save(Connector connector) {
        return toDomain(connectors.save(toEntity(connector)));
    }

    @Override
    public Optional<Connector> findById(ConnectorId id) {
        return connectors.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Connector> findAll() {
        return connectors.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(ConnectorId id) {
        connectors.deleteById(id.value());
    }

    @Override
    public Optional<Connector> findByConnectorNumber(String connectorNumber) {
        return connectors.findFirstByConnectorNumber(connectorNumber).map(this::toDomain);
    }

    @Override
    public Optional<Connector> findByKeycloakClientId(String keycloakClientId) {
        return connectors.findFirstByKeycloakClientId(keycloakClientId).map(this::toDomain);
    }

    @Override
    public boolean existsReferencingContact(UUID contactId) {
        return connectors.existsReferencingContact(contactId);
    }

    private ConnectorEntity toEntity(Connector connector) {
        return new ConnectorEntity(
                connector.id().value(),
                connector.connectorNumber(),
                ContactEntityMapper.toEntity(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                ContactEntityMapper.toEntity(connector.softwareManufacturer()),
                ContactEntityMapper.toEntity(connector.technicallyResponsible()),
                ContactEntityMapper.toEntity(connector.operationCompany()),
                connector.notes(),
                connector.keycloakClientId(),
                connector.status());
    }

    private Connector toDomain(ConnectorEntity entity) {
        return new Connector(
                new ConnectorId(entity.getId()),
                entity.getConnectorNumber(),
                ContactEntityMapper.toDomain(entity.getManufacturer()),
                entity.getTypeDescription(),
                entity.getSoftwareVersion(),
                entity.getWorksFromDataVersion(),
                entity.getDataDefinition(),
                ContactEntityMapper.toDomain(entity.getSoftwareManufacturer()),
                ContactEntityMapper.toDomain(entity.getTechnicallyResponsible()),
                ContactEntityMapper.toDomain(entity.getOperatingCompany()),
                entity.getNodes() != null ? entity.getNodes() : "",
                entity.getKeycloakClientId(),
                entity.getStatus());
    }

}
