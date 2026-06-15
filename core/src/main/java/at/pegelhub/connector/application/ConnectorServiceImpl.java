package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
class ConnectorServiceImpl implements ConnectorService {

    private final ConnectorRepository connectorRepository;
    private final ContactRepository contactRepository;

    ConnectorServiceImpl(ConnectorRepository connectorRepository, ContactRepository contactRepository) {
        this.connectorRepository = requireNonNull(connectorRepository);
        this.contactRepository = requireNonNull(contactRepository);
    }

    @Override
    @Transactional
    public Connector create(CreateConnectorCommand command) {
        requireNonNull(command);
        Connector connector = connectorFromCommand(command);
        return connectorRepository.save(persistContacts(connector));
    }

    @Override
    @Transactional
    public Connector register(String keycloakClientId, ConnectorStatus status, CreateConnectorCommand command) {
        requireNonNull(keycloakClientId);
        requireNonNull(status);
        requireNonNull(command);
        if (keycloakClientId.isBlank()) {
            throw new IllegalArgumentException("keycloakClientId must not be blank");
        }
        connectorRepository.findByKeycloakClientId(keycloakClientId).ifPresent(existing -> {
            throw new IllegalArgumentException("Connector already exists for Keycloak client id " + keycloakClientId);
        });
        Connector connector = connectorFromCommand(command);
        return connectorRepository.save(
                persistContacts(connector).withExternalAuth(keycloakClientId, status));
    }

    @Override
    public Connector get(ConnectorId id) {
        requireNonNull(id);
        return connectorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Connector not found: " + id.value()));
    }

    @Override
    public List<Connector> list() {
        return connectorRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(ConnectorId id) {
        requireNonNull(id);
        connectorRepository.delete(id);
    }

    private Connector connectorFromCommand(CreateConnectorCommand command) {
        return Connector.create(
                command.connectorNumber(),
                contactFromDto(command.manufacturer()),
                command.typeDescription(),
                command.softwareVersion(),
                command.worksFromDataVersion(),
                command.dataDefinition(),
                contactFromDto(command.softwareManufacturer()),
                contactFromDto(command.technicallyResponsible()),
                contactFromDto(command.operationCompany()),
                command.notes());
    }

    private Contact contactFromDto(at.pegelhub.contact.api.CreateContactDto dto) {
        return new Contact(null,
                dto.organization(), dto.contactPerson(), dto.contactStreet(), dto.contactPlz(),
                dto.location(), dto.contactCountry(), dto.emergencyNumber(), dto.emergencyNumberTwo(),
                dto.emergencyMail(), dto.serviceNumber(), dto.serviceNumberTwo(), dto.serviceMail(),
                dto.administrationPhoneNumber(), dto.administrationPhoneNumberTwo(), dto.administrationMail(),
                dto.contactNodes());
    }

    private Connector persistContacts(Connector connector) {
        return new Connector(
                connector.id(),
                connector.connectorNumber(),
                contactRepository.saveContact(connector.manufacturer()),
                connector.typeDescription(),
                connector.softwareVersion(),
                connector.worksFromDataVersion(),
                connector.dataDefinition(),
                contactRepository.saveContact(connector.softwareManufacturer()),
                contactRepository.saveContact(connector.technicallyResponsible()),
                contactRepository.saveContact(connector.operationCompany()),
                connector.notes(),
                connector.keycloakClientId(),
                connector.status());
    }
}
