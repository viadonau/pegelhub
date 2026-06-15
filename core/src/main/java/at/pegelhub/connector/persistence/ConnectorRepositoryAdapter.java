package at.pegelhub.connector.persistence;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    private ConnectorEntity toEntity(Connector connector) {
        return new ConnectorEntity(
                connector.id().value(),
                connector.connectorNumber(),
                toContactEntity(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                toContactEntity(connector.softwareManufacturer()),
                toContactEntity(connector.technicallyResponsible()),
                toContactEntity(connector.operationCompany()),
                connector.notes(),
                connector.keycloakClientId(),
                connector.status());
    }

    private Connector toDomain(ConnectorEntity entity) {
        return new Connector(
                new ConnectorId(entity.getId()),
                entity.getConnectorNumber(),
                toDomainContact(entity.getManufacturer()),
                entity.getTypeDescription(),
                entity.getSoftwareVersion(),
                entity.getWorksFromDataVersion(),
                entity.getDataDefinition(),
                toDomainContact(entity.getSoftwareManufacturer()),
                toDomainContact(entity.getTechnicallyResponsible()),
                toDomainContact(entity.getOperatingCompany()),
                entity.getNodes() != null ? entity.getNodes() : "",
                entity.getKeycloakClientId(),
                entity.getStatus());
    }

    private ContactEntity toContactEntity(Contact contact) {
        return new ContactEntity(
                contact.getId(), contact.getOrganization(), contact.getContactPerson(),
                contact.getContactStreet(), contact.getContactPlz(), contact.getLocation(),
                contact.getContactCountry(), contact.getEmergencyNumber(), contact.getEmergencyNumberTwo(),
                contact.getEmergencyMail(), contact.getServiceNumber(), contact.getServiceNumberTwo(),
                contact.getServiceMail(), contact.getAdministrationPhoneNumber(),
                contact.getAdministrationPhoneNumberTwo(), contact.getAdministrationMail(),
                contact.getContactNodes());
    }

    private Contact toDomainContact(ContactEntity entity) {
        return new Contact(
                entity.getId(), entity.getOrganization(), entity.getContactPerson(),
                entity.getContactStreet(), entity.getContactPlz(), entity.getLocation(),
                entity.getContactCountry(), entity.getEmergencyNumber(), entity.getEmergencyNumberTwo(),
                entity.getEmergencyMail(), entity.getServiceNumber(), entity.getServiceNumberTwo(),
                entity.getServiceMail(), entity.getAdministrationPhoneNumber(),
                entity.getAdministrationPhoneNumberTwo(), entity.getAdministrationMail(),
                entity.getContactNodes());
    }
}
