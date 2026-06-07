package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.shared.error.NotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class ConnectorServiceImplTest {

    private ConnectorServiceImpl connectorService;
    private static final ConnectorRepository REPOSITORY = mock(ConnectorRepository.class);
    private static final ContactRepository CONTACT_REPOSITORY = mock(ContactRepository.class);

    @BeforeEach
    public void prepare() {
        connectorService = new ConnectorServiceImpl(REPOSITORY, CONTACT_REPOSITORY);
        reset(REPOSITORY);
        reset(CONTACT_REPOSITORY);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new ConnectorServiceImpl(null, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new ConnectorServiceImpl(REPOSITORY, null));
    }

    @Test
    public void createConnector() {
        CreateConnectorCommand command = connectorCommand();
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> savedContact(invocation.getArgument(0)));
        when(REPOSITORY.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Connector result = connectorService.create(command);

        assertEquals(CONNECTOR_NUMBER, result.connectorNumber());
        assertContactIdsWerePersisted(result);
        verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
        verify(REPOSITORY).save(any());
        InOrder inOrder = inOrder(CONTACT_REPOSITORY, REPOSITORY);
        inOrder.verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
        inOrder.verify(REPOSITORY).save(any());
    }

    @Test
    public void registerConnectorPersistsContactsBeforeSavingConnectorAuth() {
        CreateConnectorCommand command = connectorCommand();
        String keycloakClientId = "local-connector-example";
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> savedContact(invocation.getArgument(0)));
        when(REPOSITORY.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Connector result = connectorService.register(keycloakClientId, ConnectorStatus.ACTIVE, command);

        assertEquals(keycloakClientId, result.keycloakClientId());
        assertEquals(ConnectorStatus.ACTIVE, result.status());
        assertContactIdsWerePersisted(result);
        verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
    }

    @Test
    public void registerConnectorRejectsDuplicateKeycloakClientIdBeforeSavingContacts() {
        when(REPOSITORY.findByKeycloakClientId("existing-client")).thenReturn(Optional.of(CONNECTOR));

        assertThrows(IllegalArgumentException.class,
                () -> connectorService.register("existing-client", ConnectorStatus.ACTIVE, connectorCommand()));

        verify(CONTACT_REPOSITORY, never()).saveContact(any());
        verify(REPOSITORY, never()).save(any());
    }

    @Test
    public void getById() {
        ConnectorId id = new ConnectorId(UUID.randomUUID());
        when(REPOSITORY.findById(id)).thenReturn(Optional.of(CONNECTOR));

        Connector result = connectorService.get(id);
        assertEquals(CONNECTOR, result);
        verify(REPOSITORY, times(1)).findById(id);
    }

    @Test
    public void getByIdThrowsNotFoundWhenConnectorIsMissing() {
        ConnectorId id = new ConnectorId(UUID.randomUUID());
        when(REPOSITORY.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> connectorService.get(id));
        verify(REPOSITORY, times(1)).findById(id);
    }

    @Test
    public void listAll() {
        when(REPOSITORY.findAll()).thenReturn(List.of(CONNECTOR));

        List<Connector> result = connectorService.list();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(CONNECTOR);
        verify(REPOSITORY, times(1)).findAll();
    }

    private static CreateConnectorCommand connectorCommand() {
        return new CreateConnectorCommand(
                CONNECTOR_NUMBER,
                contactDto(), DESCRIPTION, VERSION, VERSION, DATA_DEFINITION,
                contactDto(), contactDto(), contactDto(), NOTES);
    }

    private static CreateContactDto contactDto() {
        return new CreateContactDto(
                ORGANIZATION, CONTACT_PERSON, CONTACT_STREET, CONTACT_PLZ, LOCATION,
                CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO, EMERGENCY_MAIL,
                SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL,
                ADMIN_NUMBER, ADMIN_NUMBER_TWO, ADMIN_MAIL, NOTES);
    }

    private static Contact savedContact(Contact contact) {
        return contact.withId(UUID.randomUUID());
    }

    private static void assertContactIdsWerePersisted(Connector connector) {
        Assertions.assertThat(connector.manufacturer().getId()).isNotNull();
        Assertions.assertThat(connector.softwareManufacturer().getId()).isNotNull();
        Assertions.assertThat(connector.technicallyResponsible().getId()).isNotNull();
        Assertions.assertThat(connector.operationCompany().getId()).isNotNull();
    }
}
