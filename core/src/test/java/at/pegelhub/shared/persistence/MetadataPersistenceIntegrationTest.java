package at.pegelhub.shared.persistence;

import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.connector.persistence.JpaConnectorRepository;
import at.pegelhub.contact.persistence.JpaContact;
import at.pegelhub.contact.persistence.JpaContactRepository;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
final class MetadataPersistenceIntegrationTest extends JpaIntegrationTestBase {

    @Autowired
    private JpaContactRepository contacts;

    @Autowired
    private JpaConnectorRepository connectors;

    @Test
    void connectorKeycloakClientIdIsUnique() {
        JpaContact contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000003")));
        JpaConnector first = connector(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "connector-a",
                contact);
        first.setKeycloakClientId("local-connector-example");
        connectors.saveAndFlush(first);

        JpaConnector second = connector(
                UUID.fromString("30000000-0000-0000-0000-000000000004"),
                "connector-b",
                contact);
        second.setKeycloakClientId("local-connector-example");

        assertThrows(DataIntegrityViolationException.class, () -> connectors.saveAndFlush(second));
    }

    private static JpaContact contact(UUID id) {
        return new JpaContact(
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

    private static JpaConnector connector(UUID id, String connectorNumber, JpaContact contact) {
        return new JpaConnector(
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
                "notes");
    }
}
