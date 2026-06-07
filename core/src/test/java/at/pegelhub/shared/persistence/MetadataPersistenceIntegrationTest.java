package at.pegelhub.shared.persistence;

import at.pegelhub.connector.persistence.ConnectorEntity;
import at.pegelhub.connector.persistence.SpringDataConnectorRepository;
import at.pegelhub.contact.persistence.ContactEntity;
import at.pegelhub.contact.persistence.SpringDataContactRepository;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
final class MetadataPersistenceIntegrationTest extends JpaIntegrationTestBase {

    @Autowired
    private SpringDataContactRepository contacts;

    @Autowired
    private SpringDataConnectorRepository connectors;

    @Test
    void connectorKeycloakClientIdIsUnique() {
        ContactEntity contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000003")));
        ConnectorEntity first = connector(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "connector-a",
                contact);
        first.setKeycloakClientId("local-connector-example");
        connectors.saveAndFlush(first);

        ConnectorEntity second = connector(
                UUID.fromString("30000000-0000-0000-0000-000000000004"),
                "connector-b",
                contact);
        second.setKeycloakClientId("local-connector-example");

        assertThrows(DataIntegrityViolationException.class, () -> connectors.saveAndFlush(second));
    }

    private static ContactEntity contact(UUID id) {
        return new ContactEntity(
                id,
                "organization",
                "contact person",
                "street",
                "1234",
                "Vienna",
                "AT",
                "111",
                "112",
                "emergency@example.org",
                "211",
                "212",
                "service@example.org",
                "311",
                "312",
                "admin@example.org",
                "notes");
    }

    private static ConnectorEntity connector(UUID id, String connectorNumber, ContactEntity contact) {
        return new ConnectorEntity(
                id,
                connectorNumber,
                contact,
                "type",
                "1.0.0",
                "1.0.0",
                "definition",
                contact,
                contact,
                contact,
                "notes",
                null,
                null);
    }
}
