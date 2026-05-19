package at.pegelhub.connector.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
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
        Connector connector = connectorWithTransientContacts();
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> savedContact(invocation.getArgument(0)));
        when(REPOSITORY.saveConnector(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Connector result = connectorService.createConnector(connector);

        assertEquals(CONNECTOR_NUMBER, result.getConnectorNumber());
        assertContactIdsWerePersisted(result);
        verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
        verify(REPOSITORY).saveConnector(any());
        InOrder inOrder = inOrder(CONTACT_REPOSITORY, REPOSITORY);
        inOrder.verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
        inOrder.verify(REPOSITORY).saveConnector(any());
    }

    @Test
    public void registerConnectorPersistsContactsBeforeSavingConnectorAuth() {
        Connector connector = connectorWithTransientContacts();
        String keycloakClientId = "local-connector-example";
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> savedContact(invocation.getArgument(0)));
        when(REPOSITORY.saveConnector(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Connector result = connectorService.registerConnector(keycloakClientId, ConnectorStatus.ACTIVE, connector);

        assertEquals(keycloakClientId, result.getKeycloakClientId());
        assertEquals(ConnectorStatus.ACTIVE, result.getStatus());
        assertContactIdsWerePersisted(result);
        ArgumentCaptor<Connector> connectorCaptor = ArgumentCaptor.forClass(Connector.class);
        verify(REPOSITORY).saveConnector(connectorCaptor.capture());
        assertEquals(keycloakClientId, connectorCaptor.getValue().getKeycloakClientId());
        verify(CONTACT_REPOSITORY, times(4)).saveContact(any());
    }

    @Test
    public void registerConnectorRejectsDuplicateKeycloakClientIdBeforeSavingContacts() {
        when(REPOSITORY.findByKeycloakClientId("existing-client")).thenReturn(java.util.Optional.of(CONNECTOR));

        assertThrows(IllegalArgumentException.class,
                () -> connectorService.registerConnector("existing-client", ConnectorStatus.ACTIVE, connectorWithTransientContacts()));

        verify(CONTACT_REPOSITORY, never()).saveContact(any());
        verify(REPOSITORY, never()).saveConnector(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(CONNECTOR);

        Connector result = connectorService.getConnectorById(UUID.randomUUID());
        assertEquals(CONNECTOR, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllConnectors()).thenReturn(List.of(CONNECTOR));

        List<Connector> result = connectorService.getAllConnectors();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(CONNECTOR);
        verify(REPOSITORY, times(1)).getAllConnectors();
    }

    private static Connector connectorWithTransientContacts() {
        Contact contact = new Contact(null, ORGANIZATION, CONTACT_PERSON, CONTACT_STREET, CONTACT_PLZ, LOCATION,
                CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO, EMERGENCY_MAIL, SERVICE_NUMBER,
                SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO, ADMIN_MAIL, NOTES);
        return new Connector(null, CONNECTOR_NUMBER, contact, DESCRIPTION, VERSION, VERSION, DATA_DEFINITION,
                contact, contact, contact, NOTES);
    }

    private static Contact savedContact(Contact contact) {
        return contact.withId(UUID.randomUUID());
    }

    private static void assertContactIdsWerePersisted(Connector connector) {
        Assertions.assertThat(connector.getManufacturer().getId()).isNotNull();
        Assertions.assertThat(connector.getSoftwareManufacturer().getId()).isNotNull();
        Assertions.assertThat(connector.getTechnicallyResponsible().getId()).isNotNull();
        Assertions.assertThat(connector.getOperationCompany().getId()).isNotNull();
    }
}
